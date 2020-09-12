package com.paulmandal.atak.forwarder.comm.commhardware;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.CallSuper;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.AddToGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.CreateGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.group.ChannelTracker;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class CommHardware {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + CommHardware.class.getSimpleName();

    protected static final String BCAST_MARKER = "ATAKBCAST";

    private static final int DELAY_BETWEEN_POLLING_FOR_MESSAGES = Config.DELAY_BETWEEN_POLLING_FOR_MESSAGES;

    public enum ConnectionState {
        SCANNING,
        TIMEOUT,
        DISCONNECTED,
        CONNECTED
    }

    public interface MessageListener {
        void onMessageReceived(byte[] message);
    }

    public interface ConnectionStateListener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

    private Handler mHandler;

    private final CommandQueue mCommandQueue;
    private QueuedCommandFactory mQueuedCommandFactory;
    private ChannelTracker mGroupTracker;

    private List<ConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArrayList<>();
    private List<MessageListener> mMessageListeners = new CopyOnWriteArrayList<>();

    private Thread mMessageWorkerThread;

    private boolean mConnected = false;
    private boolean mDestroyed = false;

    private UserInfo mSelfInfo;

    public CommHardware(Handler uiThreadHandler,
                        CommandQueue commandQueue,
                        QueuedCommandFactory queuedCommandFactory,
                        ChannelTracker groupTracker,
                        UserInfo selfInfo) {
        mHandler = uiThreadHandler;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mGroupTracker = groupTracker;
        mSelfInfo = selfInfo;

        startWorkerThreads();
    }

    /**
     * External API
     */
//    @CallSuper
//    public void init(@NonNull Context context, @NonNull String callsign, long gId, String atakUid) {
//        mSelfInfo = new UserInfo(callsign, gId, atakUid, mGroupTracker.getGroup() != null);
//    }

    public void broadcastDiscoveryMessage() {
        broadcastDiscoveryMessage(false);
    }

    public void createGroup(List<Long> memberGids) {
        if (!mConnected) {
            Log.d(TAG, "createGroup: not connected yet");
            return;
        }

        mCommandQueue.queueCommand(mQueuedCommandFactory.createCreateGroupCommand(memberGids));
    }

    public void addToGroup(List<Long> allMemberGids, List<Long> newMemberGids) {
        if (!mConnected) {
            Log.d(TAG, "addToGroup: not connected yet");
            return;
        }

//        if (mGroupTracker.getGroup() == null) {
//            Log.d(TAG, "addToGroup: not in a group yet");
//            return;
//        }

//        mCommandQueue.queueCommand(mQueuedCommandFactory.createAddToGroupCommand(mGroupTracker.getGroup().groupId, allMemberGids, newMemberGids));
    }

    public void connect() {
        if (mConnected) {
            Log.d(TAG, "connect: already connected");
            return;
        }

        mCommandQueue.queueCommand(mQueuedCommandFactory.createScanForCommDeviceCommand());
    }

    public void disconnect() {
        if (!mConnected) {
            Log.d(TAG, "forgetDevice: already disconnected");
            return;
        }

        mCommandQueue.queueCommand(mQueuedCommandFactory.createDisconnectFromCommDeviceCommand());
    }

    public void requestBatteryStatus() {
        if (!mConnected) {
            Log.d(TAG, "requestBatteryStatus: not connected yet");
            return;
        }

        mCommandQueue.queueCommand(mQueuedCommandFactory.createRequestBatteryStatusCommand());
    }

    @CallSuper
    public void destroy() {
        mDestroyed = true;
    }

    public boolean isConnected() {
        return mConnected;
    }

//    public boolean isInGroup() {
//        return mGroupTracker.getGroup() != null;
//    }

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
        mMessageWorkerThread = new Thread(() -> {
            while (!mDestroyed) {
                sleepForDelay(DELAY_BETWEEN_POLLING_FOR_MESSAGES);

                QueuedCommand queuedCommand = mCommandQueue.popHighestPriorityCommand(mConnected);

                if (queuedCommand == null) {
                    continue;
                }

                switch (queuedCommand.commandType) {
                    case SCAN_FOR_COMM_DEVICE:
                        handleScanForCommDevice();
                        break;
                    case DISCONNECT_FROM_COMM_DEVICE:
                        handleDisconnectFromCommDevice();
                        break;
                    case BROADCAST_DISCOVERY_MSG:
                        handleBroadcastDiscoveryMessage((BroadcastDiscoveryCommand) queuedCommand);
                        break;
                    case CREATE_GROUP:
                        handleCreateGroup((CreateGroupCommand) queuedCommand);
                        break;
                    case ADD_TO_GROUP:
                        handleAddToGroup((AddToGroupCommand) queuedCommand);
                        break;
                    case SEND_TO_GROUP:
                    case SEND_TO_INDIVIDUAL:
                        handleSendMessage((SendMessageCommand) queuedCommand);
                        break;
                    case GET_BATTERY_STATUS:
                        handleGetBatteryStatus();
                        break;
                }
            }
        });
        mMessageWorkerThread.setName("CommHardware.MessageWorker");
        mMessageWorkerThread.start();
    }

    protected boolean isDestroyed() {
        return mDestroyed;
    }

    protected void setConnected(boolean isConnected) {
        mConnected = isConnected;
    }

    protected UserInfo getSelfInfo() {
        return mSelfInfo;
    }

    protected void queueCommand(QueuedCommand queuedCommand) {
        mCommandQueue.queueCommand(queuedCommand);
    }

    protected void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = BCAST_MARKER + "," + getSelfInfo().meshId + "," + getSelfInfo().atakUid + "," + getSelfInfo().callsign + "," + (initialDiscoveryMessage ? 1 : 0);
        Log.d(TAG, "DISCO queueing broadcastDiscoveryMessage: " + broadcastData);
        mCommandQueue.queueCommand(mQueuedCommandFactory.createBroadcastDiscoveryCommand(broadcastData.getBytes()));
    }

    protected void notifyMessageListeners(byte[] message) {
        for (MessageListener listener : mMessageListeners) {
            mHandler.post(() -> listener.onMessageReceived(message));
        }
    }

    protected void notifyConnectionStateListeners(ConnectionState connectionState) {
        for (ConnectionStateListener connectionStateListener : mConnectionStateListeners) {
            mHandler.post(() -> connectionStateListener.onConnectionStateChanged(connectionState));
        }
    }
    /**
     * Utils
     */
    protected void sleepForDelay(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * For subclasses to implement
     */
    protected abstract void handleScanForCommDevice();
    protected abstract void handleDisconnectFromCommDevice();
    protected abstract void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand);
    protected abstract void handleCreateGroup(CreateGroupCommand createGroupCommand);
    protected abstract void handleAddToGroup(AddToGroupCommand addToGroupCommand);
    protected abstract void handleSendMessage(SendMessageCommand sendMessageCommand);
    protected abstract void handleGetBatteryStatus();
}
