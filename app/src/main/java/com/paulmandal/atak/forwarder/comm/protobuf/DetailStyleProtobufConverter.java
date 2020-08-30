package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufColor;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDetailStyle;

public class DetailStyleProtobufConverter {
    private static final String KEY_COLOR = "color";

    private static final String KEY_ARGB = "argb";
    private static final String KEY_VALUE = "value";

    public void toColor(CotDetail cotDetail, ProtobufDetailStyle.MinimalDetailStyle.Builder detailStyleBuilder) throws UnknownDetailFieldException {
        ProtobufColor.MinimalColor.Builder builder = ProtobufColor.MinimalColor.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_ARGB:
                    builder.setArgb(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_VALUE:
                    builder.setValue(Integer.parseInt(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: color." + attribute.getName());
            }
        }

        detailStyleBuilder.setColor(builder.build());
    }

    public void maybeAddColor(CotDetail cotDetail, ProtobufColor.MinimalColor color) {
        if (color != null && color != ProtobufColor.MinimalColor.getDefaultInstance()) {
            CotDetail colorDetail = new CotDetail(KEY_COLOR);

            if (color.getArgb() != 0) {
                colorDetail.setAttribute(KEY_ARGB, Integer.toString(color.getArgb()));
            }
            if (color.getValue() != 0) {
                colorDetail.setAttribute(KEY_VALUE, Integer.toString(color.getValue()));
            }

            cotDetail.addChild(colorDetail);
        }
    }
}
