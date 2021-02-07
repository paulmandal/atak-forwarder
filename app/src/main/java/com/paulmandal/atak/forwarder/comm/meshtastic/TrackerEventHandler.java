package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.geeksville.mesh.MeshUser;
import com.geeksville.mesh.NodeInfo;
import com.geeksville.mesh.Position;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class TrackerEventHandler extends MeshEventHandler {
    public interface TrackerListener {
        void onTrackerUpdated(TrackerUserInfo trackerUserInfo);
    }

    private static final String TAG = Constants.DEBUG_TAG_PREFIX + TrackerEventHandler.class.getSimpleName();

    private static final int REJECT_STALE_NODE_CHANGE_TIME_MS = Constants.REJECT_STALE_NODE_CHANGE_TIME_MS;

    private TrackerListener mTrackerListener;

    public TrackerEventHandler(Context atakContext,
                               Logger logger,
                               List<Destroyable> destroyables,
                               MeshSuspendController meshSuspendController) {
        super(atakContext,
                logger,
                new String[] {
                        MeshServiceConstants.ACTION_NODE_CHANGE
                },
                destroyables,
                meshSuspendController);
    }

    public void setListener(TrackerListener listener) {
        mTrackerListener = listener;
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        NodeInfo nodeInfo = intent.getParcelableExtra(MeshServiceConstants.EXTRA_NODEINFO);
        long timeSinceLastSeen = System.currentTimeMillis() - nodeInfo.getLastSeen() * 1000L;
        mLogger.v(TAG, "  NODE_CHANGE: " + nodeInfo + ", timeSinceLastSeen (ms): " + timeSinceLastSeen);

        TrackerUserInfo trackerUserInfo = trackerUserInfoFromNodeInfo(nodeInfo, timeSinceLastSeen);

        if (trackerUserInfo == null || timeSinceLastSeen > REJECT_STALE_NODE_CHANGE_TIME_MS) {
            // Drop updates that do not have a MeshUser attached or are >30 mins old
            return;
        }
        mTrackerListener.onTrackerUpdated(trackerUserInfo);
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
}
