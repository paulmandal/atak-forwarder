package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.RemoteException;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MessageStatus;
import com.geeksville.mesh.Portnums;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.MessageType;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MeshSender extends MeshEventHandler implements ConnectionStateHandler.Listener, MeshServiceController.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
    public interface MessageAckNackListener {
        void onMessageAckNack(int messageId, boolean isAck);
        void onMessageTimedOut(int messageId);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshSender.class.getSimpleName();

    private static final int WATCHDOG_TIMEOUT_MS = 900000; // 15 minutes
    private static final int WATCHDOG_RUN_INTERVAL_MINS = 1;
    private static final int REMOTE_EXCEPTION_RETRY_DELAY = 5000;
    private static final int DELAY_AFTER_SEND_ERROR_MS = 5000;
    private static final int NO_ID = -1;

    private final SharedPreferences mSharedPreferences;
    private final Handler mUiThreadHandler;
    private final MeshServiceController mMeshServiceController;
    private final UserTracker mUserTracker;
    private final ScheduledExecutorService mExecutor;

    private final Set<MessageAckNackListener> mMessageAckNackListeners = new CopyOnWriteArraySet<>();

    private IMeshService mMeshService;

    private int mPliHopLimit;
    private int mChatHopLimit;
    private int mOtherHopLimit;

    private boolean mSendingMessage = false;
    private boolean mStateSaved = false;

    private final Queue<OutboundMessageChunk> mPendingMessageChunks = new LinkedList<>();
    private final Queue<OutboundMessageChunk> mRestoreChunksAfterSuspend = new LinkedList<>();

    private final Object mSyncLock = new Object();

    private long mLastMessageSentTime;
    private int mPendingMessageId = NO_ID;
    private OutboundMessageChunk mChunkInFlight;

    public MeshSender(Context atakContext,
                      List<Destroyable> destroyables,
                      SharedPreferences sharedPreferences,
                      Handler uiThreadHandler,
                      Logger logger,
                      ConnectionStateHandler connectionStateHandler,
                      MeshServiceController meshServiceController,
                      UserTracker userTracker,
                      ScheduledExecutorService scheduledExecutorService) {
        super(atakContext,
                logger,
                new String[]{
                        MeshServiceConstants.ACTION_MESSAGE_STATUS
                },
                destroyables,
                connectionStateHandler);

        mSharedPreferences = sharedPreferences;
        mUiThreadHandler = uiThreadHandler;
        mMeshServiceController = meshServiceController;
        mUserTracker = userTracker;
        mExecutor = scheduledExecutorService;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        meshServiceController.addListener(this);
        connectionStateHandler.addListener(this);

        startWatchdog();

        onSharedPreferenceChanged(sharedPreferences, PreferencesKeys.KEY_PLI_HOP_LIMIT);
    }

    public void addMessageAckNackListener(MessageAckNackListener listener) {
        mMessageAckNackListeners.add(listener);
    }

    public void sendDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        sendMessage(MessageType.PLI, broadcastDiscoveryCommand.discoveryMessage, null);
    }

    public void sendMessage(SendMessageCommand sendMessageCommand) {
        sendMessage(sendMessageCommand.messageType, sendMessageCommand.message, sendMessageCommand.toUIDs);
    }

    public boolean isSendingMessage() {
        return mSendingMessage;
    }

    @Override
    public void onServiceConnectionStateChanged(MeshServiceController.ServiceConnectionState serviceConnectionState) {
        if (serviceConnectionState != MeshServiceController.ServiceConnectionState.CONNECTED) {
            return;
        }

        mMeshService = mMeshServiceController.getMeshService();
    }

    @Override
    public void onConnectionStateChanged(ConnectionStateHandler.ConnectionState connectionState) {
        super.onConnectionStateChanged(connectionState);

        if (connectionState != ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED) {
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
        mExecutor.shutdown();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        mLogger.v(TAG, "handleReceive: " + intent.getExtras().toString());
        int id = intent.getIntExtra(MeshServiceConstants.EXTRA_PACKET_ID, 0);
        MessageStatus status = intent.getParcelableExtra(MeshServiceConstants.EXTRA_STATUS);
        handleMessageStatusChange(id, status);
    }

    private void startWatchdog() {
        mLogger.v(TAG, "startWatchdog()");
        mExecutor.scheduleAtFixedRate(() -> {
            mLogger.v(TAG, "Watchdog checking for message status update timeout");
            if (mPendingMessageId == NO_ID) {
                mLogger.v(TAG, "  No pending message, exiting");
                // Not waiting for a message status to change
                return;
            }

            long timeSinceLastSent = System.currentTimeMillis() - mLastMessageSentTime;
            mLogger.v(TAG, "  Time since last sent: " + timeSinceLastSent);
            if (timeSinceLastSent > WATCHDOG_TIMEOUT_MS) {
                mLogger.e(TAG, "Waiting over " + (WATCHDOG_TIMEOUT_MS / 60000) + " mins for a message status change, calling sendChunk() again");
                sendChunk();
            }
        }, 0, WATCHDOG_RUN_INTERVAL_MINS, TimeUnit.MINUTES);
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
                mPendingMessageChunks.clear();
                mPendingMessageChunks.addAll(mRestoreChunksAfterSuspend);
                sendNextChunk();
                mStateSaved = false;
            }
        }
    }

    private void sendMessage(MessageType messageType, byte[] message, String[] toUIDs) {
        synchronized (mSyncLock) {
            sendMessageInternal(messageType, message, toUIDs);
        }
    }

    private void sendMessageInternal(MessageType messageType, byte[] message, String[] toUIDs) {
        mSendingMessage = true;

        int messageChunkLength = ForwarderConstants.MESHTASTIC_MESSAGE_CHUNK_LENGTH;

        int chunks = (int) Math.ceil((double) message.length / (double) messageChunkLength);

        if (chunks > 15) {
            mLogger.e(TAG, "Cannot break message into more than 15 pieces since we only have 1 byte for the header");
            return;
        }

        mLogger.i(TAG, "sendMessageInternal(), message length: " + message.length + " chunks: " + chunks);

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
                    mLogger.e(TAG, "sendMessageInternal() - could not get meshId for uid: " + uid);
                    continue;
                }

                addChunksToQueues(messageType, messages, meshId);
            }
        } else {
            addChunksToQueues(messageType, messages, DataPacket.ID_BROADCAST);
        }

        sendNextChunk();
    }

    private void addChunksToQueues(MessageType messageType, byte[][] chunks, String targetUid) {
        int chunksLength = chunks.length;
        for (int i = 0; i < chunksLength; i++) {
            byte[] message = chunks[i];
            OutboundMessageChunk outboundMessageChunk = new OutboundMessageChunk(messageType, i, chunksLength, message, targetUid);
            mPendingMessageChunks.add(outboundMessageChunk);
            mRestoreChunksAfterSuspend.add(outboundMessageChunk);
        }
    }

    private void sendNextChunk() {
        mLogger.d(TAG, "  sendNextChunk()");
        OutboundMessageChunk outboundMessageChunk = mPendingMessageChunks.poll();

        if (outboundMessageChunk == null) {
            // Done sending
            mRestoreChunksAfterSuspend.clear();
            mSendingMessage = false;
            mLogger.i(TAG, "Done sending message");
            return;
        }

        mChunkInFlight = outboundMessageChunk;

        sendChunk();
    }

    private void sendChunk() {
        mLogger.d(TAG, "  sendChunk()");

        int hopLimit = getHopLimit(mChunkInFlight.messageType);

        DataPacket dataPacket = new DataPacket(mChunkInFlight.targetUid,
                mChunkInFlight.chunk,
                Portnums.PortNum.ATAK_FORWARDER.getNumber(),
                DataPacket.ID_LOCAL,
                System.currentTimeMillis(),
                0,
                MessageStatus.UNKNOWN,
                hopLimit,
                0);

        try {
            mMeshService.send(dataPacket);
            mLastMessageSentTime = System.currentTimeMillis();
            mPendingMessageId = dataPacket.getId();
            OutboundMessageChunk chunkInFlight = mChunkInFlight;

            String chunkAsStr = new String(chunkInFlight.chunk).replace("\n", "").replace("\r", "");
            mLogger.i(TAG, "---> Sent packet: " + chunkAsStr);
            mLogger.i(TAG, "        messageChunk: " + (chunkInFlight.index + 1) + "/" + chunkInFlight.count + " to: " + chunkInFlight.targetUid + ", waiting for ack/nack id: " + mPendingMessageId);
        } catch (RemoteException e) {
            maybeSaveState();
            mUiThreadHandler.postDelayed(this::maybeRestoreState, REMOTE_EXCEPTION_RETRY_DELAY);
            mLogger.e(TAG, "sendChunk(), RemoteException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMessageStatusChange(int id, MessageStatus status) {
        if (status == MessageStatus.DELIVERED || status == MessageStatus.RECEIVED || status == MessageStatus.ERROR) {
            notifyAckNackListeners(id, status);
        }

        if (id != mPendingMessageId) {
            mLogger.e(TAG, "  handleMessageStatusChange for a msg we don't care about messageId: " + id + " status: " + status + " (wanted: " + mPendingMessageId + ")");
            return;
        }

        mLogger.i(TAG, "handleMessageStatusChange, got the message we ACK/NACK we're waiting for id: " + mPendingMessageId + ", status: " + status);

        if (status == MessageStatus.DELIVERED || status == MessageStatus.RECEIVED) {
            mPendingMessageId = NO_ID;
            mChunkInFlight = null;
            sendNextChunk();
        } else if (status == MessageStatus.QUEUED || status == MessageStatus.ENROUTE || status == MessageStatus.UNKNOWN) {
            mLogger.i(TAG, "  Status is: " + status + ", waiting for ERROR/DELIVERED");
            // Do nothing, wait for delivered or error
        } else if (mChunkInFlight.messageType == MessageType.PLI) {
            // Don't try to re-send PLI, just keep going
            mPendingMessageChunks.clear();
            sendNextChunk();
        } else if (status == MessageStatus.ERROR) {
            mLogger.i(TAG, "  Status is ERROR, resending chunk after " + DELAY_AFTER_SEND_ERROR_MS +  "ms");
            try {
                Thread.sleep(DELAY_AFTER_SEND_ERROR_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            if (mChunkInFlight == null) { TODO: maybe we need this?
//                return;
//            }
            sendChunk();
        } else {
            mLogger.i(TAG, "We don't know how to handle status: " + status + " wait until there's a new status and hopefully we can handle that.");
        }
    }

    private void notifyAckNackListeners(int id, MessageStatus status) {
        mUiThreadHandler.post(() -> {
            for (MessageAckNackListener messageAckNackListener : mMessageAckNackListeners) {
                messageAckNackListener.onMessageAckNack(id, status == MessageStatus.DELIVERED || status == MessageStatus.RECEIVED);
            }
        });
    }

    private int getHopLimit(MessageType messageType) {
        if (messageType == MessageType.PLI) {
            return mPliHopLimit;
        } else if (messageType == MessageType.CHAT) {
            return mChatHopLimit;
        }
        return mOtherHopLimit;
    }
}
