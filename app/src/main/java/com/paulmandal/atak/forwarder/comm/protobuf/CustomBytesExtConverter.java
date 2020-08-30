package com.paulmandal.atak.forwarder.comm.protobuf;

/**
 * CustomBytesExt Format (fixed64):
 * how              (3 bits, mapping)
 * geopointsrc      (6 bits, mapping, nullable)
 * altsrc           (6 bits, mapping, nullable)
 * group.role       (5 bits, mapping, nullable)
 * status.battery   (8 bits, int, nullable)
 * status.readiness (2 bits, bool, nullable)
 * labels_on.value  (2 bits, bool, nullable)
 * height.unit      (4 bits, int, nullable)
 * ce_human_input   (2 bits, bool, nullable)
 */
public class CustomBytesExtConverter {
    private static final int LONG_INT_LENGTH = 64;

    private static final int CUSTOM_FIELD_HOW_LENGTH = 3;
    private static final int CUSTOM_FIELD_GEOPOINTSRC_LENGTH = 6;
    private static final int CUSTOM_FIELD_ALTSRC_LENGTH = 6;
    private static final int CUSTOM_FIELD_ROLE_LENGTH = 5;
    private static final int CUSTOM_FIELD_BATTERY_LENGTH = 8;
    private static final int CUSTOM_FIELD_READINESS_LENGTH = 2;
    private static final int CUSTOM_FIELD_LABELS_ON_LENGTH = 2;
    private static final int CUSTOM_FIELD_HEIGHT_UNIT_LENGTH = 4;
    private static final int CUSTOM_FIELD_CE_HUMAN_INPUT_LENGTH = 2;

    private static final String[] MAPPING_HOW = {
            "h-e",
            "h-g-i-g-o",
            "m-g"
    };

    private static final String[] MAPPING_ALTSRC_AND_GEOPOINTSRC = {
            "DTED0",
            "DTED1",
            "DTED2",
            "DTED3",
            "LIDAR",
            "PFI",
            "USER",
            "???",
            "GPS",
            "SRTM1",
            "COT",
            "PRI",
            "CALC",
            "ESTIMATED",
            "RTK",
            "DGPS",
            "GPS_PPS"
    };

    private static final String[] MAPPING_GROUP_ROLE = {
            "Team Member",
            "Team Lead",
            "HQ",
            "Sniper",
            "Medic",
            "Forward Observer",
            "RTO",
            "K9"
    };

    public long packCustomBytesExt(CustomBytesExtFields customBytesExtFields) throws MappingNotFoundException {
        ShiftTracker shiftTracker = new ShiftTracker();
        long customBytes = 0;

        customBytes = BitUtils.packNonNullMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.how, "how", CUSTOM_FIELD_HOW_LENGTH, MAPPING_HOW, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.geoPointSrc, "geopointsrc", CUSTOM_FIELD_GEOPOINTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.altSrc, "altsrc", CUSTOM_FIELD_ALTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.role, "role", CUSTOM_FIELD_ROLE_LENGTH, MAPPING_GROUP_ROLE, shiftTracker);
        customBytes = BitUtils.packNullableInt(customBytes, LONG_INT_LENGTH, customBytesExtFields.battery, CUSTOM_FIELD_BATTERY_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.readiness, CUSTOM_FIELD_READINESS_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.labelsOn, CUSTOM_FIELD_LABELS_ON_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableInt(customBytes, LONG_INT_LENGTH, customBytesExtFields.heightUnit, CUSTOM_FIELD_HEIGHT_UNIT_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.ceHumanInput, CUSTOM_FIELD_CE_HUMAN_INPUT_LENGTH, shiftTracker);

        return customBytes;
    }

    public CustomBytesExtFields unpackCustomBytesExt(long customBytesExt) {
        ShiftTracker shiftTracker = new ShiftTracker();

        String how = BitUtils.unpackNonNullMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_HOW_LENGTH, MAPPING_HOW, shiftTracker);
        String geoPointSrc = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_GEOPOINTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        String altSrc = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ALTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        String role = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROLE_LENGTH, MAPPING_GROUP_ROLE, shiftTracker);
        Integer battery = BitUtils.unpackNullableInt(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_BATTERY_LENGTH, shiftTracker);
        Boolean readiness = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_READINESS_LENGTH, shiftTracker);
        Boolean labelsOn = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_LABELS_ON_LENGTH, shiftTracker);
        Integer heightUnit = BitUtils.unpackNullableInt(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_HEIGHT_UNIT_LENGTH, shiftTracker);
        Boolean ceHumanInput = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_CE_HUMAN_INPUT_LENGTH, shiftTracker);

        return new CustomBytesExtFields(how, geoPointSrc, altSrc, role, battery, readiness, labelsOn, heightUnit, ceHumanInput);
    }
}
