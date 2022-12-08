package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

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
import java.util.concurrent.CopyOnWriteArraySet;

public class TrackerEventHandler extends MeshEventHandler {
    public interface TrackerListener {
        void onTrackerUpdated(TrackerUserInfo trackerUserInfo);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + TrackerEventHandler.class.getSimpleName();

    private static final double LAT_LON_INT_TO_DOUBLE_CONVERSION = 10000000D;

    private final CopyOnWriteArraySet<TrackerListener> mTrackerListeners = new CopyOnWriteArraySet<>();

    private final Handler mUiThreadHandler;

    public TrackerEventHandler(Context atakContext,
                               Logger logger,
                               List<Destroyable> destroyables,
                               Handler uiThreadHandler,
                               ConnectionStateHandler connectionStateHandler) {
        super(atakContext,
                logger,
                new String[] {
                        MeshServiceConstants.ACTION_RECEIVED_POSITION_APP,
                        MeshServiceConstants.ACTION_RECEIVED_NODEINFO_APP
                },
                destroyables,
                connectionStateHandler);

        mUiThreadHandler = uiThreadHandler;
    }

    public void addListener(TrackerListener listener) {
        mTrackerListeners.add(listener);
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        DataPacket payload = intent.getParcelableExtra(MeshServiceConstants.EXTRA_PAYLOAD);
        int dataType = payload.getDataType();

        if (dataType == Portnums.PortNum.NODEINFO_APP_VALUE) {
            try {
                MeshProtos.User meshUser = MeshProtos.User.parseFrom(payload.getBytes());
                mLogger.i(TAG, "NODEINFO_APP parsed NodeInfo: " + meshUser.getId() + ", longName: " + meshUser.getLongName() + ", shortName: " + meshUser.getShortName());

                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(meshUser.getLongName(), meshUser.getId(), null, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, false, meshUser.getShortName(), System.currentTimeMillis());

                notifyListeners(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "NODEINFO_APP message failed to parse");
                e.printStackTrace();
            }
        } else if (dataType == Portnums.PortNum.POSITION_APP_VALUE) {
            try {
                MeshProtos.Position position = MeshProtos.Position.parseFrom(payload.getBytes());
                mLogger.i(TAG, "POSITION_APP parsed position: lat: " + position.getLatitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION + ", lon: " + position.getLongitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION + ", alt: " + position.getAltitudeHae() + ", from: " + payload.getFrom());

                boolean gpsValid = position.getLatitudeI() != 0 || position.getLongitudeI() != 0 || position.getAltitudeHae() != 0;
                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(UserInfo.CALLSIGN_UNKNOWN, payload.getFrom(), null, position.getLatitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION, position.getLongitudeI() / LAT_LON_INT_TO_DOUBLE_CONVERSION, position.getAltitudeHae(), gpsValid, null, System.currentTimeMillis());

                notifyListeners(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "POSITION_APP message failed to parse");
                e.printStackTrace();
            }
        } else if (dataType == Portnums.PortNum.TELEMETRY_APP_VALUE) {
            try {
                TelemetryProtos.Telemetry telemetry = TelemetryProtos.Telemetry.parseFrom(payload.getBytes());
                mLogger.i(TAG, "TELEMETRY_APP parsed Telemetry: batteryLevel: " + telemetry.getDeviceMetrics().getBatteryLevel());

                TrackerUserInfo trackerUserInfo = new TrackerUserInfo(UserInfo.CALLSIGN_UNKNOWN, payload.getFrom(), telemetry.getDeviceMetrics().getBatteryLevel(), TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, TrackerUserInfo.NO_LAT_LON_ALT_VALUE, false, null, System.currentTimeMillis());

                notifyListeners(trackerUserInfo);
            } catch (InvalidProtocolBufferException e) {
                mLogger.e(TAG, "TELEMETRY_APP message failed to parse");
                e.printStackTrace();
            }
        }
    }

    private void notifyListeners(TrackerUserInfo trackerUserInfo) {
        for (TrackerListener listener : mTrackerListeners) {
            mUiThreadHandler.post(() -> listener.onTrackerUpdated(trackerUserInfo));
        }
    }
}
