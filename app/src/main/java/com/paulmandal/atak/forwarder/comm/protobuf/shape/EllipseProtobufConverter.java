package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufEllipse;

import java.util.List;

public class EllipseProtobufConverter {
    private static final String KEY_ELLIPSE = "ellipse";

    private static final String KEY_MAJOR = "major";
    private static final String KEY_MINOR = "minor";
    private static final String KEY_ANGLE = "angle";

    public ProtobufEllipse.Ellipse toEllipse(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufEllipse.Ellipse.Builder builder = ProtobufEllipse.Ellipse.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_MAJOR:
                    builder.setMajor(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_MINOR:
                    builder.setMinor(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_ANGLE:
                    builder.setAngle(Integer.parseInt(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: ellipse." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: ellipse." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddEllipse(CotDetail cotDetail, ProtobufEllipse.Ellipse ellipse) {
        if (ellipse == null || ellipse == ProtobufEllipse.Ellipse.getDefaultInstance()) {
            return;
        }

        CotDetail ellipseDetail = new CotDetail(KEY_ELLIPSE);

        ellipseDetail.setAttribute(KEY_MAJOR, Double.toString(ellipse.getMajor()));
        ellipseDetail.setAttribute(KEY_MINOR, Double.toString(ellipse.getMinor()));
        ellipseDetail.setAttribute(KEY_ANGLE, Integer.toString(ellipse.getAngle()));

        cotDetail.addChild(ellipseDetail);
    }
}
