package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.MessageStatus;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshSender extends MeshEventHandler implements MeshServiceController.ConnectionStateListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public interface MessageAckNackListener {
        void onMessageAckNack(int messageId, boolean isAck);
        void onMessageTimedOut(int messageId);
    }

    private final SharedPreferences mSharedPreferences;
    private final Handler mUiThreadHandler;
    private final MeshServiceController mMeshServiceController;

    private final Set<MessageAckNackListener> mMessageAckNackListeners = new CopyOnWriteArraySet<>();

    private int mPliHopLimit;
    private int mChatHopLimit;
    private int mOtherHopLimit;

    private ConnectionState mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;
    private boolean mSuspended = false;
    private boolean mSendingMessage = false;

    public MeshSender(Context atakContext,
                      List<Destroyable> destroyables,
                      SharedPreferences sharedPreferences,
                      MeshSuspendController meshSuspendController,
                      Handler uiThreadHandler,
                      Logger logger,
                      MeshServiceController meshServiceController) {
        super(atakContext,
                logger,
                new String[] {
                        MeshServiceConstants.ACTION_MESSAGE_STATUS
                },
                destroyables,
                meshSuspendController);

        mSharedPreferences = sharedPreferences;
        mUiThreadHandler = uiThreadHandler;
        mMeshServiceController = meshServiceController;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        meshServiceController.addConnectionStateListener(this);

        onSharedPreferenceChanged(sharedPreferences, PreferencesKeys.KEY_PLI_HOP_LIMIT);
    }

    public void addMessageAckNackListener(MessageAckNackListener listener) {
        mMessageAckNackListeners.add(listener);
    }

    public void sendDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {

    }

    public void sendMessage(SendMessageCommand sendMessageCommand) {

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
    }

    @Override
    public void onSuspendedChanged(boolean suspended) {
        super.onSuspendedChanged(suspended);

        mSuspended = suspended;
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
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(MeshServiceConstants.EXTRA_PACKET_ID, 0);
        MessageStatus status = intent.getParcelableExtra(MeshServiceConstants.EXTRA_STATUS);
        handleMessageStatusChange(id, status);
    }

    private void handleMessageStatusChange(int id, MessageStatus status) {
        mUiThreadHandler.post(() -> {
            for (MessageAckNackListener messageAckNackListener : mMessageAckNackListeners) {
                messageAckNackListener.onMessageAckNack(id, status == MessageStatus.DELIVERED);
            }
        });

        if (id != mPendingMessageId) {
            Log.e(TAG, "handleMessageStatusChange for a msg we don't care about messageId: " + id + " status: " + status + " (wanted: " + mPendingMessageId + ")");
            return;
        }

        mPendingMessageReceived = status != MessageStatus.ERROR;
        Log.d(TAG, "handleMessageStatusChange, got the message we ACK/NACK we're waiting for id: " + mPendingMessageId + ", status: " + status);

        if (status == MessageStatus.ERROR || status == MessageStatus.DELIVERED) {
            mPendingMessageCountdownLatch.countDown();
        }
    }
}
