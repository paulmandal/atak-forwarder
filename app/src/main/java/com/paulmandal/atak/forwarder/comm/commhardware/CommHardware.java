package com.paulmandal.atak.forwarder.comm.commhardware;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.annotation.CallSuper;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class CommHardware extends DestroyableSharedPrefsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + CommHardware.class.getSimpleName();

    protected static final String BCAST_MARKER = "ATAKBCAST";

    public enum ConnectionState {
        NO_SERVICE_CONNECTION,
        NO_DEVICE_CONFIGURED,
        DEVICE_DISCONNECTED,
        DEVICE_CONNECTED
    }

    public interface MessageListener {
        void onMessageReceived(int messageId, byte[] message);
    }

    public interface ConnectionStateListener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

    private Handler mUiThreadHandler;

    private final CommandQueue mCommandQueue;
    private QueuedCommandFactory mQueuedCommandFactory;

    private final List<ConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArrayList<>();
    private final List<MessageListener> mMessageListeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService mMessageWorkerExecutor;

    private ConnectionState mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;
    private boolean mDestroyed = false;

    private UserInfo mSelfInfo;

    public CommHardware(List<Destroyable> destroyables,
                        SharedPreferences sharedPreferences,
                        String[] simplePreferencesKeys,
                        String[] complexPreferencesKeys,
                        Handler uiThreadHandler,
                        CommandQueue commandQueue,
                        QueuedCommandFactory queuedCommandFactory,
                        UserInfo selfInfo) {
        super(destroyables, sharedPreferences, simplePreferencesKeys, complexPreferencesKeys);
        mUiThreadHandler = uiThreadHandler;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mSelfInfo = selfInfo;

        startWorkerThreads();
    }

    /**
     * External API
     */
    public void broadcastDiscoveryMessage() {
        broadcastDiscoveryMessage(false);
    }

    @Override
    @CallSuper
    public void onDestroy(Context context, MapView mapView) {
        super.onDestroy(context, mapView);
        mMessageWorkerExecutor.shutdown();
        mDestroyed = true;
    }

    /**
     * Listener Management
     */
    public void addMessageListener(MessageListener listener) {
        mMessageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        mMessageListeners.remove(listener);
    }

    public void addConnectionStateListener(ConnectionStateListener listener) {
        mConnectionStateListeners.add(listener);
    }

    public void removeConnectionStateListener(ConnectionStateListener listener) {
        mConnectionStateListeners.remove(listener);
    }

    /**
     * Internal API
     */
    @CallSuper
    protected void startWorkerThreads() {
        mMessageWorkerExecutor = Executors.newSingleThreadScheduledExecutor();
        mMessageWorkerExecutor.scheduleAtFixedRate(() -> {
            while (!mDestroyed) {
                QueuedCommand queuedCommand = mCommandQueue.popHighestPriorityCommand(mConnectionState == ConnectionState.DEVICE_CONNECTED);

                if (queuedCommand == null) {
                    continue;
                }

                switch (queuedCommand.commandType) {
                    case BROADCAST_DISCOVERY_MSG:
                        handleBroadcastDiscoveryMessage((BroadcastDiscoveryCommand) queuedCommand);
                        break;
                    case SEND_TO_CHANNEL:
                    case SEND_TO_INDIVIDUAL:
                        handleSendMessage((SendMessageCommand) queuedCommand);
                        break;
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    protected boolean isDestroyed() {
        return mDestroyed;
    }

    protected void setConnectionState(ConnectionState connectionState) {
        mConnectionState = connectionState;
    }

    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    protected UserInfo getSelfInfo() {
        return mSelfInfo;
    }

    protected void queueCommand(QueuedCommand queuedCommand) {
        mCommandQueue.queueCommand(queuedCommand);
    }

    protected void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = BCAST_MARKER + "," + mSelfInfo.meshId + "," + mSelfInfo.atakUid + "," + mSelfInfo.callsign + "," + (initialDiscoveryMessage ? 1 : 0);

        String broadcastWithInitialDiscoveryUnset = broadcastData.replaceAll(",1$", ",0");
        handleDiscoveryMessage(broadcastWithInitialDiscoveryUnset);

        mCommandQueue.queueCommand(mQueuedCommandFactory.createBroadcastDiscoveryCommand(broadcastData.getBytes()));
    }

    protected void notifyMessageListeners(int messageId, byte[] message) {
        for (MessageListener listener : mMessageListeners) {
            mUiThreadHandler.post(() -> listener.onMessageReceived(messageId, message));
        }
    }

    protected void notifyConnectionStateListeners(ConnectionState connectionState) {
        for (ConnectionStateListener connectionStateListener : mConnectionStateListeners) {
            mUiThreadHandler.post(() -> connectionStateListener.onConnectionStateChanged(connectionState));
        }
    }

    /**
     * For subclasses to implement
     */
    public abstract void connect();
    protected abstract void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand);
    protected abstract void handleSendMessage(SendMessageCommand sendMessageCommand);
    protected abstract void handleDiscoveryMessage(String message);
}
