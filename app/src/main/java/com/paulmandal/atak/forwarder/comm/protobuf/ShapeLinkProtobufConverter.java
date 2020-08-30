package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDrawnShape;
import com.paulmandal.atak.forwarder.protobufs.ProtobufShape;
import com.paulmandal.atak.forwarder.protobufs.ProtobufShapeLink;

public class ShapeLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_POINT = "point";

    private static final double NULL_VALUE = -1.0;

    public void toShapeLink(CotDetail cotDetail, ProtobufDrawnShape.MinimalDrawnShape.Builder shapeBuilder) throws UnknownDetailFieldException {
        ProtobufShapeLink.MinimalShapeLink.Builder builder = ProtobufShapeLink.MinimalShapeLink.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();

        builder.setLat(NULL_VALUE);
        builder.setLon(NULL_VALUE);
        builder.setHae(NULL_VALUE);

        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_POINT:
                    String[] splitPoint = attribute.getValue().split(",");
                    if (splitPoint.length > 0) {
                        builder.setLat(Double.parseDouble(splitPoint[0]));
                    }
                    if (splitPoint.length > 1) {
                        builder.setLon(Double.parseDouble(splitPoint[1]));
                    }
                    if (splitPoint.length > 2) {
                        builder.setHae(Double.parseDouble(splitPoint[2]));
                    }
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: link[shape]." + attribute.getName());
            }
        }

        shapeBuilder.addLink(builder);
    }

    public void maybeAddShape(CotDetail cotDetail, ProtobufShape.MinimalShape shape) {
        if (shape != null && shape != ProtobufShape.MinimalShape.getDefaultInstance()
                && shape.getLinkList() != ProtobufShape.MinimalShape.getDefaultInstance().getLinkList()) {
            for (ProtobufShapeLink.MinimalShapeLink link : shape.getLinkList()) {
                CotDetail linkDetail = new CotDetail(KEY_LINK);

                String point = "";
                if (link.getLat() != NULL_VALUE) {
                    point += Double.toString(link.getLat());
                }
                if (link.getLon() != NULL_VALUE) {
                    point += ((point.length() > 0 ? "," : "") + link.getLon());
                }
                if (link.getHae() != NULL_VALUE) {
                    point += ((point.length() > 0 ? "," : "") + link.getHae());
                }
                linkDetail.setAttribute(KEY_POINT, point);

                cotDetail.addChild(linkDetail);
            }
        }
    }
}
