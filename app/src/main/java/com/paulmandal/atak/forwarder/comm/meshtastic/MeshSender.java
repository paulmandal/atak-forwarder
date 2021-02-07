package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MessageStatus;
import com.geeksville.mesh.Portnums;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshSender extends MeshEventHandler implements MeshServiceController.ConnectionStateListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public interface MessageAckNackListener {
        void onMessageAckNack(int messageId, boolean isAck);

        void onMessageTimedOut(int messageId);
    }

    private static final String TAG = Constants.DEBUG_TAG_PREFIX + MeshSender.class.getSimpleName();

    private static final int REMOTE_EXCEPTION_RETRY_DELAY = 5000;

    private final SharedPreferences mSharedPreferences;
    private final Handler mUiThreadHandler;
    private final MeshServiceController mMeshServiceController;
    private final UserTracker mUserTracker;

    private final Set<MessageAckNackListener> mMessageAckNackListeners = new CopyOnWriteArraySet<>();

    private IMeshService mMeshService;

    private int mPliHopLimit;
    private int mChatHopLimit;
    private int mOtherHopLimit;

    private ConnectionState mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;
    private boolean mSuspended = false;
    private boolean mSendingMessage = false;
    private boolean mStateSaved = false;

    private byte[] mPendingMessage;
    private String[] mPendingMessageTargets;

    private Queue<OutboundMessageChunk> mPendingMessageChunks = new LinkedList<>();
    private Queue<OutboundMessageChunk> mRestoreChunksAfterSuspend = new LinkedList<>();

    private final Object mSyncLock = new Object();

    private int mPendingMessageId;
    private OutboundMessageChunk mChunkInFlight;

    public MeshSender(Context atakContext,
                      List<Destroyable> destroyables,
                      SharedPreferences sharedPreferences,
                      MeshSuspendController meshSuspendController,
                      Handler uiThreadHandler,
                      Logger logger,
                      MeshServiceController meshServiceController,
                      UserTracker userTracker) {
        super(atakContext,
                logger,
                new String[]{
                        MeshServiceConstants.ACTION_MESSAGE_STATUS
                },
                destroyables,
                meshSuspendController);

        mSharedPreferences = sharedPreferences;
        mUiThreadHandler = uiThreadHandler;
        mMeshServiceController = meshServiceController;
        mUserTracker = userTracker;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        meshServiceController.addConnectionStateListener(this);

        onSharedPreferenceChanged(sharedPreferences, PreferencesKeys.KEY_PLI_HOP_LIMIT);
    }

    public void addMessageAckNackListener(MessageAckNackListener listener) {
        mMessageAckNackListeners.add(listener);
    }

    public void sendDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        sendMessage(broadcastDiscoveryCommand.discoveryMessage, null);
    }

    public void sendMessage(SendMessageCommand sendMessageCommand) {
        sendMessage(sendMessageCommand.message, sendMessageCommand.toUIDs);
    }

    public boolean isSuspended() {
        return mSuspended;
    }

    public boolean isSendingMessage() {
        return mSendingMessage;
    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {
        mConnectionState = connectionState;
        mMeshService = mMeshServiceController.getMeshService();

        if (connectionState != ConnectionState.DEVICE_CONNECTED) {
            maybeSaveState();
        } else {
            maybeRestoreState();
        }
    }

    @Override
    public void onSuspendedChanged(boolean suspended) {
        super.onSuspendedChanged(suspended);

        mSuspended = suspended;

        if (suspended) {
            maybeSaveState();
        } else {
            maybeRestoreState();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PreferencesKeys.KEY_PLI_HOP_LIMIT)
                || key.equals(PreferencesKeys.KEY_CHAT_HOP_LIMIT)
                || key.equals(PreferencesKeys.KEY_OTHER_HOP_LIMIT)) {
            mPliHopLimit = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_PLI_HOP_LIMIT, PreferencesDefaults.DEFAULT_PLI_HOP_LIMIT));
            mChatHopLimit = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHAT_HOP_LIMIT, PreferencesDefaults.DEFAULT_CHAT_HOP_LIMIT));
            mOtherHopLimit = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_OTHER_HOP_LIMIT, PreferencesDefaults.DEFAULT_OTHER_HOP_LIMIT));
        }
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        super.onDestroy(context, mapView);
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(MeshServiceConstants.EXTRA_PACKET_ID, 0);
        MessageStatus status = intent.getParcelableExtra(MeshServiceConstants.EXTRA_STATUS);
        handleMessageStatusChange(id, status);
    }

    private void maybeSaveState() {
        synchronized (mSyncLock) {
            if (mRestoreChunksAfterSuspend.size() > 0) {
                mStateSaved = true;
            }
        }
    }

    private void maybeRestoreState() {
        synchronized (mSyncLock) {
            if (mStateSaved) {
                mPendingMessageChunks.addAll(mRestoreChunksAfterSuspend);
                sendNextChunk();
                mStateSaved = false;
            }
        }
    }

    private void sendMessage(byte[] message, String[] toUIDs) {
        synchronized (mSyncLock) {
            sendMessageInternal(message, toUIDs);
        }
    }

    private void sendMessageInternal(byte[] message, String[] toUIDs) {
        mSendingMessage = true;

        mPendingMessage = message;
        mPendingMessageTargets = toUIDs;

        int messageChunkLength = Constants.MESHTASTIC_MESSAGE_CHUNK_LENGTH;

        int chunks = (int) Math.ceil((double) message.length / (double) messageChunkLength);

        if (chunks > 15) {
            mLogger.e(TAG, "Cannot break message into more than 15 pieces since we only have 1 byte for the header");
            return;
        }

        Log.v(TAG, "sendMessageToUserOrGroup, message length: " + message.length + " chunks: " + chunks);

        byte[][] messages = new byte[chunks][];
        for (int i = 0; i < chunks; i++) {
            int start = i * messageChunkLength;
            int end = Math.min((i + 1) * messageChunkLength, message.length);
            int length = end - start;

            messages[i] = new byte[length + 1];
            messages[i][0] = (byte) (i << 4 | chunks);

            for (int idx = 1, j = start; j < end; j++, idx++) {
                messages[i][idx] = message[j];
            }
        }

        if (toUIDs != null) {
            for (String uid : toUIDs) {
                String meshId = mUserTracker.getMeshIdForUid(uid);
                if (meshId.isEmpty()) {
                    continue;
                }

                addChunksToQueues(messages, uid);
            }
        } else {
            addChunksToQueues(messages, DataPacket.ID_BROADCAST);
        }

        sendNextChunk();
    }

    private void addChunksToQueues(byte[][] chunks, String targetUid) {
        int chunksLength = chunks.length;
        for (int i = 0; i < chunksLength; i++) {
            byte[] message = chunks[i];
            OutboundMessageChunk outboundMessageChunk = new OutboundMessageChunk(i, chunksLength, message, targetUid);
            mPendingMessageChunks.add(outboundMessageChunk);
            mRestoreChunksAfterSuspend.add(outboundMessageChunk);
        }
    }

    private void sendNextChunk() {
        OutboundMessageChunk outboundMessageChunk = mPendingMessageChunks.poll();

        if (outboundMessageChunk == null) {
            // Done sending
            mRestoreChunksAfterSuspend.clear();
            mChunkInFlight = null; // TODO: necessary?
            mSendingMessage = false;
        }

        mChunkInFlight = outboundMessageChunk;

        sendChunk();
    }

    private void sendChunk() {
        DataPacket dataPacket = new DataPacket(mChunkInFlight.targetUid,
                mChunkInFlight.chunk,
                Portnums.PortNum.UNKNOWN_APP.getNumber(),
                DataPacket.ID_LOCAL,
                System.currentTimeMillis(),
                0,
                MessageStatus.UNKNOWN);
        try {
            mMeshService.send(dataPacket);
            mPendingMessageId = dataPacket.getId();
            mLogger.d(TAG, "  sendChunk() waiting for ACK/NACK for messageId: " + dataPacket.getId());
        } catch (RemoteException e) {
            maybeSaveState();
            mUiThreadHandler.postDelayed(() -> maybeRestoreState(), REMOTE_EXCEPTION_RETRY_DELAY);
            mLogger.e(TAG, "  sendChunk(), RemoteException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMessageStatusChange(int id, MessageStatus status) {
        mUiThreadHandler.post(() -> {
            for (MessageAckNackListener messageAckNackListener : mMessageAckNackListeners) {
                messageAckNackListener.onMessageAckNack(id, status == MessageStatus.DELIVERED);
            }
        });

        if (id != mPendingMessageId) {
            mLogger.e(TAG, "  handleMessageStatusChange for a msg we don't care about messageId: " + id + " status: " + status + " (wanted: " + mPendingMessageId + ")");
            return;
        }

        mLogger.d(TAG, "  handleMessageStatusChange, got the message we ACK/NACK we're waiting for id: " + mPendingMessageId + ", status: " + status);

        if (status == MessageStatus.DELIVERED) {
            sendNextChunk();
        } else if (status == MessageStatus.QUEUED) {
            mLogger.d(TAG, "  Status is queued, waiting for ERROR/DELIVERED");
            // Do nothing, wait for delivered or error
        } else {
            mLogger.d(TAG, "  Status is ERROR, resending chunk");
            sendChunk();
        }
    }
}
