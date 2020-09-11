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
import com.geeksville.mesh.MessageStatus;
import com.geeksville.mesh.NodeInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.AddToGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.CreateGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MeshtasticCommHardware extends MessageLengthLimitedCommHardware {
    public interface GroupListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);
        void onGroupCreated(String groupId, List<String> memberMeshIds);
    }

    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticCommHardware.class.getSimpleName();

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

    private static final String STATE_DISCONNECTED = "DISCONNECTED";

    private GroupListener mGroupListener;
    private Activity mActivity;

    IMeshService mMeshService;
    private ServiceConnection mServiceConnection;
    private CountDownLatch mPendingMessageCountdownLatch; // TODO: maybe move this up to MessageLengthLimitedCommHardware
    private int mPendingMessageId;
    private boolean mPendingMessageReceived;

    boolean mBound = false;

    public MeshtasticCommHardware(Handler handler,
                                  GroupListener groupListener,
                                  GroupTracker groupTracker,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  Activity activity,
                                  UserInfo selfInfo) {
        super(handler, commandQueue, queuedCommandFactory, groupTracker, Config.MESHTASTIC_MESSAGE_CHUNK_LENGTH, selfInfo);

        mActivity = activity;
        mGroupListener = groupListener;

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mBound = true;
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;
                setConnected(false);
                notifyConnectionStateListeners(ConnectionState.DISCONNECTED);
                mBound = false;
            }
        };

        Intent intent = new Intent();
        intent.setClassName("com.geeksville.mesh","com.geeksville.mesh.service.MeshService");
        activity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);
        filter.addAction(ACTION_NODE_CHANGE);
        filter.addAction(ACTION_RECEIVED_DATA);
        filter.addAction(ACTION_MESSAGE_STATUS);

        mActivity.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected boolean sendMessageSegment(byte[] message, String targetId) {
        Log.d(TAG, "sendMessageSegment");

        prepareToSendMessage();

        DataPacket dataPacket = new DataPacket(targetId, message);
        try {
            mMeshService.send(dataPacket);
            mPendingMessageId = dataPacket.getId();
            Log.d(TAG, "send message id: " + dataPacket.getId());
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessageSegment, RemoteException: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        awaitPendingMessageCountDownLatch();

        return mPendingMessageReceived;
    }

    private void setupRadio() {
        try {
            UserInfo selfInfo = getSelfInfo();
            Log.d(TAG, "setting long/short names to: " + selfInfo.callsign + ", " + selfInfo.callsign.substring(0, 1));
            mMeshService.setOwner(null, selfInfo.callsign, selfInfo.callsign.substring(0, 1));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void tryReadRadioStuff() {
        try {
            getSelfInfo().meshId = mMeshService.getMyId();
            Log.d(TAG, "myId: " + mMeshService.getMyId());
            Log.d(TAG, "My Node Info: " + mMeshService.getMyNodeInfo());
//            if (!mMeshService.connectionState().equals("CONNECTED")) {
//                return;
//            }
//            try {
//                byte[] radioConfigBytes = mMeshService.getRadioConfig();
//                if (radioConfigBytes != null) {
//                    MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
//                    Log.d(TAG, " radioConfig: " + radioConfig);
//                    MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
//                    MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();
//
//                    Log.d(TAG, " user prefs: " + userPreferences);
//                    Log.d(TAG, " channelSettings: " + channelSettings);
//                    Log.d(TAG, " channelSettings.name: " + channelSettings.getName());
//                }
//            } catch (InvalidProtocolBufferException e) {
//                e.printStackTrace();
//            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBatteryCharging() {
        return false;
    }

    @Override
    public Integer getBatteryChargePercentage() {
        return null;
    }

    @Override
    protected void handleScanForCommDevice() {
        // TODO: handle connect/disconnect in plugin
    }

    @Override
    protected void handleDisconnectFromCommDevice() {
        // TODO: handle connnect/disconnect in plugin
    }

    @Override
    protected void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        if (!sendMessageSegment(broadcastDiscoveryCommand.discoveryMessage, DataPacket.ID_BROADCAST)) {
            // Send this message back to the queue
            queueCommand(broadcastDiscoveryCommand);
        }
    }

    @Override
    protected void handleCreateGroup(CreateGroupCommand createGroupCommand) {
        // TODO: handle group creation in plugin
    }

    @Override
    protected void handleAddToGroup(AddToGroupCommand addToGroupCommand) {
        // TODO: handle group creation in plugin
    }

    @Override
    protected void handleGetBatteryStatus() {
        // TODO: handle group creation in plugin
    }

    @Override
    public void destroy() {
        super.destroy();
        mActivity.unbindService(mServiceConnection);
        mActivity.unregisterReceiver(mBroadcastReceiver);
        mBound = false;
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
                    boolean connected = !intent.getStringExtra(EXTRA_CONNECTED).equals("DISCONNECTED");
                    Log.d(TAG, "ACTION_MESH_CONNECTED: " + connected);
                    setConnected(connected);
                    setupRadio();
                    tryReadRadioStuff();
                    if (connected) {
                        broadcastDiscoveryMessage(true);
                    }
                    notifyConnectionStateListeners(connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED);
                    break;
                case ACTION_NODE_CHANGE:
                    NodeInfo nodeInfo = intent.getParcelableExtra(EXTRA_NODEINFO);
                    Log.d(TAG, "ACTION_NODE_CHANGE, info: " + nodeInfo);
                    try {
                        List<NodeInfo> nodes = mMeshService.getNodes();
                        for (NodeInfo nodeInfoItem : nodes) {
                            Log.d(TAG, "  node info: " + nodeInfoItem);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    tryReadRadioStuff();
                    break;
                case ACTION_MESSAGE_STATUS:
                    Log.d(TAG, "ACTION_MESSAGE_STATUS");

                    int id = intent.getIntExtra(EXTRA_PACKET_ID, 0);
                    MessageStatus status = intent.getParcelableExtra(EXTRA_STATUS);

                    Log.d(TAG, "id: " + id + " status: " + status);

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
                            handleMessageChunk(payload.getFrom(), payload.getBytes());
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

        Log.d(TAG, "handleDiscoveryMessage, meshId: " + meshId + ", atakUid: " + atakUid + ", callsign: " + callsign);

        if (initialDiscoveryMessage) {
            broadcastDiscoveryMessage(false);
        }
        mGroupListener.onUserDiscoveryBroadcastReceived(callsign, meshId, atakUid);
    }

    private void handleMessageStatusChange(int id, MessageStatus status) {
        if (id != mPendingMessageId) {
            Log.e(TAG, "handleMessageStatusChange for a msg we don't care about id: " + id + " status: " + status);
            return;
        }

        // TODO: fix this, for some reason the MeshService is reporting ERROR for all msgs but they are actually going through
        mPendingMessageReceived = true; // status != MessageStatus.ERROR;

        if (status == MessageStatus.ERROR || status == MessageStatus.DELIVERED) {
            mPendingMessageCountdownLatch.countDown();
        }
    }

    private void prepareToSendMessage() {
        mPendingMessageCountdownLatch = new CountDownLatch(1);
    }

    private void awaitPendingMessageCountDownLatch() {
        try {
            mPendingMessageCountdownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
