package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import com.geeksville.mesh.DataPacket;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class DiscoveryBroadcastEventHandler extends MeshEventHandler implements ConnectionStateHandler.Listener {
    public interface DiscoveryBroadcastListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + DiscoveryBroadcastEventHandler.class.getSimpleName();

    private DiscoveryBroadcastListener mDiscoveryBroadcastListener;
    private final CommandQueue mCommandQueue;
    private final QueuedCommandFactory mQueuedCommandFactory;
    private final MeshServiceController mMeshServiceController;

    private String mMeshId;
    private final String mAtakUid;
    private final String mCallsign;
    private boolean mInitialDiscoveryBroadcastSent = false;

    public DiscoveryBroadcastEventHandler(Context atakContext,
                                          Logger logger,
                                          CommandQueue commandQueue,
                                          QueuedCommandFactory queuedCommandFactory,
                                          List<Destroyable> destroyables,
                                          ConnectionStateHandler connectionStateHandler,
                                          MeshServiceController meshServiceController,
                                          String atakUid,
                                          String callsign) {
        super(atakContext,
                logger,
                new String[]{
                        MeshServiceConstants.ACTION_RECEIVED_ATAK_FORWARDER
                },
                destroyables,
                connectionStateHandler);

        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mMeshServiceController = meshServiceController;
        mAtakUid = atakUid;
        mCallsign = callsign;

        mMeshId = "";

        connectionStateHandler.addListener(this);
    }

    public void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = ForwarderConstants.DISCOVERY_BROADCAST_MARKER + "," + mMeshId + "," + mAtakUid + "," + mCallsign + "," + (initialDiscoveryMessage ? 1 : 0);

        String broadcastWithInitialDiscoveryUnset = broadcastData.replaceAll(",1$", ",0");
        handleDiscoveryMessage(broadcastWithInitialDiscoveryUnset);

        mCommandQueue.queueCommand(mQueuedCommandFactory.createBroadcastDiscoveryCommand(broadcastData.getBytes()));
    }

    public void setListener(DiscoveryBroadcastListener listener) {
        mDiscoveryBroadcastListener = listener;
    }

    @Override
    public void onConnectionStateChanged(ConnectionStateHandler.ConnectionState connectionState) {
        super.onConnectionStateChanged(connectionState);

        if (connectionState == ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED) {
            mMeshId = null;
            try {
                mMeshId = mMeshServiceController.getMeshService().getMyId();
            } catch (RemoteException e) {
                mLogger.e(TAG, "Exception getting meshId");
                e.printStackTrace();
            }

            if (mMeshId != null && !mInitialDiscoveryBroadcastSent) {
                broadcastDiscoveryMessage(true);
                mInitialDiscoveryBroadcastSent = true;
            }
        }
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        DataPacket payload = intent.getParcelableExtra(MeshServiceConstants.EXTRA_PAYLOAD);

        int dataType = payload.getDataType();

        mLogger.v(TAG, "handleReceive(), dataType: " + dataType);
        String message = new String(payload.getBytes()).substring(1);
        if (!message.startsWith(ForwarderConstants.DISCOVERY_BROADCAST_MARKER)) {
            return;
        }

        mLogger.d(TAG, "<--- Received broadcast: " + message);

        handleDiscoveryMessage(message);
    }

    private void handleDiscoveryMessage(String message) {
        String messageWithoutMarker = message.replace(ForwarderConstants.DISCOVERY_BROADCAST_MARKER + ",", "");
        String[] messageSplit = messageWithoutMarker.split(",");
        String meshId = messageSplit[0];
        String atakUid = messageSplit[1];
        String callsign = messageSplit[2];
        boolean initialDiscoveryMessage = messageSplit[3].equals("1");

        if (initialDiscoveryMessage && mInitialDiscoveryBroadcastSent) {
            broadcastDiscoveryMessage(false);
        }
        mDiscoveryBroadcastListener.onUserDiscoveryBroadcastReceived(callsign, meshId, atakUid);
    }
}
