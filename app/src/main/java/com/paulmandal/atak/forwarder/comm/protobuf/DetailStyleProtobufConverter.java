package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufColor;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDetailStyle;

public class DetailStyleProtobufConverter {
    private static final String KEY_COLOR = "color";
    private static final String KEY_STROKE_COLOR = "strokeColor";
    private static final String KEY_STROKE_WEIGHT = "strokeWeight";
    private static final String KEY_FILL_COLOR = "fillColor";

    private static final String KEY_ARGB = "argb";
    private static final String KEY_VALUE = "value";

    public void toColor(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle.Builder detailStyleBuilder) throws UnknownDetailFieldException {
        ProtobufColor.Color.Builder builder = ProtobufColor.Color.newBuilder();
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

    public void maybeAddColor(CotDetail cotDetail, ProtobufColor.Color color) {
        if (color == null || color == ProtobufColor.Color.getDefaultInstance()) {
            return;
        }

        CotDetail colorDetail = new CotDetail(KEY_COLOR);

        if (color.getArgb() != 0) {
            colorDetail.setAttribute(KEY_ARGB, Integer.toString(color.getArgb()));
        }
        if (color.getValue() != 0) {
            colorDetail.setAttribute(KEY_VALUE, Integer.toString(color.getValue()));
        }

        cotDetail.addChild(colorDetail);
    }

    public void toStrokeColor(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle.Builder detailStyleBuilder) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_VALUE:
                    detailStyleBuilder.setStrokeColor(Integer.parseInt(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: strokeColor." + attribute.getName());
            }
        }
    }

    public void maybeAddStrokeColor(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle detailStyle) {
        if (detailStyle.getStrokeColor() == 0) {
            return;
        }

        CotDetail strokeColorDetail = new CotDetail(KEY_STROKE_COLOR);

        strokeColorDetail.setAttribute(KEY_VALUE, Integer.toString(detailStyle.getStrokeColor()));

        cotDetail.addChild(strokeColorDetail);
    }

    public void toStrokeWeight(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle.Builder detailStyleBuilder) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_VALUE:
                    detailStyleBuilder.setStrokeWeight((int)(Double.parseDouble(attribute.getValue()) * 100));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: strokeWeight." + attribute.getName());
            }
        }
    }

    public void maybeAddStrokeWeight(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle detailStyle) {
        if (detailStyle.getStrokeWeight() == 0) {
            return;
        }

        CotDetail strokeWeightDetail = new CotDetail(KEY_STROKE_WEIGHT);

        strokeWeightDetail.setAttribute(KEY_VALUE, Double.toString(detailStyle.getStrokeWeight() / 100D));

        cotDetail.addChild(strokeWeightDetail);
    }

    public void toFillColor(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle.Builder detailStyleBuilder) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_VALUE:
                    detailStyleBuilder.setFillColor(Integer.parseInt(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: strokeWeight." + attribute.getName());
            }
        }
    }

    public void maybeAddFillColor(CotDetail cotDetail, ProtobufDetailStyle.DetailStyle detailStyle) {
        if (detailStyle.getFillColor() == 0) {
            return;
        }

        CotDetail fillColorDetail = new CotDetail(KEY_FILL_COLOR);

        fillColorDetail.setAttribute(KEY_VALUE, Integer.toString(detailStyle.getFillColor()));

        cotDetail.addChild(fillColorDetail);
    }
}
