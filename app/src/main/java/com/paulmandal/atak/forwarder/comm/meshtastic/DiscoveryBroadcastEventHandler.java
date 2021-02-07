package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;

import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.Portnums;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class DiscoveryBroadcastEventHandler extends MeshEventHandler implements MeshServiceController.ConnectionStateListener {
    public interface DiscoveryBroadcastListener {
        void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid);
    }

    private final DiscoveryBroadcastListener mDiscoveryBroadcastListener;
    private final CommandQueue mCommandQueue;
    private final QueuedCommandFactory mQueuedCommandFactory;
    private final MeshServiceController mMeshServiceController;

    private String mMeshId;
    private String mAtakUid;
    private String mCallsign;
    private boolean mInitialDiscoveryBroadcastSent = false;

    public DiscoveryBroadcastEventHandler(Context atakContext,
                                          Logger logger,
                                          DiscoveryBroadcastListener discoveryBroadcastListener,
                                          CommandQueue commandQueue,
                                          QueuedCommandFactory queuedCommandFactory,
                                          List<Destroyable> destroyables,
                                          MeshSuspendController meshSuspendController,
                                          MeshServiceController meshServiceController,
                                          String atakUid,
                                          String callsign) {
        super(atakContext,
                logger,
                new String[]{
                        MeshServiceConstants.ACTION_RECEIVED_DATA
                },
                destroyables,
                meshSuspendController);

        mDiscoveryBroadcastListener = discoveryBroadcastListener;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mMeshServiceController = meshServiceController;
        mAtakUid = atakUid;
        mCallsign = callsign;

        mMeshId = "";

        meshServiceController.addConnectionStateListener(this);
    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {
        if (connectionState == ConnectionState.DEVICE_CONNECTED) {
            mMeshId = mMeshServiceController.getMeshId();

            if (mMeshId != null && !mInitialDiscoveryBroadcastSent) {
                broadcastDiscoveryMessage(true);
            }
        }
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        DataPacket payload = intent.getParcelableExtra(MeshServiceConstants.EXTRA_PAYLOAD);

        int dataType = payload.getDataType();

        if (dataType != Portnums.PortNum.UNKNOWN_APP.getNumber()) {
            return;
        }

        String message = new String(payload.getBytes());
        if (!message.startsWith(Constants.DISCOVERY_BROADCAST_MARKER)) {
            return;
        }

        handleDiscoveryMessage(message);
    }

    private void handleDiscoveryMessage(String message) {
        String messageWithoutMarker = message.replace(Constants.DISCOVERY_BROADCAST_MARKER + ",", "");
        String[] messageSplit = messageWithoutMarker.split(",");
        String meshId = messageSplit[0];
        String atakUid = messageSplit[1];
        String callsign = messageSplit[2];
        boolean initialDiscoveryMessage = messageSplit[3].equals("1");

        if (initialDiscoveryMessage) {
            broadcastDiscoveryMessage(false);
        }
        mDiscoveryBroadcastListener.onUserDiscoveryBroadcastReceived(callsign, meshId, atakUid);
    }

    private void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = Constants.DISCOVERY_BROADCAST_MARKER + "," + mMeshId + "," + mAtakUid + "," + mCallsign + "," + (initialDiscoveryMessage ? 1 : 0);

        String broadcastWithInitialDiscoveryUnset = broadcastData.replaceAll(",1$", ",0");
        handleDiscoveryMessage(broadcastWithInitialDiscoveryUnset);

        mCommandQueue.queueCommand(mQueuedCommandFactory.createBroadcastDiscoveryCommand(broadcastData.getBytes()));
    }
}
