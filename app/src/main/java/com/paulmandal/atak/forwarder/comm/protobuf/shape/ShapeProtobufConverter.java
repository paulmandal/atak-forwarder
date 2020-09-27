package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufShape;

import java.util.List;

public class ShapeProtobufConverter {
    private static final String KEY_SHAPE = "shape";

    private static final String KEY_ELLIPSE = "ellipse";
    private static final String KEY_LINK = "link";

    private final EllipseProtobufConverter mEllipseProtobufConverter;
    private final EllipseLinkProtobufConverter mEllipseLinkProtobufConverter;

    public ShapeProtobufConverter(EllipseProtobufConverter ellipseProtobufConverter, EllipseLinkProtobufConverter ellipseLinkProtobufConverter) {
        mEllipseProtobufConverter = ellipseProtobufConverter;
        mEllipseLinkProtobufConverter = ellipseLinkProtobufConverter;
    }

    public ProtobufShape.Shape toShape(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufShape.Shape.Builder builder = ProtobufShape.Shape.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: shape." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_ELLIPSE:
                    builder.setEllipse(mEllipseProtobufConverter.toEllipse(child));
                    break;
                case KEY_LINK:
                    builder.setLink(mEllipseLinkProtobufConverter.toEllipseLink(child));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: shape." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddShape(CotDetail cotDetail, ProtobufShape.Shape shape) {
        if (shape == null || shape == ProtobufShape.Shape.getDefaultInstance()) {
            return;
        }

        CotDetail shapeDetail = new CotDetail(KEY_SHAPE);

        mEllipseProtobufConverter.maybeAddEllipse(shapeDetail, shape.getEllipse());
        mEllipseLinkProtobufConverter.maybeAddEllipseLink(shapeDetail, shape.getLink());

        cotDetail.addChild(shapeDetail);
    }
}
