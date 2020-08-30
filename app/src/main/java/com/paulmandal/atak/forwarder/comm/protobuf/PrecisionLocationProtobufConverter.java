package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;

public class PrecisionLocationProtobufConverter {
    private static final String KEY_PRECISIONLOCATION = "precisionlocation";

    private static final String KEY_GEOPOINTSRC = "geopointsrc";
    private static final String KEY_ALTSRC = "altsrc";

    public void maybeGetPrecisionLocationValues(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail precisionLocation = cotDetail.getFirstChildByName(0, KEY_PRECISIONLOCATION);
        if (precisionLocation != null) {
            customBytesExtFields.geoPointSrc = precisionLocation.getAttribute(KEY_GEOPOINTSRC);
            customBytesExtFields.altSrc = precisionLocation.getAttribute(KEY_ALTSRC);
        }
    }

    public void toPrecisionLocation(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_ALTSRC:
                case KEY_GEOPOINTSRC:
                    // Do nothing, we pack these fields into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: precisionLocation." + attribute.getName());
            }
        }
    }

    public void maybeAddPrecisionLocation(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        if (StringUtils.isNullOrEmpty(customBytesExtFields.altSrc) && StringUtils.isNullOrEmpty(customBytesExtFields.geoPointSrc)) {
            return;
        }

        CotDetail precisionLocationDetail = new CotDetail(KEY_PRECISIONLOCATION);

        if (!StringUtils.isNullOrEmpty(customBytesExtFields.altSrc)) {
            precisionLocationDetail.setAttribute(KEY_ALTSRC, customBytesExtFields.altSrc);
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.geoPointSrc)) {
            precisionLocationDetail.setAttribute(KEY_GEOPOINTSRC, customBytesExtFields.geoPointSrc);
        }

        cotDetail.addChild(precisionLocationDetail);
    }
}
