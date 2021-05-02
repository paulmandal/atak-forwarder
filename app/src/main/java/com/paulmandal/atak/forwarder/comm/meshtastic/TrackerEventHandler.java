package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;

import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.Portnums;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class TrackerEventHandler extends MeshEventHandler {
    public interface TrackerListener {
        void onTrackerUpdated(TrackerUserInfo trackerUserInfo);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + TrackerEventHandler.class.getSimpleName();

//    private static final int REJECT_STALE_NODE_CHANGE_TIME_MS = ForwarderConstants.REJECT_STALE_NODE_CHANGE_TIME_MS;
    private static final double LAT_LON_INT_TO_DOUBLE_CONVERSION = 10000D;

    private TrackerListener mTrackerListener;

    public TrackerEventHandler(Context atakContext,
                               Logger logger,
                               List<Destroyable> destroyables,
                               MeshSuspendController meshSuspendController) {
        super(atakContext,
                logger,
                new String[] {
                        MeshServiceConstants.ACTION_RECEIVED_POSITION_APP,
                        MeshServiceConstants.ACTION_RECEIVED_NODEINFO_APP,
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
//        if (intent.getAction() != null && intent.getAction().equals(MeshServiceConstants.ACTION_NODE_CHANGE)) {
//            // TODO: this is probably not supported anymore, remove it a few Meshtastic versions after 1.2
//            NodeInfo nodeInfo = intent.getParcelableExtra(MeshServiceConstants.EXTRA_NODEINFO);
//            long timeSinceLastSeen = System.currentTimeMillis() - nodeInfo.getLastSeen() * 1000L;
//            mLogger.v(TAG, "  NODE_CHANGE: " + nodeInfo + ", timeSinceLastSeen (ms): " + timeSinceLastSeen);
//
//            TrackerUserInfo trackerUserInfo = trackerUserInfoFromNodeInfo(nodeInfo, timeSinceLastSeen);
//
//            if (trackerUserInfo == null || timeSinceLastSeen > REJECT_STALE_NODE_CHANGE_TIME_MS) {
//                mLogger.v(TAG, "    dropping NODE_CHANGE that didn't have a user: " + (trackerUserInfo == null) + " or was old, timeSinceLastSeen: " + timeSinceLastSeen);
//                // Drop updates that do not have a MeshUser attached or are >30 mins old
//                return;
//            }
//            mTrackerListener.onTrackerUpdated(trackerUserInfo);
//            return;
//        }

        DataPacket payload = intent.getParcelableExtra(MeshServiceConstants.EXTRA_PAYLOAD);
        int dataType = payload.getDataType();

        if (dataType == Portnums.PortNum.NODEINFO_APP.getNumber()) {
            mLogger.d(TAG, "  NODEINFO_APP, parsing");
            try {
                MeshProtos.User meshUser = MeshProtos.User.parseFrom(payload.getBytes());
                mLogger.d(TAG, "    parsed NodeInfo: " + meshUser.getId() + ", longName: " + meshUser.getLongName() + ", shortName: " + meshUser.getShortName());

                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(meshUser.getLongName(), meshUser.getId(), null, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, false, meshUser.getShortName(), 0);

                mTrackerListener.onTrackerUpdated(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "    NODEINFO_APP message failed to parse");
                e.printStackTrace();
            }
        } else if (dataType == Portnums.PortNum.POSITION_APP.getNumber()) {
            mLogger.d(TAG, "  POSITION_APP, parsing");
            try {
                MeshProtos.Position position = MeshProtos.Position.parseFrom(payload.getBytes());
                mLogger.d(TAG, "    parsed position: lat: " + position.getLatitudeI() + ", lon: " + position.getLongitudeI() + ", alt: " + position.getAltitude() + ", from: " + payload.getFrom());

                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(null, payload.getFrom(), position.getBatteryLevel(), position.getLatitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION, position.getLongitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION, position.getAltitude(), true, null, 0);

                mTrackerListener.onTrackerUpdated(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "    POSITION_APP message failed to parse");
                e.printStackTrace();
            }
        }
    }

//    @Nullable
//    private TrackerUserInfo trackerUserInfoFromNodeInfo(MeshProtos.NodeInfo nodeInfo) {
//        MeshProtos.User meshUser = nodeInfo.getUser();
//
//        if (meshUser == null) {
//            return null;
//        }
//
//        double lat = 0.0;
//        double lon = 0.0;
//        int altitude = 0;
//        boolean gpsValid = false;
//        long timeSinceLastSeen = 0;
//        MeshProtos.Position position = nodeInfo.getPosition();
//        if (position != null) {
//            lat = position.getLatitudeI();
//            lon = position.getLongitudeI();
//            altitude = position.getAltitude();
//            timeSinceLastSeen = position.getTime();
//            gpsValid = true;
//        }
//        return new TrackerUserInfo(meshUser.getLongName(), meshUser.getId(), 0, lat, lon, altitude, gpsValid, meshUser.getShortName(), timeSinceLastSeen);
//    }
//
//    @Nullable
//    private TrackerUserInfo trackerUserInfoFromNodeInfo(NodeInfo nodeInfo, long timeSinceLastSeen) {
//        MeshUser meshUser = nodeInfo.getUser();
//
//        if (meshUser == null) {
//            return null;
//        }
//
//        double lat = 0.0;
//        double lon = 0.0;
//        int altitude = 0;
//        boolean gpsValid = false;
//        Position position = nodeInfo.getPosition();
//        if (position != null) {
//            lat = position.getLatitude();
//            lon = position.getLongitude();
//            altitude = position.getAltitude();
//            gpsValid = true;
//        }
//        return new TrackerUserInfo(meshUser.getLongName(), meshUser.getId(), 0, lat, lon, altitude, gpsValid, meshUser.getShortName(), timeSinceLastSeen);
//    }
}
