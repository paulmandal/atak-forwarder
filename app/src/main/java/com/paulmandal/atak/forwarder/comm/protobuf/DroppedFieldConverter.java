package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;

public class DroppedFieldConverter {
    private static final String KEY_DROID = "Droid";

    public void toArchive(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: archive." + attribute.getName());
            }
        }
    }

    public void toUid(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_DROID:
                    // Do nothing, we drop this field
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: uid." + attribute.getName());
            }
        }
    }
}
