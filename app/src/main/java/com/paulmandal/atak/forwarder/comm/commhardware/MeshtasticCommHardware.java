package com.paulmandal.atak.forwarder.comm.commhardware;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.MeshUser;
import com.geeksville.mesh.MessageStatus;
import com.geeksville.mesh.NodeInfo;
import com.geeksville.mesh.Position;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.channel.NonAtakUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MeshtasticCommHardware extends MessageLengthLimitedCommHardware {
    public interface UserListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);
        void onChannelUsersUpdated(List<NonAtakUserInfo> userInfoList);
        void onUserUpdated(NonAtakUserInfo nonAtakUserInfo);
    }

    public interface ChannelSettingsListener {
        void onChannelSettingsUpdated(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig);
    }

    public interface MessageAckNackListener {
        void onMessageAckNack(int messageId, boolean isAck);
        void onMessageTimedOut(int messageId);
    }

    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticCommHardware.class.getSimpleName();

    private static final int MESSAGE_AWAIT_TIMEOUT_MS = Config.MESSAGE_AWAIT_TIMEOUT_MS;
    private static final int DELAY_AFTER_STOPPING_SERVICE = Config.DELAY_AFTER_STOPPING_SERVICE;
    private static final int POSITION_BROADCAST_INTERVAL_S = Config.POSITION_BROADCAST_INTERVAL_S;
    private static final int LCD_SCREEN_ON_S = Config.LCD_SCREEN_ON_S;

    /**
     * Intents the Meshtastic service can send
     */
    private static final String ACTION_MESH_CONNECTED = "com.geeksville.mesh.MESH_CONNECTED";
    private static final String ACTION_RECEIVED_DATA = "com.geeksville.mesh.RECEIVED_DATA";
    private static final String ACTION_NODE_CHANGE = "com.geeksville.mesh.NODE_CHANGE";
    private static final String ACTION_MESSAGE_STATUS = "com.geeksville.mesh.MESSAGE_STATUS";

    /**
     * Extra data fields from the Meshtastic service
     */
    // a bool true means now connected, false means not
    private static final String EXTRA_CONNECTED = "com.geeksville.mesh.Connected";

    /// a bool true means we expect this condition to continue until, false means device might come back
    private static final String EXTRA_PERMANENT = "com.geeksville.mesh.Permanent";

    private static final String EXTRA_PAYLOAD = "com.geeksville.mesh.Payload";
    private static final String EXTRA_NODEINFO = "com.geeksville.mesh.NodeInfo";
    private static final String EXTRA_PACKET_ID = "com.geeksville.mesh.PacketId";
    private static final String EXTRA_STATUS = "com.geeksville.mesh.Status";

    private static final String STATE_CONNECTED = "CONNECTED";

    private UserTracker mChannelTracker;
    private UserListener mUserListener;
    private Activity mActivity;
    private Handler mUiThreadHandler;
    private StateStorage mStateStorage;

    private IntentFilter mIntentFilter;

    private IMeshService mMeshService;
    private ServiceConnection mServiceConnection;

    private final List<ChannelSettingsListener> mChannelSettingsListeners = new CopyOnWriteArrayList<>();
    private MessageAckNackListener mMessageAckNackListener;

    private CountDownLatch mPendingMessageCountdownLatch; // TODO: maybe move this up to MessageLengthLimitedCommHardware
    private int mPendingMessageId;
    private boolean mPendingMessageReceived;

    private Intent mServiceIntent;

    boolean mConnectedToService = false;
    boolean mRadioSetupCalled = false;

    private int mDataRate;
    private String mCommDeviceAddress;

    public MeshtasticCommHardware(Handler uiThreadHandler,
                                  UserListener userListener,
                                  UserTracker channelTracker,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  Activity activity,
                                  UserInfo selfInfo,
                                  StateStorage stateStorage,
                                  String commDeviceAddress) {
        super(uiThreadHandler, commandQueue, queuedCommandFactory, channelTracker, Config.MESHTASTIC_MESSAGE_CHUNK_LENGTH, selfInfo);

        mUiThreadHandler = uiThreadHandler;
        mActivity = activity;
        mUserListener = userListener;
        mChannelTracker = channelTracker;
        mStateStorage = stateStorage;
        mCommDeviceAddress = commDeviceAddress;

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mConnectedToService = true;

                maybeInitialConnection();
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;

                setConnectionState(ConnectionState.NO_SERVICE_CONNECTION);
                notifyConnectionStateListeners(ConnectionState.NO_SERVICE_CONNECTION);
                mConnectedToService = false;
            }
        };

        mServiceIntent = new Intent();
        mServiceIntent.setClassName("com.geeksville.mesh", "com.geeksville.mesh.service.MeshService");

        bindToService();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);
        filter.addAction(ACTION_NODE_CHANGE);
        filter.addAction(ACTION_RECEIVED_DATA);
        filter.addAction(ACTION_MESSAGE_STATUS);

        mIntentFilter = filter;

        mActivity.registerReceiver(mBroadcastReceiver, filter);
    }

    public void addChannelSettingsListener(ChannelSettingsListener listener) {
        mChannelSettingsListeners.add(listener);
    }

    public void setMessageAckNackListener(MessageAckNackListener listener) {
        mMessageAckNackListener = listener;
    }

    @Override
    protected boolean sendMessageSegment(byte[] message, String targetId) {
        prepareToSendMessage(message.length);

        DataPacket dataPacket = new DataPacket(targetId, message);
        try {
            mMeshService.send(dataPacket);
            mPendingMessageId = dataPacket.getId();
            Log.d(TAG, "sendMessageSegment() waiting for ACK/NACK for messageId: " + dataPacket.getId());
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessageSegment, RemoteException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        awaitPendingMessageCountDownLatch();

        return mPendingMessageReceived;
    }

    public void updateChannelSettings(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        try {
            byte[] radioConfigBytes = mMeshService.getRadioConfig();

            if (radioConfigBytes == null) {
                Log.e(TAG, "radioConfigBytes was null");
                return;
            }

            mChannelTracker.clearData();

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
            MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

            // Begin Updates TODO: remove

            channelSettingsBuilder.setName(channelName);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(psk));
            channelSettingsBuilder.setModemConfig(modemConfig);

            // End Updates TODO: remove

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            mMeshService.setRadioConfig(radioConfig.toByteArray());
            updateChannelStatus();
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "Exception in updateChannelSettings(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void connect() {
        if (getConnectionState() == ConnectionState.DEVICE_CONNECTED) {
            Log.d(TAG, "connect: already connected");
            return;
        }
        unbindAndStopService();
        bindToService();
    }

    public boolean setDeviceAddress(String deviceAddress) {
        Log.e(TAG, "setDeviceAddress: " + deviceAddress);
        boolean success = false;
        try {
            mMeshService.setDeviceAddress(String.format("x%s", deviceAddress));
            mCommDeviceAddress = deviceAddress;

            mStateStorage.storeCommDeviceAddress(deviceAddress);

            success = true;
            Log.e(TAG, "setDeviceAddress success: " + success);
            connect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return success;
    }

    public void suspendResume(boolean suspended) { // TODO: rename
        if (suspended) {
            mActivity.unregisterReceiver(mBroadcastReceiver);
            setConnectionState(ConnectionState.DEVICE_DISCONNECTED);
        } else {
            mActivity.registerReceiver(mBroadcastReceiver, mIntentFilter);
            setConnectionState(ConnectionState.DEVICE_CONNECTED);
        }
    }

    public String getDeviceAddress() {
        return mCommDeviceAddress;
    }

    @Override
    protected void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        if (!sendMessageSegment(broadcastDiscoveryCommand.discoveryMessage, DataPacket.ID_BROADCAST)) {
            // Send this message back to the queue
            queueCommand(broadcastDiscoveryCommand);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mActivity.unbindService(mServiceConnection);
        mActivity.unregisterReceiver(mBroadcastReceiver);
        mConnectedToService = false;
    }

    private void bindToService() {
        mActivity.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindAndStopService() {
        if (!mConnectedToService) {
            return;
        }

        mActivity.unbindService(mServiceConnection);
        mActivity.stopService(mServiceIntent);

        try {
            Thread.sleep(DELAY_AFTER_STOPPING_SERVICE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void maybeInitialConnection() {
        ConnectionState oldConnectionState = getConnectionState();

        if (mCommDeviceAddress != null) {
            try {
                mMeshService.setDeviceAddress(String.format("x%s", mCommDeviceAddress));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        updateConnectionState();

        boolean connected = getConnectionState() == ConnectionState.DEVICE_CONNECTED;

        if (connected) {
            maybeSetupRadio();
            updateMeshId();
            updateChannelMembers();
            updateChannelStatus();
        }

        if (oldConnectionState != getConnectionState() && connected) {
            broadcastDiscoveryMessage(true);
        }
    }

    private void updateConnectionState() {
        try {
            String meshId = mMeshService.getMyId();
            ConnectionState connectionState;
            if (mCommDeviceAddress == null) {
                connectionState = ConnectionState.NO_DEVICE_CONFIGURED;
            } else {
                boolean connected = mMeshService.connectionState().equals(STATE_CONNECTED);
                connectionState = connected ? ConnectionState.DEVICE_CONNECTED : ConnectionState.DEVICE_DISCONNECTED;
            }

            setConnectionState(connectionState);
            notifyConnectionStateListeners(connectionState);
            Log.e(TAG, "  ConnectionState: " + connectionState);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in updateConnectionState: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void maybeSetupRadio() {
        if (mRadioSetupCalled) {
            return;
        }

        try {
            UserInfo selfInfo = getSelfInfo();
            mMeshService.setOwner(null, selfInfo.callsign, selfInfo.callsign.substring(0, 1));

            // Set up radio / channel settings
            byte[] radioConfigBytes = mMeshService.getRadioConfig();

            if (radioConfigBytes == null) {
                Log.e(TAG, "radioConfigBytes was null");
                return;
            }

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
            MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

            // Begin Updates

            userPreferencesBuilder.setPositionBroadcastSecs(POSITION_BROADCAST_INTERVAL_S);
            userPreferencesBuilder.setScreenOnSecs(LCD_SCREEN_ON_S);

            // End Updates

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            mMeshService.setRadioConfig(radioConfig.toByteArray());
            mRadioSetupCalled = true;
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "Exception in setupRadio(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateMeshId() {
        try {
            getSelfInfo().meshId = mMeshService.getMyId();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in updateMeshId(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateChannelMembers() {
        try {
            List<NodeInfo> nodes = mMeshService.getNodes();
            List<NonAtakUserInfo> userInfoList = new ArrayList<>();
            for (NodeInfo nodeInfo : nodes) {

                NonAtakUserInfo nonAtakUserInfo = nonAtakUserInfoFromNodeInfo(nodeInfo);
                userInfoList.add(nonAtakUserInfo);
            }
            mUserListener.onChannelUsersUpdated(userInfoList);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in updateChannelMembers(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private NonAtakUserInfo nonAtakUserInfoFromNodeInfo(NodeInfo nodeInfo) {
        MeshUser meshUser = nodeInfo.getUser();

        double lat = 0.0;
        double lon = 0.0;
        int altitude = 0;
        Position position = nodeInfo.getValidPosition();
        if (position != null) {
            lat = position.getLatitude();
            lon = position.getLongitude();
            altitude = position.getAltitude();
        }
        return new NonAtakUserInfo(meshUser.getLongName(), meshUser.getId(), nodeInfo.getBatteryPctLevel(), lat, lon, altitude, meshUser.getShortName());
    }

    private void updateChannelStatus() {
        try {
            byte[] radioConfigBytes = mMeshService.getRadioConfig();

            if (radioConfigBytes == null) {
                Log.e(TAG, "radioConfigBytes was null");
                return;
            }

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            mDataRate = channelSettings.getModemConfig().getNumber();

            for (ChannelSettingsListener listener : mChannelSettingsListeners) {
                mUiThreadHandler.post(() -> listener.onChannelSettingsUpdated(channelSettings.getName(), channelSettings.getPsk().toByteArray(), channelSettings.getModemConfig()));
            }
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "Exception in updateChannelStatus(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "onReceive, action was null");
                return;
            }

            switch (action) {
                case ACTION_MESH_CONNECTED:
                    String extraConnected = intent.getStringExtra(EXTRA_CONNECTED);
                    boolean connected = extraConnected.equals(STATE_CONNECTED);
                    maybeInitialConnection();
                    break;
                case ACTION_NODE_CHANGE:
                    NodeInfo nodeInfo = intent.getParcelableExtra(EXTRA_NODEINFO);
                    NonAtakUserInfo nonAtakUserInfo = nonAtakUserInfoFromNodeInfo(nodeInfo);
                    Log.e(TAG, "Node Change: " + nodeInfo.toString());
                    mUserListener.onUserUpdated(nonAtakUserInfo);
                    break;
                case ACTION_MESSAGE_STATUS:
                    int id = intent.getIntExtra(EXTRA_PACKET_ID, 0);
                    MessageStatus status = intent.getParcelableExtra(EXTRA_STATUS);
                    handleMessageStatusChange(id, status);
                    break;
                case ACTION_RECEIVED_DATA:
                    DataPacket payload = intent.getParcelableExtra(EXTRA_PAYLOAD);

                    if (payload.getDataType() == MeshProtos.Data.Type.CLEAR_TEXT_VALUE) {
                        String message = new String(payload.getBytes());
                        Log.d(TAG, "data: " + message);
                        if (message.startsWith(BCAST_MARKER)) {
                            handleDiscoveryMessage(message);
                        } else {
                            handleMessageChunk(payload.getId(), payload.getFrom(), payload.getBytes());
                        }
                    } else {
                        Log.e(TAG, "Unknown payload type: " + payload.getDataType());
                    }
                    break;
                default:
                    Log.e(TAG, "Do not know how to handle intent action: " + intent.getAction());
                    break;
            }
        }
    };

    protected void handleDiscoveryMessage(String message) {
        String messageWithoutMarker = message.replace(BCAST_MARKER + ",", "");
        String[] messageSplit = messageWithoutMarker.split(",");
        String meshId = messageSplit[0];
        String atakUid = messageSplit[1];
        String callsign = messageSplit[2];
        boolean initialDiscoveryMessage = messageSplit[3].equals("1");

        if (initialDiscoveryMessage) {
            broadcastDiscoveryMessage(false);
        }
        mUserListener.onUserDiscoveryBroadcastReceived(callsign, meshId, atakUid);
    }

    private void handleMessageStatusChange(int id, MessageStatus status) {
        mUiThreadHandler.post(() -> mMessageAckNackListener.onMessageAckNack(id, status == MessageStatus.DELIVERED));

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

    /**
     * Message Utils
     */
    private void prepareToSendMessage(int messageSize) {
        mPendingMessageReceived = false;
        mPendingMessageCountdownLatch = new CountDownLatch(1);
    }

    private void awaitPendingMessageCountDownLatch() {
        boolean timedOut = false;
        try {
            timedOut = !mPendingMessageCountdownLatch.await(MESSAGE_AWAIT_TIMEOUT_MS * (mDataRate + 1), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(timedOut) {
            Log.e(TAG, "Timed out waiting for message ACK/NACK for: " + mPendingMessageId);
            mUiThreadHandler.post(() -> mMessageAckNackListener.onMessageTimedOut(mPendingMessageId));
        }
    }
}
