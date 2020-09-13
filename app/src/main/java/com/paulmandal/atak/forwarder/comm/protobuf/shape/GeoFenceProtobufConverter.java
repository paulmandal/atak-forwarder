package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import android.util.Log;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufGeoFence;

import java.util.List;

public class GeoFenceProtobufConverter {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + GeoFenceProtobufConverter.class.getSimpleName();

    private static final String KEY_GEOFENCE = "__geofence";

    private static final String KEY_ELEVATION_MONITORED = "elevationMonitored";
    private static final String KEY_MIN_ELEVATION = "minElevation";
    private static final String KEY_MONITOR = "monitor";
    private static final String KEY_TRIGGER = "trigger";
    private static final String KEY_TRACKING = "tracking";
    private static final String KEY_MAX_ELEVATION = "maxElevation";
    private static final String KEY_BOUNDING_SPHERE = "boundingSphere";

    public ProtobufGeoFence.GeoFence toGeoFence(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufGeoFence.GeoFence.Builder builder = ProtobufGeoFence.GeoFence.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_ELEVATION_MONITORED:
                    builder.setElevationMonitored(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_MIN_ELEVATION:
                    builder.setMinElevation(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_MONITOR:
                    builder.setMonitor(ProtobufGeoFence.GeoFence.Monitor.valueOf(attribute.getValue().toUpperCase()));
                    break;
                case KEY_TRIGGER:
                    builder.setTrigger(ProtobufGeoFence.GeoFence.Trigger.valueOf(attribute.getValue().toUpperCase()));
                    break;
                case KEY_TRACKING:
                    builder.setTracking(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_MAX_ELEVATION:
                    builder.setMaxElevation(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_BOUNDING_SPHERE:
                    builder.setBoundingSphere(Double.parseDouble(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: __geofence." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: __geofence." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddGeoFence(CotDetail cotDetail, ProtobufGeoFence.GeoFence geoFence) {
        if (geoFence == null || geoFence == ProtobufGeoFence.GeoFence.getDefaultInstance()) {
            return;
        }

        CotDetail geoFenceDetail = new CotDetail(KEY_GEOFENCE);

        geoFenceDetail.setAttribute(KEY_ELEVATION_MONITORED, Boolean.toString(geoFence.getElevationMonitored()));
        geoFenceDetail.setAttribute(KEY_MIN_ELEVATION, Double.toString(geoFence.getMinElevation()));
        geoFenceDetail.setAttribute(KEY_MONITOR, xmlNameFromMonitor(geoFence.getMonitor()));
        geoFenceDetail.setAttribute(KEY_TRIGGER, xmlNameFromTrigger(geoFence.getTrigger()));
        geoFenceDetail.setAttribute(KEY_TRACKING, Boolean.toString(geoFence.getTracking()));
        geoFenceDetail.setAttribute(KEY_MAX_ELEVATION, Double.toString(geoFence.getMaxElevation()));
        geoFenceDetail.setAttribute(KEY_BOUNDING_SPHERE, Double.toString(geoFence.getBoundingSphere()));

        cotDetail.addChild(geoFenceDetail);
    }

    private String xmlNameFromMonitor(ProtobufGeoFence.GeoFence.Monitor monitor) {
        switch (monitor) {
            case TAKUSERS:
                return "TAKUsers";
            case FRIENDLY:
                return "Friendly";
            case HOSTILE:
                return "Hostile";
            case CUSTOM:
                return "Custom";
            case ALL:
                return "All";
            default:
                Log.e(TAG, "Unknown monitor type: " + monitor);
        }
        return "";
    }

    private String xmlNameFromTrigger(ProtobufGeoFence.GeoFence.Trigger trigger) {
        switch (trigger) {
            case ENTRY:
                return "Entry";
            case EXIT:
                return "Exit";
            case BOTH:
                return "Both";
            default:
                Log.e(TAG, "Unknown trigger type: " + trigger);
        }
        return "";
    }


}
