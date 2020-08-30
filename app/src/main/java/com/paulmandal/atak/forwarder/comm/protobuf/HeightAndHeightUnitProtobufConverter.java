package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufHeight;

public class HeightAndHeightUnitProtobufConverter {
    private static final String KEY_HEIGHT_UNIT = "height_unit";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_UNIT = "unit";
    private static final String KEY_VALUE = "value";

    private static final String[] MAPPING_HEIGHT_UNIT = {
            "kilometers",
            "meters",
            "miles",
            "yards",
            "feet",
            "nautical miles"
    };

    private static final double NULL_MARKER = -1.0;

    public void maybeGetHeightUnitValues(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail heightUnitDetail = cotDetail.getFirstChildByName(0, KEY_HEIGHT_UNIT);
        if (heightUnitDetail != null) {
            String heightUnitStr = heightUnitDetail.getInnerText();
            if (heightUnitStr != null) {
                customBytesExtFields.heightUnit = Integer.parseInt(heightUnitStr);
            }
        }
    }

    public void maybeGetHeightValues(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) throws MappingNotFoundException {
        CotDetail heightDetail = cotDetail.getFirstChildByName(0, KEY_HEIGHT);
        if (heightDetail != null) {
            String heightUnitStr = heightDetail.getAttribute(KEY_UNIT);
            if (heightUnitStr != null) {
                customBytesExtFields.heightUnit = BitUtils.findMappingForArray("height.unit", MAPPING_HEIGHT_UNIT, heightUnitStr);
            }
        }
    }

    public void toHeightUnit(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: height_unit." + attribute.getName());
            }
        }
    }

    public ProtobufHeight.Height toHeight(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufHeight.Height.Builder builder = ProtobufHeight.Height.newBuilder();
        builder.setHeightValue(NULL_MARKER);

        if (cotDetail.getInnerText() != null) {
            builder.setHeightValue(Double.parseDouble(cotDetail.getInnerText()));
        }

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UNIT:
                    // Do nothing, we are packing this into bits
                    break;
                case KEY_VALUE:
                    builder.setHeightValue(Double.parseDouble(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: height." + attribute.getName());
            }
        }

        return builder.build();
    }

    public void maybeAddHeightAndHeightUnit(CotDetail cotDetail, ProtobufHeight.Height height, CustomBytesExtFields customBytesExtFields) {
        if (customBytesExtFields.heightUnit == null && (height == null || height == ProtobufHeight.Height.getDefaultInstance())) {
            return;
        }

        CotDetail heightDetail = new CotDetail(KEY_HEIGHT);

        if (customBytesExtFields.heightUnit != null) {
            CotDetail heightUnitDetail = new CotDetail(KEY_HEIGHT_UNIT);

            heightUnitDetail.setInnerText(Integer.toString(customBytesExtFields.heightUnit));

            cotDetail.addChild(heightUnitDetail);

            heightDetail.setAttribute(KEY_UNIT, MAPPING_HEIGHT_UNIT[customBytesExtFields.heightUnit]);
        }

        if (height.getHeightValue() != NULL_MARKER) {
            String heightValueStr = Double.toString(height.getHeightValue());
            heightDetail.setInnerText(heightValueStr);
            heightDetail.setAttribute(KEY_VALUE, heightValueStr);
        }

        cotDetail.addChild(heightDetail);
    }
}
