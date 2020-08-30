package com.paulmandal.atak.forwarder.comm.protobuf;

/**
 * CustomBytesExt Format (fixed64):
 * how                 (3 bits, mapping)
 * geopointsrc         (6 bits, mapping, nullable)
 * altsrc              (6 bits, mapping, nullable)
 * group.role          (5 bits, mapping, nullable)
 * status.battery      (8 bits, int, nullable)
 * status.readiness    (2 bits, bool, nullable)
 * labels_on.value     (2 bits, bool, nullable)
 * height.unit         (4 bits, int, nullable)
 * ce_human_input      (2 bits, bool, nullable)
 * tog                 (2 bits, bool, nullable)
 * routePlanningMethod (2 bits, mapping, nullable)
 * routeMethod         (4 bits, mapping, nullable)
 * routeType           (2 bits, mapping, nullable)
 * routeRouteType      (2 bits, mapping, nullable)
 * routeOrder          (2 bits, int, nullable)
 * routeStroke         (8 bits, int, nullable)
 */
public class CustomBytesExtConverter {
    private static final int LONG_INT_LENGTH = 64;

    private static final int CUSTOM_FIELD_HOW_LENGTH = 3;
    private static final int CUSTOM_FIELD_GEOPOINTSRC_LENGTH = 6;
    private static final int CUSTOM_FIELD_ALTSRC_LENGTH = 6;
    private static final int CUSTOM_FIELD_ROLE_LENGTH = 5;
    private static final int CUSTOM_FIELD_BATTERY_LENGTH = 8;
    private static final int CUSTOM_FIELD_HEIGHT_UNIT_LENGTH = 4;
    private static final int NULLABLE_BOOLEAN_FIELD_LENGTH = 2;
    private static final int CUSTOM_FIELD_ROUTE_PLANNING_METHOD_LENGTH = 2;
    private static final int CUSTOM_FIELD_ROUTE_METHOD_LENGTH = 4;
    private static final int CUSTOM_FIELD_ROUTE_TYPE_LENGTH = 2;
    private static final int CUSTOM_FIELD_ROUTE_ROUTE_TYPE_LENGTH = 2;
    private static final int CUSTOM_FIELD_ROUTE_ORDER_LENGTH = 2;
    private static final int CUSTOM_FIELD_ROUTE_STROKE_LENGTH = 8;

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

    private static String[] MAPPING_ROUTE_PLANNING_METHOD = {
            "Infil",
            "Exfil"
    };

    private static String[] MAPPING_ROUTE_METHOD = {
            "Driving",
            "Walking",
            "Flying",
            "Swimming",
            "Watercraft"
    };

    private static String[] MAPPING_ROUTE_TYPE = {
            "On Foot",
            "Vehicle"
    };

    private static String[] MAPPING_ROUTE_ROUTE_TYPE = {
            "Primary",
            "Secondary"
    };

    private static String[] MAPPING_ROUTE_ORDER = {
            "Ascending Check Points",
            "Descending Check Points"
    };


    public long packCustomBytesExt(CustomBytesExtFields customBytesExtFields) throws MappingNotFoundException {
        ShiftTracker shiftTracker = new ShiftTracker();
        long customBytes = 0;

        customBytes = BitUtils.packNonNullMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.how, "how", CUSTOM_FIELD_HOW_LENGTH, MAPPING_HOW, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.geoPointSrc, "geopointsrc", CUSTOM_FIELD_GEOPOINTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.altSrc, "altsrc", CUSTOM_FIELD_ALTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.role, "role", CUSTOM_FIELD_ROLE_LENGTH, MAPPING_GROUP_ROLE, shiftTracker);
        customBytes = BitUtils.packNullableInt(customBytes, LONG_INT_LENGTH, customBytesExtFields.battery, CUSTOM_FIELD_BATTERY_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.readiness, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.labelsOn, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableInt(customBytes, LONG_INT_LENGTH, customBytesExtFields.heightUnit, CUSTOM_FIELD_HEIGHT_UNIT_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.ceHumanInput, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableBoolean(customBytes, LONG_INT_LENGTH, customBytesExtFields.tog, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.routePlanningMethod, "routePlanningMethod", CUSTOM_FIELD_ROUTE_PLANNING_METHOD_LENGTH, MAPPING_ROUTE_PLANNING_METHOD, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.routeMethod, "routeMethod", CUSTOM_FIELD_ROUTE_METHOD_LENGTH, MAPPING_ROUTE_METHOD, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.routeType, "routeType", CUSTOM_FIELD_ROUTE_TYPE_LENGTH, MAPPING_ROUTE_TYPE, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.routeRouteType, "routeRouteType", CUSTOM_FIELD_ROUTE_ROUTE_TYPE_LENGTH, MAPPING_ROUTE_ROUTE_TYPE, shiftTracker);
        customBytes = BitUtils.packNullableMappedString(customBytes, LONG_INT_LENGTH, customBytesExtFields.routeOrder, "routeOrder", CUSTOM_FIELD_ROUTE_ORDER_LENGTH, MAPPING_ROUTE_ORDER, shiftTracker);
        customBytes = BitUtils.packNullableInt(customBytes, LONG_INT_LENGTH, customBytesExtFields.routeStroke, CUSTOM_FIELD_ROUTE_STROKE_LENGTH, shiftTracker);

        return customBytes;
    }

    public CustomBytesExtFields unpackCustomBytesExt(long customBytesExt) {
        ShiftTracker shiftTracker = new ShiftTracker();

        String how = BitUtils.unpackNonNullMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_HOW_LENGTH, MAPPING_HOW, shiftTracker);
        String geoPointSrc = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_GEOPOINTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        String altSrc = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ALTSRC_LENGTH, MAPPING_ALTSRC_AND_GEOPOINTSRC, shiftTracker);
        String role = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROLE_LENGTH, MAPPING_GROUP_ROLE, shiftTracker);
        Integer battery = BitUtils.unpackNullableInt(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_BATTERY_LENGTH, shiftTracker);
        Boolean readiness = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        Boolean labelsOn = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        Integer heightUnit = BitUtils.unpackNullableInt(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_HEIGHT_UNIT_LENGTH, shiftTracker);
        Boolean ceHumanInput = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        Boolean tog = BitUtils.unpackNullableBoolean(customBytesExt, LONG_INT_LENGTH, NULLABLE_BOOLEAN_FIELD_LENGTH, shiftTracker);
        String routePlanningMethod = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROUTE_PLANNING_METHOD_LENGTH, MAPPING_ROUTE_PLANNING_METHOD, shiftTracker);
        String routeMethod = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROUTE_METHOD_LENGTH, MAPPING_ROUTE_METHOD, shiftTracker);
        String routeType = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROUTE_TYPE_LENGTH, MAPPING_ROUTE_TYPE, shiftTracker);
        String routeRouteType = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROUTE_ROUTE_TYPE_LENGTH, MAPPING_ROUTE_ROUTE_TYPE, shiftTracker);
        String routeOrder = BitUtils.unpackNullableMappedString(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROUTE_ORDER_LENGTH, MAPPING_ROUTE_ORDER, shiftTracker);
        Integer routeStroke = BitUtils.unpackNullableInt(customBytesExt, LONG_INT_LENGTH, CUSTOM_FIELD_ROUTE_STROKE_LENGTH, shiftTracker);

        return new CustomBytesExtFields(how, geoPointSrc, altSrc, role, battery, readiness, labelsOn, heightUnit, ceHumanInput, tog, routePlanningMethod, routeMethod, routeType, routeRouteType, routeOrder, routeStroke);
    }
}
