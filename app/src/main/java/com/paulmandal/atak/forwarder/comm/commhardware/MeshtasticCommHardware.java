package com.paulmandal.atak.forwarder.comm.commhardware;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.MeshUser;
import com.geeksville.mesh.MessageStatus;
import com.geeksville.mesh.NodeInfo;
import com.geeksville.mesh.Portnums;
import com.geeksville.mesh.Position;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.NonAtakUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticChannelConfigurer;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDeviceConfigurer;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.persistence.PreferencesDefaults;
import com.paulmandal.atak.forwarder.persistence.PreferencesKeys;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MeshtasticCommHardware extends MessageLengthLimitedCommHardware {
    public interface UserListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);

        void onUserUpdated(NonAtakUserInfo nonAtakUserInfo);
    }

    public interface MessageAckNackListener {
        void onMessageAckNack(int messageId, boolean isAck);

        void onMessageTimedOut(int messageId);
    }

    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticCommHardware.class.getSimpleName();

    private static final int MESSAGE_AWAIT_TIMEOUT_MS = Config.MESSAGE_AWAIT_TIMEOUT_MS;
    private static final int DELAY_AFTER_STOPPING_SERVICE = Config.DELAY_AFTER_STOPPING_SERVICE;
    private static final int REJECT_STALE_NODE_CHANGE_TIME_MS = Config.REJECT_STALE_NODE_CHANGE_TIME_MS;

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

    private MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private MeshtasticDeviceConfigurer mMeshtasticDeviceConfigurer;
    private MeshtasticChannelConfigurer mMeshtasticChannelConfigurer;

    private UserListener mUserListener;
    private SharedPreferences mSharedPreferences;
    private Context mAtakContext;
    private Handler mUiThreadHandler;

    private IntentFilter mIntentFilter;

    private IMeshService mMeshService;
    private ServiceConnection mServiceConnection;

    private final List<MessageAckNackListener> mMessageAckNackListeners = new CopyOnWriteArrayList<>();

    private CountDownLatch mPendingMessageCountdownLatch; // TODO: maybe move this up to MessageLengthLimitedCommHardware
    private int mPendingMessageId;
    private boolean mPendingMessageReceived;

    private Intent mServiceIntent;

    private boolean mConnectedToService = false;
    private boolean mSetDeviceAddressCalled = false;
    private boolean mInitialRadioConfigurationDone = false;
    private boolean mInitialChannelSetupDone = false;

    private String mChannelName;
    private int mChannelMode;
    private byte[] mChannelPsk;

    private int mDataRate;
    private MeshtasticDevice mCommDevice;

    public MeshtasticCommHardware(List<Destroyable> destroyables,
                                  SharedPreferences sharedPreferences,
                                  Context atakContext,
                                  Handler uiThreadHandler,
                                  MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                  MeshtasticDeviceConfigurer meshtasticDeviceConfigurer,
                                  MeshtasticChannelConfigurer meshtasticChannelConfigurer,
                                  UserListener userListener,
                                  UserTracker userTracker,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  UserInfo selfInfo) {
        super(destroyables,
                sharedPreferences,
                new String[]{},
                new String[]{
                        PreferencesKeys.KEY_SET_COMM_DEVICE,
                        PreferencesKeys.KEY_CHANNEL_NAME,
                        PreferencesKeys.KEY_CHANNEL_MODE,
                        PreferencesKeys.KEY_CHANNEL_PSK
                },
                uiThreadHandler,
                commandQueue,
                queuedCommandFactory,
                userTracker,
                Config.MESHTASTIC_MESSAGE_CHUNK_LENGTH,
                selfInfo);

        mSharedPreferences = sharedPreferences;
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mMeshtasticDeviceConfigurer = meshtasticDeviceConfigurer;
        mMeshtasticChannelConfigurer = meshtasticChannelConfigurer;
        mUserListener = userListener;

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mConnectedToService = true;

                if (!mSetDeviceAddressCalled) {
                    Log.e(TAG, "complexUpdate(sharedPreferences, PreferencesKeys.KEY_SET_COMM_DEVICE)");
                    complexUpdate(sharedPreferences, PreferencesKeys.KEY_SET_COMM_DEVICE);
                    mSetDeviceAddressCalled = true;
                    return;
                }

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

        mAtakContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public void addMessageAckNackListener(MessageAckNackListener listener) {
        mMessageAckNackListeners.add(listener);
    }

    @Override
    protected boolean sendMessageSegment(byte[] message, String targetId) {
        prepareToSendMessage(message.length);

        DataPacket dataPacket = new DataPacket(targetId,
                message,
                Portnums.PortNum.UNKNOWN_APP.getNumber(),
                DataPacket.ID_LOCAL,
                System.currentTimeMillis(),
                0,
                MessageStatus.UNKNOWN);
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

    @Override
    public void connect() {
        if (getConnectionState() == ConnectionState.DEVICE_CONNECTED) {
            Log.d(TAG, "connect: already connected");
            return;
        }
        unbindAndStopService();
        bindToService();
    }

    public void suspendResume(boolean suspended) { // TODO: rename
        if (suspended) {
            mAtakContext.unregisterReceiver(mBroadcastReceiver);
            setConnectionState(ConnectionState.DEVICE_DISCONNECTED);
        } else {
            mAtakContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
            setConnectionState(ConnectionState.DEVICE_CONNECTED);
        }
    }

    public MeshtasticDevice getDevice() {
        return mCommDevice;
    }

    @Override
    protected void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        if (!sendMessageSegment(broadcastDiscoveryCommand.discoveryMessage, DataPacket.ID_BROADCAST)) {
            // Send this message back to the queue
            queueCommand(broadcastDiscoveryCommand);
        }
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        super.onDestroy(context, mapView);
        mAtakContext.unbindService(mServiceConnection);
        mAtakContext.unregisterReceiver(mBroadcastReceiver);
        mConnectedToService = false;
    }

    private void bindToService() {
        mAtakContext.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindAndStopService() {
        if (!mConnectedToService) {
            return;
        }

        mAtakContext.unbindService(mServiceConnection);
        mAtakContext.stopService(mServiceIntent);

        try {
            Thread.sleep(DELAY_AFTER_STOPPING_SERVICE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void maybeInitialConnection() {
        ConnectionState oldConnectionState = getConnectionState();

        updateConnectionState();

        boolean connected = getConnectionState() == ConnectionState.DEVICE_CONNECTED;

        if (connected) {
            updateMeshId();
            maybeSetupRadio();
        }

        if (oldConnectionState != getConnectionState() && connected) {
            broadcastDiscoveryMessage(true);
        }
    }

    private void updateConnectionState() {
        try {
            String meshId = mMeshService.getMyId();
            ConnectionState connectionState;
            if (mCommDevice == null) {
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

    private void updateMeshId() {
        try {
            getSelfInfo().meshId = mMeshService.getMyId();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in updateMeshId(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void maybeSetupRadio() {
        if (!mInitialRadioConfigurationDone) {
            Log.e(TAG, "mMeshtasticDeviceConfigurer.configureDevice(mMeshService)");
            mMeshtasticDeviceConfigurer.configureDevice(mMeshService);
            mInitialRadioConfigurationDone = true;
        }

        if (!mInitialChannelSetupDone) {
            Log.e(TAG, "complexUpdate(mSharedPreferences, PreferencesKeys.KEY_CHANNEL_NAME)");
            complexUpdate(mSharedPreferences, PreferencesKeys.KEY_CHANNEL_NAME);
            mInitialChannelSetupDone = true;
        }
    }

    @Nullable
    private NonAtakUserInfo nonAtakUserInfoFromNodeInfo(NodeInfo nodeInfo) {
        MeshUser meshUser = nodeInfo.getUser();

        if (meshUser == null) {
            return null;
        }

        double lat = 0.0;
        double lon = 0.0;
        int altitude = 0;
        boolean gpsValid = false;
        Position position = nodeInfo.getValidPosition();
        if (position != null) {
            lat = position.getLatitude();
            lon = position.getLongitude();
            altitude = position.getAltitude();
            gpsValid = true;
        }
        return new NonAtakUserInfo(meshUser.getLongName(), meshUser.getId(), nodeInfo.getBatteryPctLevel(), lat, lon, altitude, gpsValid, meshUser.getShortName());
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
                    Log.d(TAG, "MESH_CONNECTED: " + connected);
                    maybeInitialConnection();
                    break;
                case ACTION_NODE_CHANGE:
                    NodeInfo nodeInfo = intent.getParcelableExtra(EXTRA_NODEINFO);
                    long timeSinceLastSeen = System.currentTimeMillis() - nodeInfo.getLastSeen() * 1000L;
                    Log.d(TAG, "NODE_CHANGE: " + nodeInfo + ", timeSinceLastSeen (ms): " + timeSinceLastSeen);

                    NonAtakUserInfo nonAtakUserInfo = nonAtakUserInfoFromNodeInfo(nodeInfo);

                    if (nonAtakUserInfo == null || timeSinceLastSeen > REJECT_STALE_NODE_CHANGE_TIME_MS) {
                        // Drop updates that do not have a MeshUser attached or are >30 mins old
                        return;
                    }
                    mUserListener.onUserUpdated(nonAtakUserInfo);
                    break;
                case ACTION_MESSAGE_STATUS:
                    int id = intent.getIntExtra(EXTRA_PACKET_ID, 0);
                    MessageStatus status = intent.getParcelableExtra(EXTRA_STATUS);
                    handleMessageStatusChange(id, status);
                    break;
                case ACTION_RECEIVED_DATA:
                    DataPacket payload = intent.getParcelableExtra(EXTRA_PAYLOAD);

                    if (payload.getDataType() == Portnums.PortNum.UNKNOWN_APP.getNumber()) {
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

        if (timedOut) {
            Log.e(TAG, "Timed out waiting for message ACK/NACK for: " + mPendingMessageId);
            mUiThreadHandler.post(() -> {
                for (MessageAckNackListener messageAckNackListener : mMessageAckNackListeners) {
                    messageAckNackListener.onMessageTimedOut(mPendingMessageId);
                }
            });
        }
    }

    /**
     * Config State Handling
     */
    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        // Nothing yet
        Log.e(TAG, "updateSettings");
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        Log.e(TAG, "complexUpdate: " + key);
        mUiThreadHandler.post(() -> {
            switch (key) {
                case PreferencesKeys.KEY_SET_COMM_DEVICE:
                    String commDeviceStr = sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE);
                    Gson gson = new Gson();
                    MeshtasticDevice meshtasticDevice = gson.fromJson(commDeviceStr, MeshtasticDevice.class);

                    Log.e(TAG, "complexUpdate.KEY_COMM_DEVICE: " + meshtasticDevice.address);
                    try {
                        mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, meshtasticDevice);
                        mCommDevice = meshtasticDevice;

                        connect();
                        Log.e(TAG, "setDeviceAddress success");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case PreferencesKeys.KEY_CHANNEL_NAME:
                case PreferencesKeys.KEY_CHANNEL_MODE:
                case PreferencesKeys.KEY_CHANNEL_PSK:
                    String channelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
                    int channelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
                    byte[] psk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);

                    boolean changed = !channelName.equals(mChannelName) || !(channelMode != mChannelMode) || !compareByteArrays(psk, mChannelPsk);

                    if (changed) {
                        mChannelName = channelName;
                        mChannelMode = channelMode;
                        mChannelPsk = psk;
                        Log.e(TAG, "Updating channel settings: " + channelName + ", " + channelMode + ", " + new String(psk));

                        MeshProtos.ChannelSettings.ModemConfig modemConfig = MeshProtos.ChannelSettings.ModemConfig.forNumber(channelMode);

                        mMeshtasticChannelConfigurer.updateChannelSettings(mMeshService, channelName, psk, modemConfig);
                    }
                    break;
            }
        });
    }

    private boolean compareByteArrays(byte[] lhs, byte[] rhs) {
        if (lhs.length != rhs.length) {
            return false;
        }

        for (int i = 0 ; i < lhs.length ; i++) {
            if (lhs[i] != rhs[i]) {
                return false;
            }
        }
        return true;
    }
}
