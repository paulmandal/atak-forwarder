package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDrawnShape;
import com.paulmandal.atak.forwarder.protobufs.ProtobufShapeLink;

public class ShapeLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_POINT = "point";

    private static final double NULL_VALUE = -1.0;

    public void toShapeLink(CotDetail cotDetail, ProtobufDrawnShape.DrawnShape.Builder shapeBuilder) throws UnknownDetailFieldException {
        ProtobufShapeLink.ShapeLink.Builder builder = ProtobufShapeLink.ShapeLink.newBuilder();
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

    public void maybeAddDrawnShape(CotDetail cotDetail, ProtobufDrawnShape.DrawnShape drawnShape) {
        if (drawnShape == null || drawnShape == ProtobufDrawnShape.DrawnShape.getDefaultInstance()
                || drawnShape.getLinkList() == ProtobufDrawnShape.DrawnShape.getDefaultInstance().getLinkList()) {
            return;
        }

        for (ProtobufShapeLink.ShapeLink link : drawnShape.getLinkList()) {
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
