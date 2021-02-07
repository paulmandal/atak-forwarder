package com.paulmandal.atak.forwarder.comm.refactor;

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
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshServiceConstants;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MeshtasticCommHardware extends MessageLengthLimitedCommHardware {
    public interface UserListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);
        void onUserUpdated(TrackerUserInfo trackerUserInfo);
    }

    public interface MessageAckNackListener {
        void onMessageAckNack(int messageId, boolean isAck);
        void onMessageTimedOut(int messageId);
    }

    private static final String TAG = Constants.DEBUG_TAG_PREFIX + MeshtasticCommHardware.class.getSimpleName();

    private static final int MESSAGE_AWAIT_TIMEOUT_MS = Constants.MESSAGE_AWAIT_TIMEOUT_MS;
    private static final int DELAY_AFTER_STOPPING_SERVICE = Constants.DELAY_AFTER_STOPPING_SERVICE;
    private static final int REJECT_STALE_NODE_CHANGE_TIME_MS = Constants.REJECT_STALE_NODE_CHANGE_TIME_MS;

    private MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private MeshtasticDeviceConfigurer mMeshtasticDeviceConfigurer;
    private MeshtasticChannelConfigurer mMeshtasticChannelConfigurer;

    private UserListener mUserListener;
    private SharedPreferences mSharedPreferences;
    private Context mAtakContext;
    private Handler mUiThreadHandler;


    private final List<MessageAckNackListener> mMessageAckNackListeners = new CopyOnWriteArrayList<>();

    private CountDownLatch mPendingMessageCountdownLatch; // TODO: maybe move this up to MessageLengthLimitedCommHardware
    private int mPendingMessageId;
    private boolean mPendingMessageReceived;


    private boolean mConnectedToService = false;
    private boolean mSetDeviceAddressCalled = false;
    private boolean mInitialRadioConfigurationDone = false;
    private boolean mInitialChannelSetupDone = false;

    private String mChannelName;
    private int mChannelMode;
    private byte[] mChannelPsk;

    private int mPliHopLimit;
    private int mChatHopLimit;
    private int mOtherHopLimit;

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
                Constants.MESHTASTIC_MESSAGE_CHUNK_LENGTH,
                selfInfo);

        mSharedPreferences = sharedPreferences;
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mMeshtasticDeviceConfigurer = meshtasticDeviceConfigurer;
        mMeshtasticChannelConfigurer = meshtasticChannelConfigurer;
        mUserListener = userListener;



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






    private void maybeInitialConnection() {
        CommHardware.ConnectionState oldConnectionState = getConnectionState();

        updateConnectionState();

        boolean connected = getConnectionState() == CommHardware.ConnectionState.DEVICE_CONNECTED;

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
            CommHardware.ConnectionState connectionState;
            if (mCommDevice == null) {
                connectionState = CommHardware.ConnectionState.NO_DEVICE_CONFIGURED;
            } else {
                boolean connected = mMeshService.connectionState().equals(MeshServiceConstants.STATE_CONNECTED);
                connectionState = connected ? CommHardware.ConnectionState.DEVICE_CONNECTED : CommHardware.ConnectionState.DEVICE_DISCONNECTED;
            }

            setConnectionState(connectionState);
            notifyConnectionStateListeners(connectionState);
            Log.v(TAG, "  ConnectionState: " + connectionState);
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
            Log.v(TAG, "maybeSetupRadio, calling configureDevice()");
            mInitialRadioConfigurationDone = mMeshtasticDeviceConfigurer.configureDevice(mMeshService);
        }

        if (!mInitialChannelSetupDone) {
            complexUpdate(mSharedPreferences, PreferencesKeys.KEY_CHANNEL_NAME);
            mInitialChannelSetupDone = true;
        }
    }

    @Nullable
    private TrackerUserInfo trackerUserInfoFromNodeInfo(NodeInfo nodeInfo, long timeSinceLastSeen) {
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
        return new TrackerUserInfo(meshUser.getLongName(), meshUser.getId(), nodeInfo.getBatteryPctLevel(), lat, lon, altitude, gpsValid, meshUser.getShortName(), timeSinceLastSeen);
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
                case MeshServiceConstants.ACTION_MESH_CONNECTED:
                    String extraConnected = intent.getStringExtra(MeshServiceConstants.EXTRA_CONNECTED);
                    boolean connected = extraConnected.equals(MeshServiceConstants.STATE_CONNECTED);
                    Log.d(TAG, "MESH_CONNECTED: " + connected);
                    maybeInitialConnection();
                    break;
                case MeshServiceConstants.ACTION_NODE_CHANGE:
                    NodeInfo nodeInfo = intent.getParcelableExtra(MeshServiceConstants.EXTRA_NODEINFO);
                    long timeSinceLastSeen = System.currentTimeMillis() - nodeInfo.getLastSeen() * 1000L;
                    Log.v(TAG, "NODE_CHANGE: " + nodeInfo + ", timeSinceLastSeen (ms): " + timeSinceLastSeen);

                    TrackerUserInfo trackerUserInfo = trackerUserInfoFromNodeInfo(nodeInfo, timeSinceLastSeen);

                    if (trackerUserInfo == null || timeSinceLastSeen > REJECT_STALE_NODE_CHANGE_TIME_MS) {
                        // Drop updates that do not have a MeshUser attached or are >30 mins old
                        return;
                    }
                    mUserListener.onUserUpdated(trackerUserInfo);
                    break;
                case MeshServiceConstants.ACTION_MESSAGE_STATUS:
                    int id = intent.getIntExtra(MeshServiceConstants.EXTRA_PACKET_ID, 0);
                    MessageStatus status = intent.getParcelableExtra(MeshServiceConstants.EXTRA_STATUS);
                    handleMessageStatusChange(id, status);
                    break;
                case MeshServiceConstants.ACTION_RECEIVED_DATA:

                    break;
                default:
                    Log.e(TAG, "Do not know how to handle intent action: " + intent.getAction());
                    break;
            }
        }
    };

    protected void handleDiscoveryMessage(String message) {
        String messageWithoutMarker = message.replace(Constants.DISCOVERY_BROADCAST_MARKER + ",", "");
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
//            timedOut = !mPendingMessageCountdownLatch.await(600, TimeUnit.SECONDS);
            timedOut = !mPendingMessageCountdownLatch.await(MESSAGE_AWAIT_TIMEOUT_MS * (mChannelMode + 1), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted waiting for pending message: " + mPendingMessageId);
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
        mPliHopLimit = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_PLI_HOP_LIMIT, PreferencesDefaults.DEFAULT_PLI_HOP_LIMIT));
        mChatHopLimit = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHAT_HOP_LIMIT, PreferencesDefaults.DEFAULT_CHAT_HOP_LIMIT));
        mOtherHopLimit = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_OTHER_HOP_LIMIT, PreferencesDefaults.DEFAULT_OTHER_HOP_LIMIT));
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        new Thread(() -> {
            switch (key) {
                case PreferencesKeys.KEY_SET_COMM_DEVICE:
                    String commDeviceStr = sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE);
                    Gson gson = new Gson();
                    MeshtasticDevice meshtasticDevice = gson.fromJson(commDeviceStr, MeshtasticDevice.class);

                    if (meshtasticDevice == null) {
                        return;
                    }

                    try {
                        Log.v(TAG, "complexUpdate, calling setDeviceAddress: " + meshtasticDevice);
                        mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, meshtasticDevice);
                        mCommDevice = meshtasticDevice;
                        mSetDeviceAddressCalled = true;

                        connect();
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

                        MeshProtos.ChannelSettings.ModemConfig modemConfig = MeshProtos.ChannelSettings.ModemConfig.forNumber(channelMode);

                        Log.v(TAG, "complexUpdate, updating channel settings: " + channelName + ", " + channelMode);
                        mMeshtasticChannelConfigurer.updateChannelSettings(mMeshService, channelName, psk, modemConfig);
                    }
                    break;
            }
        }).start();
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
