package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;

import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.Portnums;
import com.geeksville.mesh.TelemetryProtos;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class TrackerEventHandler extends MeshEventHandler {
    public interface TrackerListener {
        void onTrackerUpdated(TrackerUserInfo trackerUserInfo);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + TrackerEventHandler.class.getSimpleName();

    private static final double LAT_LON_INT_TO_DOUBLE_CONVERSION = 10000000D;

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
                        MeshServiceConstants.ACTION_RECEIVED_TELEMETRY_APP
                },
                destroyables,
                meshSuspendController);
    }

    public void setListener(TrackerListener listener) {
        mTrackerListener = listener;
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        DataPacket payload = intent.getParcelableExtra(MeshServiceConstants.EXTRA_PAYLOAD);
        int dataType = payload.getDataType();

        if (dataType == Portnums.PortNum.NODEINFO_APP.getNumber()) {
            try {
                MeshProtos.User meshUser = MeshProtos.User.parseFrom(payload.getBytes());
                mLogger.d(TAG, "NODEINFO_APP parsed NodeInfo: " + meshUser.getId() + ", longName: " + meshUser.getLongName() + ", shortName: " + meshUser.getShortName());

                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(meshUser.getLongName(), meshUser.getId(), null, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, false, meshUser.getShortName(), System.currentTimeMillis());

                mTrackerListener.onTrackerUpdated(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "NODEINFO_APP message failed to parse");
                e.printStackTrace();
            }
        } else if (dataType == Portnums.PortNum.POSITION_APP.getNumber()) {
            try {
                MeshProtos.Position position = MeshProtos.Position.parseFrom(payload.getBytes());
                mLogger.d(TAG, "POSITION_APP parsed position: lat: " + position.getLatitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION + ", lon: " + position.getLongitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION + ", alt: " + position.getAltitude() + ", batteryLevel: " + position.getBatteryLevel() + ", from: " + payload.getFrom());

                boolean gpsValid = position.getLatitudeI() != 0 || position.getLongitudeI() != 0 || position.getAltitude() != 0;
                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(UserInfo.CALLSIGN_UNKNOWN, payload.getFrom(), null, position.getLatitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION, position.getLongitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION, position.getAltitude(), gpsValid, null, System.currentTimeMillis());

                mTrackerListener.onTrackerUpdated(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "POSITION_APP message failed to parse");
                e.printStackTrace();
            }
        } else if (dataType == Portnums.PortNum.TELEMETRY_APP.getNumber()) {
            try {
                TelemetryProtos.Telemetry telemetry = TelemetryProtos.Telemetry.parseFrom(payload.getBytes());
                mLogger.v(TAG, "TELEMETRY_APP parsed telemetry: batteryLevel: " + telemetry.getDeviceMetrics().getBatteryLevel());

                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(UserInfo.CALLSIGN_UNKNOWN, payload.getFrom(), telemetry.getDeviceMetrics().getBatteryLevel(), TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, false, null, System.currentTimeMillis());

                mTrackerListener.onTrackerUpdated(trackerUserInfo);

            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "TELEMETRY_APP message failed to parse");
                e.printStackTrace();
            }
        }
    }
}
