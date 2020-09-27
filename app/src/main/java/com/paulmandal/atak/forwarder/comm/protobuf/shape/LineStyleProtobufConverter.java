package com.paulmandal.atak.forwarder.comm.protobuf.shape;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufLineStyle;

import java.util.List;

public class LineStyleProtobufConverter {
    private static final String KEY_LINE_STYLE = "LineStyle";

    private static final String KEY_COLOR = "color";
    private static final String KEY_WIDTH = "width";

    public ProtobufLineStyle.LineStyle toLineStyle(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufLineStyle.LineStyle.Builder builder = ProtobufLineStyle.LineStyle.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: shape.Style.LineStyle." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_COLOR:
                    builder.setColor(Long.parseLong(child.getInnerText(), 16));
                    break;
                case KEY_WIDTH:
                    builder.setWidth((int)(Double.parseDouble(child.getInnerText()) * 100));
                    break;
                    default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: shape.Style.LineStyle." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddLineStyle(CotDetail cotDetail, ProtobufLineStyle.LineStyle lineStyle) {
        if (lineStyle == null || lineStyle == ProtobufLineStyle.LineStyle.getDefaultInstance()) {
            return;
        }

        CotDetail lineStyleDetail = new CotDetail(KEY_LINE_STYLE);

        CotDetail colorDetail = new CotDetail(KEY_COLOR);
        colorDetail.setInnerText(String.format("%08x", lineStyle.getColor()));
        lineStyleDetail.addChild(colorDetail);

        CotDetail widthDetail = new CotDetail(KEY_WIDTH);
        widthDetail.setInnerText(Double.toString(lineStyle.getWidth() / 100D));
        lineStyleDetail.addChild(widthDetail);

        cotDetail.addChild(lineStyleDetail);
    }

}
