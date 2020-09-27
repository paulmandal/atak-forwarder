package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufPolyStyle;

import java.util.List;

public class PolyStyleProtobufConverter {
    private static final String KEY_POLY_STYLE = "PolyStyle";

    private static final String KEY_COLOR = "color";

    public ProtobufPolyStyle.PolyStyle toPolyStyle(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufPolyStyle.PolyStyle.Builder builder = ProtobufPolyStyle.PolyStyle.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: shape.Style.PolyStyle." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_COLOR:
                    builder.setColor(Long.parseLong(child.getInnerText(), 16));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: shape.Style.PolyStyle." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddPolyStyle(CotDetail cotDetail, ProtobufPolyStyle.PolyStyle polyStyle) {
        if (polyStyle == null || polyStyle == ProtobufPolyStyle.PolyStyle.getDefaultInstance()) {
            return;
        }

        CotDetail polyStyleDetail = new CotDetail(KEY_POLY_STYLE);

        CotDetail colorDetail = new CotDetail(KEY_COLOR);
        colorDetail.setInnerText(String.format("%08x", polyStyle.getColor()));
        polyStyleDetail.addChild(colorDetail);

        cotDetail.addChild(polyStyleDetail);
    }
}
