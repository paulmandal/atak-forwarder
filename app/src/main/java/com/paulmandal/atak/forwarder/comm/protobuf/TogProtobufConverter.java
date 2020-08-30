package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;

public class TogProtobufConverter {
    private static final String KEY_TOG = "tog";

    private static final String KEY_ENABLED = "enabled";

    public void maybeGetTogValue(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail togDetail = cotDetail.getFirstChildByName(0, KEY_TOG);
        if (togDetail != null) {
            String valueStr = togDetail.getAttribute(KEY_ENABLED);
            if (valueStr != null) {
                customBytesExtFields.tog = valueStr.equals("1");
            }
        }
    }

    public void toTog(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_ENABLED:
                    // Do nothing, we are packing this into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: tog." + attribute.getName());
            }
        }
    }

    public void maybeAddTog(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        if (customBytesExtFields.tog == null) {
            return;
        }

        CotDetail togDetail = new CotDetail(KEY_TOG);

        togDetail.setAttribute(KEY_ENABLED, customBytesExtFields.tog ? "1" : "0");

        cotDetail.addChild(togDetail);
    }
}
