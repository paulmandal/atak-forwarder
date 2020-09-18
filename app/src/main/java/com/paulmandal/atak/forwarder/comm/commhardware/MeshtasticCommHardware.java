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
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.UpdateChannelCommand;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.channel.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MeshtasticCommHardware extends MessageLengthLimitedCommHardware {
    public interface ChannelListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);
        void onChannelMembersUpdated(List<UserInfo> userInfoList);
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

    private ChannelTracker mChannelTracker;
    private ChannelListener mChannelListener;
    private List<ChannelSettingsListener> mChannelSettingsListeners = new ArrayList<>();
    private MessageAckNackListener mMessageAckNackListener;
    private Activity mActivity;

    IMeshService mMeshService;
    private ServiceConnection mServiceConnection;

    private CountDownLatch mPendingMessageCountdownLatch; // TODO: maybe move this up to MessageLengthLimitedCommHardware
    private int mPendingMessageId;
    private boolean mPendingMessageReceived;

    private Intent mServiceIntent;

    boolean mBound = false;
    boolean mRadioSetupCalled = false;

    private int mDataRate;

    public MeshtasticCommHardware(Handler handler,
                                  ChannelListener channelListener,
                                  ChannelTracker channelTracker,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  Activity activity,
                                  UserInfo selfInfo) {
        super(handler, commandQueue, queuedCommandFactory, channelTracker, Config.MESHTASTIC_MESSAGE_CHUNK_LENGTH, selfInfo);

        mActivity = activity;
        mChannelListener = channelListener;
        mChannelTracker = channelTracker;

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mBound = true;

                maybeInitialConnection();
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;

                setConnectionState(ConnectionState.DISCONNECTED);
                notifyConnectionStateListeners(ConnectionState.DISCONNECTED);
                mBound = false;
            }
        };

        mServiceIntent = new Intent();
        mServiceIntent.setClassName("com.geeksville.mesh","com.geeksville.mesh.service.MeshService");

        bindToService();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);
        filter.addAction(ACTION_NODE_CHANGE);
        filter.addAction(ACTION_RECEIVED_DATA);
        filter.addAction(ACTION_MESSAGE_STATUS);

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
            Log.e(TAG, "Exception in handleUpdateChannel(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void connect() {
        if (getConnectionState() == ConnectionState.CONNECTED) {
            Log.d(TAG, "connect: already connected");
            return;
        }
        unbindAndStopService();
        bindToService();
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
        mBound = false;
    }

    private void bindToService() {
        mActivity.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindAndStopService() {
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

        updateConnectionState();

        boolean connected = getConnectionState() == ConnectionState.CONNECTED;

        if (connected) {
            maybeSetupRadio();
            updateMeshId();
            updateChannelMembers();
            updateChannelStatus();
        }

        if (oldConnectionState != getConnectionState() && connected) {
            broadcastDiscoveryMessage(true);
        }

        notifyConnectionStateListeners(connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED);
    }

    private void updateConnectionState() {
        try {
            String meshId = mMeshService.getMyId();
            ConnectionState connectionState;
            if (meshId == null) {
                connectionState = ConnectionState.UNPAIRED;
            } else {
                boolean connected = mMeshService.connectionState().equals(STATE_CONNECTED);
                connectionState = connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED;
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
            List<UserInfo> userInfoList = new ArrayList<>();
            for (NodeInfo nodeInfoItem : nodes) {
                MeshUser meshUser = nodeInfoItem.getUser();
                userInfoList.add(new UserInfo(meshUser.getLongName(), meshUser.getId(), null, true, nodeInfoItem.getBatteryPctLevel()));
            }
            mChannelListener.onChannelMembersUpdated(userInfoList);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in updateChannelMembers(): " + e.getMessage());
            e.printStackTrace();
        }
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
                listener.onChannelSettingsUpdated(channelSettings.getName(), channelSettings.getPsk().toByteArray(), channelSettings.getModemConfig());
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
            Log.d(TAG, "onReceive: " + action);

            if (action == null) {
                Log.e(TAG, "onReceive, action was null");
                return;
            }

            switch (action) {
                case ACTION_MESH_CONNECTED:
                    String extraConnected = intent.getStringExtra(EXTRA_CONNECTED);
                    boolean connected = extraConnected.equals(STATE_CONNECTED);
                    Log.d(TAG, "ACTION_MESH_CONNECTED: " + connected + ", extra: " + extraConnected);

                    maybeInitialConnection();
                    break;
                case ACTION_NODE_CHANGE:
                    NodeInfo nodeInfo = intent.getParcelableExtra(EXTRA_NODEINFO);
                    Log.d(TAG, "ACTION_NODE_CHANGE: " + nodeInfo);

                    updateChannelMembers();
                    updateChannelStatus();
                    break;
                case ACTION_MESSAGE_STATUS:
                    Log.d(TAG, "ACTION_MESSAGE_STATUS");
                    int id = intent.getIntExtra(EXTRA_PACKET_ID, 0);
                    MessageStatus status = intent.getParcelableExtra(EXTRA_STATUS);
                    handleMessageStatusChange(id, status);
                    break;
                case ACTION_RECEIVED_DATA:
                    Log.d(TAG, "ACTION_RECEIVED_DATA");

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

    private void handleDiscoveryMessage(String message) {
        String messageWithoutMarker = message.replace(BCAST_MARKER + ",", "");
        String[] messageSplit = messageWithoutMarker.split(",");
        String meshId = messageSplit[0];
        String atakUid = messageSplit[1];
        String callsign = messageSplit[2];
        boolean initialDiscoveryMessage = messageSplit[3].equals("1");

        if (initialDiscoveryMessage) {
            broadcastDiscoveryMessage(false);
        }
        mChannelListener.onUserDiscoveryBroadcastReceived(callsign, meshId, atakUid);
    }

    private void handleMessageStatusChange(int id, MessageStatus status) {
        mMessageAckNackListener.onMessageAckNack(id, status == MessageStatus.DELIVERED);

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
            mMessageAckNackListener.onMessageTimedOut(mPendingMessageId);
        }
    }
}
