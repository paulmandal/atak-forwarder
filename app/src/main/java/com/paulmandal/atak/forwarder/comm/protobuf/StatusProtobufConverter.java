package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;

public class StatusProtobufConverter {
    private static final String KEY_STATUS = "status";

    private static final String KEY_BATTERY = "battery";
    private static final String KEY_READINESS = "readiness";

    public void maybeGetStatusValues(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail status = cotDetail.getFirstChildByName(0, KEY_STATUS);
        if (status != null) {
            String batteryStr = status.getAttribute(KEY_BATTERY);
            if (batteryStr != null) {
                customBytesExtFields.battery = Integer.parseInt(batteryStr);
            }

            String readinessStr = status.getAttribute(KEY_READINESS);
            if (readinessStr != null) {
                customBytesExtFields.readiness = Boolean.parseBoolean(readinessStr);
            }
        }
    }

    public void toStatus(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_READINESS:
                case KEY_BATTERY:
                    // Do nothing, we pack these fields into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: status." + attribute.getName());
            }
        }
    }

    public void maybeAddStatus(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        if (customBytesExtFields.battery == null && customBytesExtFields.readiness == null) {
            return;
        }

        CotDetail statusDetail = new CotDetail(KEY_STATUS);

        if (customBytesExtFields.battery != null) {
            statusDetail.setAttribute(KEY_BATTERY, Integer.toString(customBytesExtFields.battery));
        }
        if (customBytesExtFields.readiness != null) {
            statusDetail.setAttribute(KEY_READINESS, Boolean.toString(customBytesExtFields.readiness));
        }

        cotDetail.addChild(statusDetail);
    }
}
