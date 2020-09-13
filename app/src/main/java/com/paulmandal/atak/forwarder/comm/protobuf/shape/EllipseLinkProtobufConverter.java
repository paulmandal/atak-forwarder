package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufEllipseLink;

import java.util.List;

public class EllipseLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_UID = "uid";
    private static final String KEY_TYPE = "type";
    private static final String KEY_RELATION = "relation";

    private static final String KEY_STYLE = "Style";

    private final StyleProtobufConverter mStyleProtobufConverter;

    public EllipseLinkProtobufConverter(StyleProtobufConverter styleProtobufConverter) {
        mStyleProtobufConverter = styleProtobufConverter;
    }

    public ProtobufEllipseLink.EllipseLink toEllipseLink(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufEllipseLink.EllipseLink.Builder builder = ProtobufEllipseLink.EllipseLink.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UID:
                    builder.setUid(attribute.getValue());
                    break;
                case KEY_TYPE:
                    builder.setType(attribute.getValue());
                    break;
                case KEY_RELATION:
                    builder.setRelation(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: shape.link." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_STYLE:
                    builder.setStyle(mStyleProtobufConverter.toStyle(child));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: shape.link." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddEllipseLink(CotDetail cotDetail, ProtobufEllipseLink.EllipseLink ellipseLink) {
        if (ellipseLink == null || ellipseLink == ProtobufEllipseLink.EllipseLink.getDefaultInstance()) {
            return;
        }

        CotDetail linkDetail = new CotDetail(KEY_LINK);

        linkDetail.setAttribute(KEY_UID, ellipseLink.getUid());
        linkDetail.setAttribute(KEY_TYPE, ellipseLink.getType());
        linkDetail.setAttribute(KEY_RELATION, ellipseLink.getRelation());
        mStyleProtobufConverter.maybeAddStyle(linkDetail, ellipseLink.getStyle());

        cotDetail.addChild(linkDetail);
    }
}
