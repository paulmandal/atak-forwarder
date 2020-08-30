package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;

public class LabelsOnProtobufConverter {
    private static final String KEY_LABELS_ON = "labels_on";

    private static final String KEY_VALUE = "value";

    public void maybeGetLabelsOnValue(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail labelsOnDetail = cotDetail.getFirstChildByName(0, KEY_LABELS_ON);
        if (labelsOnDetail != null) {
            String valueStr = labelsOnDetail.getAttribute(KEY_VALUE);
            if (valueStr != null) {
                customBytesExtFields.labelsOn = Boolean.parseBoolean(valueStr);
            }
        }
    }

    public void toLabelsOn(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_VALUE:
                    // Do nothing, we are packing this into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: labels_on." + attribute.getName());
            }
        }
    }

    public void maybeAddLabelsOn(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        if (customBytesExtFields.labelsOn == null) {
            return;
        }

        CotDetail labelsOnDetail = new CotDetail(KEY_LABELS_ON);

        labelsOnDetail.setAttribute(KEY_VALUE, Boolean.toString(customBytesExtFields.labelsOn));

        cotDetail.addChild(labelsOnDetail);
    }
}
