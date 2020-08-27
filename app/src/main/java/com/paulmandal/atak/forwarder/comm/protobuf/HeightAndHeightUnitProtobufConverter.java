package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;

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

            String heightValueInnerText = heightDetail.getInnerText();
            if (heightValueInnerText != null) {
                double heightValueDouble = Double.parseDouble(heightValueInnerText);
                customBytesExtFields.heightValue = (int)heightValueDouble;
            }

            String heightValueStr = heightDetail.getAttribute(KEY_VALUE);
            if (heightValueStr != null) {
                double heightValueDouble = Double.parseDouble(heightValueStr);
                customBytesExtFields.heightValue = (int)heightValueDouble;
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

    public void toHeight(CotDetail cotDetail) throws UnknownDetailFieldException {
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UNIT:
                case KEY_VALUE:
                    // Do nothing, we are packing these into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: height." + attribute.getName());
            }
        }
    }

    public void maybeAddHeightAndHeightUnit(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        if (customBytesExtFields.heightUnit != null || customBytesExtFields.heightValue != null) {
            CotDetail heightDetail = new CotDetail(KEY_HEIGHT);

            if (customBytesExtFields.heightUnit != null) {
                CotDetail heightUnitDetail = new CotDetail(KEY_HEIGHT_UNIT);

                heightUnitDetail.setInnerText(Integer.toString(customBytesExtFields.heightUnit));

                cotDetail.addChild(heightUnitDetail);

                heightDetail.setAttribute(KEY_UNIT, MAPPING_HEIGHT_UNIT[customBytesExtFields.heightUnit]);
            }

            if (customBytesExtFields.heightValue != null) {
                String heightValueStr = Float.toString(customBytesExtFields.heightValue);
                heightDetail.setInnerText(heightValueStr);
                heightDetail.setAttribute(KEY_VALUE, heightValueStr);
            }

            cotDetail.addChild(heightDetail);
        }
    }

}
