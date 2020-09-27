package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufStyle;

import java.util.List;

public class StyleProtobufConverter {
    private static final String KEY_STYLE = "Style";

    private static final String KEY_LINE_STYLE = "LineStyle";
    private static final String KEY_POLY_STYLE = "PolyStyle";

    private final LineStyleProtobufConverter mLineStyleProtobufConverter;
    private final PolyStyleProtobufConverter mPolyStyleProtobufConverter;

    public StyleProtobufConverter(LineStyleProtobufConverter lineStyleProtobufConverter, PolyStyleProtobufConverter polyStyleProtobufConverter) {
        mLineStyleProtobufConverter = lineStyleProtobufConverter;
        mPolyStyleProtobufConverter = polyStyleProtobufConverter;
    }

    public ProtobufStyle.Style toStyle(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufStyle.Style.Builder builder = ProtobufStyle.Style.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: shape.Style." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_LINE_STYLE:
                    builder.setLineStyle(mLineStyleProtobufConverter.toLineStyle(child));
                    break;
                case KEY_POLY_STYLE:
                    builder.setPolyStyle(mPolyStyleProtobufConverter.toPolyStyle(child));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: shape.Style." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddStyle(CotDetail cotDetail, ProtobufStyle.Style style) {
        if (style == null || style == ProtobufStyle.Style.getDefaultInstance()) {
            return;
        }

        CotDetail styleDetail = new CotDetail(KEY_STYLE);

        mLineStyleProtobufConverter.maybeAddLineStyle(styleDetail, style.getLineStyle());
        mPolyStyleProtobufConverter.maybeAddPolyStyle(styleDetail, style.getPolyStyle());

        cotDetail.addChild(styleDetail);
    }
}
