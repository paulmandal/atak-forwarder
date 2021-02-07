package com.paulmandal.atak.forwarder.comm.commhardware;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.annotation.CallSuper;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Constants;
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
    private static final String TAG = Constants.DEBUG_TAG_PREFIX + CommHardware.class.getSimpleName();

    private static final int CHECK_MESSAGE_QUEUE_INTERVAL_MS = 300;

    public enum ConnectionState {
        NO_SERVICE_CONNECTION,
        NO_DEVICE_CONFIGURED,
        DEVICE_DISCONNECTED,
        DEVICE_CONNECTED
    }



    private final Handler mUiThreadHandler;

    private final CommandQueue mCommandQueue;
    private final QueuedCommandFactory mQueuedCommandFactory;


    private ScheduledExecutorService mMessageWorkerExecutor;

    private ConnectionState mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;
    private boolean mDestroyed = false;

    private boolean mSendingMessage = false;

    private final UserInfo mSelfInfo;

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
        mMessageWorkerExecutor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread thread = new Thread(r);
            thread.setName(CommHardware.class.getSimpleName() + ".Worker");
            return thread;
        });
        mMessageWorkerExecutor.scheduleAtFixedRate(() -> {
            if (!mDestroyed) {
                QueuedCommand queuedCommand = mCommandQueue.popHighestPriorityCommand(mConnectionState == ConnectionState.DEVICE_CONNECTED);

                if (mSendingMessage) {
                    return;
                }

                if (queuedCommand == null) {
                    return;
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
        }, 0, CHECK_MESSAGE_QUEUE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    protected boolean isDestroyed() {
        return mDestroyed;
    }

    protected void setSendingMessage(boolean sendingMessage) {
        mSendingMessage = sendingMessage;
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

    /**
     * For subclasses to implement
     */
    public abstract void connect();
    protected abstract void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand);
    protected abstract void handleSendMessage(SendMessageCommand sendMessageCommand);
    protected abstract void handleDiscoveryMessage(String message);
}
