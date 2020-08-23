package com.paulmandal.atak.forwarder.comm.protobuf;

import android.util.Log;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.cotutils.CotMessageTypes;
import com.paulmandal.atak.forwarder.protobufs.CotEventMinimalProtos;
import com.paulmandal.atak.forwarder.protobufs.Minimalcontact;
import com.paulmandal.atak.forwarder.protobufs.Minimaldetail;
import com.paulmandal.atak.forwarder.protobufs.Minimalgroup;
import com.paulmandal.atak.forwarder.protobufs.Minimallink;
import com.paulmandal.atak.forwarder.protobufs.Minimalremarks;
import com.paulmandal.atak.forwarder.protobufs.Minimaltakv;
import com.paulmandal.atak.forwarder.protobufs.Minimaltrack;

import org.apache.commons.lang.ArrayUtils;

import java.text.ParseException;

public class MinimalCotProtobufConverter {
    private static final String TAG = "ATAKDBG." + MinimalCotProtobufConverter.class.getSimpleName();

    private static final int LONG_INT_LENGTH = 64;
    private static final int INT_LENGTH = 32;

    /**
     * CotDetail fields
     */
    private static final String KEY_DETAIL = "detail";
    private static final String KEY_CONTACT = "contact";
    private static final String KEY_GROUP = "__group";
    private static final String KEY_PRECISIONLOCATION = "precisionlocation";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TAKV = "takv";
    private static final String KEY_TRACK = "track";
    private static final String KEY_REMARKS = "remarks";
    private static final String KEY_LINK = "link";
    private static final String KEY_USER_ICON = "usericon";

    // Contact
    private static final String KEY_CALLSIGN = "callsign";
    private static final String KEY_ENDPOINT = "endpoint";

    // Group
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    // PrecisionLocation
    private static final String KEY_GEOPOINTSRC = "geopointsrc";
    private static final String KEY_ALTSRC = "altsrc";

    // Status
    private static final String KEY_BATTERY = "battery";
    private static final String KEY_READINESS = "readiness";

    // TakV
    private static final String KEY_DEVICE = "device";
    private static final String KEY_OS = "os";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_VERSION = "version";

    // Track
    private static final String KEY_COURSE = "course";
    private static final String KEY_SPEED = "speed";

    // UserIcon
    private static final String KEY_ICON_SET_PATH = "iconsetpath";

    // Link
    private static final String KEY_UID = "uid";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PARENT_CALLSIGN = "parent_callsign";
    private static final String KEY_RELATION = "relation";
    private static final String KEY_PRODUCTION_TIME = "production_time";

    private static final String VALUE_TRUE = "true";

    /**
     * Mappings
     */
    private static final String[] MAPPING_TYPE = {
            CotMessageTypes.TYPE_PLI,
            CotMessageTypes.TYPE_NEUTRAL_MARKER,
            CotMessageTypes.TYPE_HOSTILE_MARKER,
            CotMessageTypes.TYPE_FRIENDLY_MARKER,
            CotMessageTypes.TYPE_UNKNOWN_MARKER
    };

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
            "Team Lead"
    };

    private final long mStartOfYearMs;

    public MinimalCotProtobufConverter(long startOfYearMs) {
        mStartOfYearMs = startOfYearMs;
    }

    public boolean isSupportedType(String type) {
        Log.d(TAG, "isSupportedType: " + type + ": " + ArrayUtils.contains(MAPPING_TYPE, type));
        return ArrayUtils.contains(MAPPING_TYPE, type);
    }

    public byte[] toByteArray(CotEvent cotEvent) {
        CotEventMinimalProtos.MinimalCotEvent cotEventProtobuf = toCotEventProtobuf(cotEvent);
        return cotEventProtobuf.toByteArray();
    }

    public CotEvent toCotEvent(byte[] cotProtobuf) {
        CotEvent cotEvent = null;

        try {
            CotEventMinimalProtos.MinimalCotEvent protoCotEvent = CotEventMinimalProtos.MinimalCotEvent.parseFrom(cotProtobuf);

            CustomBytesFields customBytesFields = unpackCustomBytes(protoCotEvent.getCustomBytes());
            CustomBytesExtFields customBytesExtFields = unpackCustomBytesExt(protoCotEvent.getCustomBytesExt());

            cotEvent = new CotEvent();
            cotEvent.setUID(protoCotEvent.getUid());
            cotEvent.setType(customBytesFields.type);
            cotEvent.setTime(customBytesFields.time);
            cotEvent.setStart(customBytesFields.time);
            cotEvent.setStale(customBytesFields.stale);
            cotEvent.setHow(customBytesExtFields.how);
            cotEvent.setDetail(cotDetailFromProtoDetail(protoCotEvent.getDetail(), customBytesExtFields));
            cotEvent.setPoint(new CotPoint(protoCotEvent.getLat(), protoCotEvent.getLon(), customBytesFields.hae, protoCotEvent.getCe(), protoCotEvent.getLe()));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return cotEvent;
    }

    /**
     * toByteArray
     */
    private CotEventMinimalProtos.MinimalCotEvent toCotEventProtobuf(CotEvent cotEvent) {
        CotEventMinimalProtos.MinimalCotEvent.Builder builder = CotEventMinimalProtos.MinimalCotEvent.newBuilder();

        if (cotEvent.getUID() != null) {
            builder.setUid(cotEvent.getUID());
        }

        CotPoint cotPoint = cotEvent.getCotPoint();

        if (cotPoint != null) {
            builder.setLat(cotPoint.getLat());
            builder.setLon(cotPoint.getLon());
            builder.setCe((int)cotPoint.getCe());
            builder.setLe((int)cotPoint.getLe());
        }

        double hae = cotPoint != null ? cotPoint.getHae() : 0;
        builder.setCustomBytes(packCustomBytes(cotEvent.getType(), cotEvent.getTime(), cotEvent.getStale(), hae));

        String geoPointSrc = null;
        String altSrc = null;
        CotDetail precisionLocation = cotEvent.getDetail().getFirstChildByName(0, KEY_PRECISIONLOCATION);
        if (precisionLocation != null) {
            geoPointSrc = precisionLocation.getAttribute(KEY_GEOPOINTSRC);
            altSrc = precisionLocation.getAttribute(KEY_ALTSRC);
        }

        String groupRole = null;
        CotDetail group = cotEvent.getDetail().getFirstChildByName(0, KEY_GROUP);
        if (group != null) {
            groupRole = group.getAttribute(KEY_ROLE);
        }

        Integer battery = null;
        Boolean readiness = null;
        CotDetail status = cotEvent.getDetail().getFirstChildByName(0, KEY_STATUS);
        if (status != null) {
            String batteryStr = status.getAttribute(KEY_BATTERY);
            if (batteryStr != null) {
                battery = Integer.parseInt(batteryStr);
            }

            String readinessStr = status.getAttribute(KEY_READINESS);
            if (readinessStr != null) {
                readiness = Boolean.parseBoolean(readinessStr);
            }
        }

        builder.setCustomBytesExt(packCustomBytesExt(cotEvent.getHow(), geoPointSrc, altSrc, groupRole, battery, readiness));

        builder.setDetail(toDetail(cotEvent.getDetail()));

        return builder.build();
    }

    private Minimaldetail.MinimalDetail toDetail(CotDetail cotDetail) {
        Minimaldetail.MinimalDetail.Builder builder = Minimaldetail.MinimalDetail.newBuilder();

        if (cotDetail.getElementName().equals(KEY_DETAIL)) { // TODO: do we need this check?
            for (CotDetail innerDetail : cotDetail.getChildren()) {
                switch (innerDetail.getElementName()) {
                    case KEY_CONTACT:
                        builder.setContact(toContact(innerDetail));
                        break;
                    case KEY_GROUP:
                        builder.setGroup(toGroup(innerDetail));
                        break;
                    case KEY_TAKV:
                        builder.setTakv(toTakv(innerDetail));
                        break;
                    case KEY_TRACK:
                        builder.setTrack(toTrack(innerDetail));
                        break;
                    case KEY_REMARKS:
                        builder.setRemarks(toRemarks(innerDetail));
                        break;
                    case KEY_LINK:
                        builder.setLink(toLink(innerDetail));
                        break;
                    case KEY_USER_ICON:
                        builder.setIconSetPath(innerDetail.getAttribute(KEY_ICON_SET_PATH));
                        break;
                }
            }
        }
        return builder.build();
    }

    private Minimalcontact.MinimalContact toContact(CotDetail cotDetail) {
        Minimalcontact.MinimalContact.Builder builder = Minimalcontact.MinimalContact.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_CALLSIGN:
                    builder.setCallsign(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Minimalgroup.MinimalGroup toGroup(CotDetail cotDetail) {
        Minimalgroup.MinimalGroup.Builder builder = Minimalgroup.MinimalGroup.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_NAME:
                    builder.setName(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Minimaltakv.MinimalTakv toTakv(CotDetail cotDetail) {
        Minimaltakv.MinimalTakv.Builder builder = Minimaltakv.MinimalTakv.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_DEVICE:
                    builder.setDevice(attribute.getValue());
                    break;
                case KEY_OS:
                    builder.setOs(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_PLATFORM:
                    builder.setPlatform(attribute.getValue());
                    break;
                case KEY_VERSION:
                    builder.setVersion(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Minimaltrack.MinimalTrack toTrack(CotDetail cotDetail) {
        Minimaltrack.MinimalTrack.Builder builder = Minimaltrack.MinimalTrack.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_COURSE:
                    builder.setCourse(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_SPEED:
                    builder.setSpeed(Double.parseDouble(attribute.getValue()));
                    break;
            }
        }
        return builder.build();
    }

    private Minimalremarks.MinimalRemarks toRemarks(CotDetail cotDetail) {
        Minimalremarks.MinimalRemarks.Builder builder = Minimalremarks.MinimalRemarks.newBuilder();
        String remarks = cotDetail.getInnerText();
        if (remarks != null) {
            builder.setRemarks(remarks);
        }
        return builder.build();
    }

    private Minimallink.MinimalLink toLink(CotDetail cotDetail) {
        Minimallink.MinimalLink.Builder builder = Minimallink.MinimalLink.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UID:
                    builder.setUid(attribute.getValue());
                    break;
                case KEY_TYPE:
                    builder.setType(attribute.getValue());
                    break;
                case KEY_PARENT_CALLSIGN:
                    builder.setParentCallsign(attribute.getValue());
                    break;
                case KEY_RELATION:
                    builder.setRelation(attribute.getValue());
                    break;
                case KEY_PRODUCTION_TIME:
                    try {
                        long productionTime = CoordinatedTime.fromCot(attribute.getValue()).getMilliseconds();
                        long sinceStartOfYear = (productionTime - mStartOfYearMs) / 1000;
                        builder.setProductionTime((int) sinceStartOfYear);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        return builder.build();
    }

    /**
     * CustomBytes Format (fixed64):
     *
     * type   (6 bits, mapping)
     * time   (25 bits, seconds since start of year)
     * stale  (17 bits, seconds after `time`)
     * hae    (14 bits, whole meters)
     *
     * CustomBytesExt Format (fixed32):
     * how              (3 bits, mapping)
     * geopointsrc      (6 bits, mapping, high bit marks whether we have a value)
     * altsrc           (6 bits, mapping, high bit marks whether we have a value)
     * group.role       (5 bits, mapping, high bit marks whether we have a value)
     * status.battery   (8 bits, int, high bit marks whether we have a value)
     * status.readiness (2 bits, bool, high bit marks whether we have a value)
     */

    private static final int CUSTOM_FIELD_TYPE_LENGTH = 6;
    private static final int CUSTOM_FIELD_TIME_LENGTH = 25;
    private static final int CUSTOM_FIELD_STALE_LENGTH = 17;
    private static final int CUSTOM_FIELD_HAE_LENGTH = 14;

    private static final int CUSTOM_FIELD_HOW_LENGTH = 3;
    private static final int CUSTOM_FIELD_GEOPOINTSRC_LENGTH = 6;
    private static final int CUSTOM_FIELD_ALTSRC_LENGTH = 6;
    private static final int CUSTOM_FIELD_ROLE_LENGTH = 5;
    private static final int CUSTOM_FIELD_BATTERY_LENGTH = 8;
    private static final int CUSTOM_FIELD_READINESS_LENGTH = 2;

    private long packCustomBytes(String type, CoordinatedTime time, CoordinatedTime stale, double hae) {
        long customBytes = 0;
        int accumlatedShift = 0;

        int typeAsInt = findMappingForArray("type", MAPPING_TYPE, type);
        customBytes |= ((long)typeAsInt & createBitMask(CUSTOM_FIELD_TYPE_LENGTH)) << LONG_INT_LENGTH - CUSTOM_FIELD_TYPE_LENGTH;
        accumlatedShift += CUSTOM_FIELD_TYPE_LENGTH;

        long timeSinceStartOfYear = (time.getMilliseconds() - mStartOfYearMs) / 1000L;
        customBytes |= (timeSinceStartOfYear & createBitMask(CUSTOM_FIELD_TIME_LENGTH)) << LONG_INT_LENGTH - accumlatedShift - CUSTOM_FIELD_TIME_LENGTH;
        accumlatedShift += CUSTOM_FIELD_TIME_LENGTH;

        long timeUntilStale = (stale.getMilliseconds() - time.getMilliseconds()) / 1000L;
        customBytes |= (timeUntilStale & createBitMask(CUSTOM_FIELD_STALE_LENGTH)) << LONG_INT_LENGTH - accumlatedShift - CUSTOM_FIELD_STALE_LENGTH;
        accumlatedShift += CUSTOM_FIELD_STALE_LENGTH;

        customBytes |= ((long)hae & createBitMask(CUSTOM_FIELD_HAE_LENGTH)) << LONG_INT_LENGTH - accumlatedShift - CUSTOM_FIELD_HAE_LENGTH;
        accumlatedShift += CUSTOM_FIELD_HAE_LENGTH;

        return customBytes;
    }

    private int packCustomBytesExt(String how, String geoPointSrc, String altSrc, String groupRole, Integer battery, Boolean readiness) {
        int customBytes = 0;
        int accumulatedShift = 0;

        int howAsInt = findMappingForArray("how", MAPPING_HOW, how);
        customBytes |= (howAsInt & createBitMask(CUSTOM_FIELD_HOW_LENGTH)) << INT_LENGTH - CUSTOM_FIELD_HOW_LENGTH;
        accumulatedShift += CUSTOM_FIELD_HOW_LENGTH;

        if (geoPointSrc != null) {
            int geoPointSrcAsInt = findMappingForArray("geopointsrc", MAPPING_ALTSRC_AND_GEOPOINTSRC, geoPointSrc);
            customBytes |= (geoPointSrcAsInt & createBitMask(CUSTOM_FIELD_GEOPOINTSRC_LENGTH)) << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_GEOPOINTSRC_LENGTH;
        } else {
            customBytes |= 1L << INT_LENGTH - accumulatedShift - 1;
        }
        accumulatedShift += CUSTOM_FIELD_GEOPOINTSRC_LENGTH;

        if (altSrc != null) {
            int altSrcAsInt = findMappingForArray("altsrc", MAPPING_ALTSRC_AND_GEOPOINTSRC, altSrc);
            customBytes |= (altSrcAsInt & createBitMask(CUSTOM_FIELD_ALTSRC_LENGTH)) << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ALTSRC_LENGTH;
        } else {
            customBytes |= 1L << INT_LENGTH - accumulatedShift - 1;
        }
        accumulatedShift += CUSTOM_FIELD_ALTSRC_LENGTH;

        if (groupRole != null) {
            int groupRoleAsInt = findMappingForArray("role", MAPPING_GROUP_ROLE, groupRole);
            customBytes |= (groupRoleAsInt & createBitMask(CUSTOM_FIELD_ROLE_LENGTH)) << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ROLE_LENGTH;
        } else {
            customBytes |= 1L << INT_LENGTH - accumulatedShift - 1;
        }
        accumulatedShift += CUSTOM_FIELD_ROLE_LENGTH;

        if (battery != null) {
            customBytes |= (battery & createBitMask(CUSTOM_FIELD_BATTERY_LENGTH)) << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_BATTERY_LENGTH;
        } else {
            // Mark the high bit 1 to indicate no value
            customBytes |= 1L << INT_LENGTH - accumulatedShift - 1;
        }
        accumulatedShift += CUSTOM_FIELD_BATTERY_LENGTH;

        if (readiness != null) {
            customBytes |= ((readiness ? 1 : 0) & createBitMask(CUSTOM_FIELD_READINESS_LENGTH)) << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_READINESS_LENGTH;
        } else {
            // Mark the high bit 1 to indicate no value
            customBytes |= 1L << INT_LENGTH - accumulatedShift - 1;
        }
        accumulatedShift += CUSTOM_FIELD_READINESS_LENGTH;

        return customBytes;
    }

    /**
     * toCotEvent
     */

    private CotDetail cotDetailFromProtoDetail(Minimaldetail.MinimalDetail detail, CustomBytesExtFields customBytesExtFields) {
        CotDetail cotDetail = new CotDetail();

        Minimaltakv.MinimalTakv takv = detail.getTakv();
        if (takv != null && takv != Minimaltakv.MinimalTakv.getDefaultInstance()) {
            CotDetail takvDetail = new CotDetail(KEY_TAKV);

            takvDetail.setAttribute(KEY_OS, Integer.toString(takv.getOs()));
            if (!isNullOrEmpty(takv.getVersion())) {
                takvDetail.setAttribute(KEY_VERSION, takv.getVersion());
            }
            if (!isNullOrEmpty(takv.getDevice())) {
                takvDetail.setAttribute(KEY_DEVICE, takv.getDevice());
            }
            if (!isNullOrEmpty(takv.getPlatform())) {
                takvDetail.setAttribute(KEY_PLATFORM, takv.getPlatform());
            }
            cotDetail.addChild(takvDetail);
        }

        Minimalcontact.MinimalContact contact = detail.getContact();
        if (contact != null && contact != Minimalcontact.MinimalContact.getDefaultInstance()) {
            CotDetail contactDetail = new CotDetail(KEY_CONTACT);

            if (!isNullOrEmpty(contact.getCallsign())) {
                contactDetail.setAttribute(KEY_CALLSIGN, contact.getCallsign());
            }

            cotDetail.addChild(contactDetail);
        }

        if (!isNullOrEmpty(customBytesExtFields.altSrc) || !isNullOrEmpty(customBytesExtFields.geoPointSrc)) {
            CotDetail precisionLocationDetail = new CotDetail(KEY_PRECISIONLOCATION);

            if (!isNullOrEmpty(customBytesExtFields.altSrc)) {
                precisionLocationDetail.setAttribute(KEY_ALTSRC, customBytesExtFields.altSrc);
            }
            if (!isNullOrEmpty(customBytesExtFields.geoPointSrc)) {
                precisionLocationDetail.setAttribute(KEY_GEOPOINTSRC, customBytesExtFields.geoPointSrc);
            }

            cotDetail.addChild(precisionLocationDetail);
        }

        Minimalgroup.MinimalGroup group = detail.getGroup();
        if (group != null && group != Minimalgroup.MinimalGroup.getDefaultInstance()) {
            CotDetail groupDetail = new CotDetail(KEY_GROUP);

            if (!isNullOrEmpty(group.getName())) {
                groupDetail.setAttribute(KEY_NAME, group.getName());
            }
            if (!isNullOrEmpty(customBytesExtFields.role)) {
                groupDetail.setAttribute(KEY_ROLE, customBytesExtFields.role);
            }

            cotDetail.addChild(groupDetail);
        }

        if (customBytesExtFields.battery != null || customBytesExtFields.readiness != null) {
            CotDetail statusDetail = new CotDetail(KEY_STATUS);

            if (customBytesExtFields.battery != null) {
                statusDetail.setAttribute(KEY_BATTERY, Integer.toString(customBytesExtFields.battery));
            }
            if (customBytesExtFields.readiness != null) {
                statusDetail.setAttribute(KEY_READINESS, Boolean.toString(customBytesExtFields.readiness));
            }

            cotDetail.addChild(statusDetail);
        }

        Minimaltrack.MinimalTrack track = detail.getTrack();
        if (track != null && track != Minimaltrack.MinimalTrack.getDefaultInstance()) {
            CotDetail trackDetail = new CotDetail(KEY_TRACK);

            if (track.getCourse() != 0.0) {
                trackDetail.setAttribute(KEY_COURSE, Double.toString(track.getCourse()));
            }
            if (track.getSpeed() != 0.0) { // TODO: better handling of nulls
                trackDetail.setAttribute(KEY_SPEED, Double.toString(track.getSpeed()));
            }
            cotDetail.addChild(trackDetail);
        }

        Minimalremarks.MinimalRemarks remarks = detail.getRemarks();
        if (remarks != null && remarks != Minimalremarks.MinimalRemarks.getDefaultInstance()) {
            CotDetail remarksDetail = new CotDetail(KEY_REMARKS);

            if (!isNullOrEmpty(remarks.getRemarks())) {
                remarksDetail.setInnerText(remarks.getRemarks());
            }

            cotDetail.addChild(remarksDetail);
        }

        Minimallink.MinimalLink link = detail.getLink();
        if (link != null && link != Minimallink.MinimalLink.getDefaultInstance()) {
            CotDetail linkDetail = new CotDetail(KEY_LINK);

            if (!isNullOrEmpty(link.getUid())) {
                linkDetail.setAttribute(KEY_UID, link.getUid());
            }
            if (!isNullOrEmpty(link.getType())) {
                linkDetail.setAttribute(KEY_TYPE, link.getType());
            }
            if (!isNullOrEmpty(link.getParentCallsign())) {
                linkDetail.setAttribute(KEY_PARENT_CALLSIGN, link.getParentCallsign());
            }
            if (!isNullOrEmpty(link.getRelation())) {
                linkDetail.setAttribute(KEY_RELATION, link.getRelation());
            }
            long productionTimeOffset = link.getProductionTime();
            if (productionTimeOffset > 0) {
                long productionTimeMs = mStartOfYearMs + (productionTimeOffset * 1000);
                CoordinatedTime productionTime = new CoordinatedTime(productionTimeMs);
                linkDetail.setAttribute(KEY_PRODUCTION_TIME, productionTime.toString());
            }

            cotDetail.addChild(linkDetail);
        }

        String iconSetPath = detail.getIconSetPath();
        if (!isNullOrEmpty(iconSetPath)) {
            CotDetail userIconDetail = new CotDetail(KEY_USER_ICON);

            userIconDetail.setAttribute(KEY_ICON_SET_PATH, iconSetPath);

            cotDetail.addChild(userIconDetail);
        }

        return cotDetail;
    }

    private CustomBytesFields unpackCustomBytes(long customBytes) {
        int accumulatedShift = 0;

        int typeAsInt = (int)(customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_TYPE_LENGTH & createBitMask(CUSTOM_FIELD_TYPE_LENGTH));
        accumulatedShift += CUSTOM_FIELD_TYPE_LENGTH;
        String type = MAPPING_TYPE[typeAsInt];

        long timeSinceStartOfYear = customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_TIME_LENGTH & createBitMask(CUSTOM_FIELD_TIME_LENGTH);
        accumulatedShift += CUSTOM_FIELD_TIME_LENGTH;

        CoordinatedTime time = new CoordinatedTime(mStartOfYearMs + timeSinceStartOfYear * 1000);

        long timeUntilStale = customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_STALE_LENGTH & createBitMask(CUSTOM_FIELD_STALE_LENGTH);
        accumulatedShift += CUSTOM_FIELD_STALE_LENGTH;

        CoordinatedTime stale = new CoordinatedTime(time.getMilliseconds() + timeUntilStale * 1000);

        double hae = customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_HAE_LENGTH & createBitMask(CUSTOM_FIELD_HAE_LENGTH);

        return new CustomBytesFields(type, time, stale, hae);
    }

    private CustomBytesExtFields unpackCustomBytesExt(int customBytesExt) {
        int accumulatedShift = 0;

        int howAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_HOW_LENGTH & createBitMask(CUSTOM_FIELD_HOW_LENGTH);
        accumulatedShift += CUSTOM_FIELD_HOW_LENGTH;

        String how = MAPPING_HOW[howAsInt];

        boolean hasGeoPointSrc = (customBytesExt >> INT_LENGTH - accumulatedShift - 1 & createBitMask(1)) == 0;
        String geoPointSrc = null;
        if (hasGeoPointSrc) {
            int geoPointSrcAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_GEOPOINTSRC_LENGTH & createBitMask(CUSTOM_FIELD_GEOPOINTSRC_LENGTH);
            geoPointSrc = MAPPING_ALTSRC_AND_GEOPOINTSRC[geoPointSrcAsInt];
        }
        accumulatedShift += CUSTOM_FIELD_GEOPOINTSRC_LENGTH;

        boolean hasAltSrc = (customBytesExt >> INT_LENGTH - accumulatedShift - 1 & createBitMask(1)) == 0;
        String altSrc = null;
        if (hasAltSrc) {
            int altSrcAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ALTSRC_LENGTH & createBitMask(CUSTOM_FIELD_ALTSRC_LENGTH);
            altSrc = MAPPING_ALTSRC_AND_GEOPOINTSRC[altSrcAsInt];
        }
        accumulatedShift += CUSTOM_FIELD_ALTSRC_LENGTH;

        boolean hasGroupRole = (customBytesExt >> INT_LENGTH - accumulatedShift - 1 & createBitMask(1)) == 0;
        String role = null;
        if (hasGroupRole) {
            int groupRoleAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ROLE_LENGTH & createBitMask(CUSTOM_FIELD_ROLE_LENGTH);
            role = MAPPING_GROUP_ROLE[groupRoleAsInt];
        }
        accumulatedShift += CUSTOM_FIELD_ROLE_LENGTH;

        boolean hasBattery = (customBytesExt >>> INT_LENGTH - accumulatedShift - 1 & createBitMask(1)) == 0;
        Integer battery = null;
        if (hasBattery) {
            battery = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_BATTERY_LENGTH & createBitMask(CUSTOM_FIELD_BATTERY_LENGTH);
        }
        accumulatedShift += CUSTOM_FIELD_BATTERY_LENGTH;

        boolean hasReadiness = (customBytesExt >>> INT_LENGTH - accumulatedShift - 1 & createBitMask(1)) == 0;
        Boolean readiness = null;
        if (hasReadiness) {
            readiness = (customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_READINESS_LENGTH & createBitMask(CUSTOM_FIELD_READINESS_LENGTH)) == 1;
        }

        return new CustomBytesExtFields(how, geoPointSrc, altSrc, role, battery, readiness);
    }

    private int createBitMask(int bits) {
        int bitmask = bits > 0 ? 1 : 0;
        for (int i = 1 ; i < bits ; i++) {
            bitmask |= 1 << i;
        }
        return bitmask;
    }

    /**
     * Utils
     */
    private int findMappingForArray(String fieldName, Object[] array, Object objectToFind) {
        int index = ArrayUtils.indexOf(array, objectToFind);

        if (index == -1) {
            throw new RuntimeException("Could not find mapping for field: " + fieldName + ", value: " + objectToFind);
        }
        return index;
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

//    private long addBits(long customBytes, long value, int fieldLength, BitsWrittenTracker tracker) {
//
//    }

    /**
     * Data Classes
     */
    private static class CustomBytesFields {
        public final String type;
        public final CoordinatedTime time;
        public final CoordinatedTime stale;
        public final double hae;

        public CustomBytesFields(String type, CoordinatedTime time, CoordinatedTime stale, double hae) {
            this.type = type;
            this.time = time;
            this.stale = stale;
            this.hae = hae;
        }
    }

    private static class CustomBytesExtFields {
        public final String how;
        public final String geoPointSrc;
        public final String altSrc;
        public final String role;
        public final Integer battery;
        public final Boolean readiness;

        public CustomBytesExtFields(String how, String geoPointSrc, String altSrc, String role, Integer battery, Boolean readiness) {
            this.how = how;
            this.geoPointSrc = geoPointSrc;
            this.altSrc = altSrc;
            this.role = role;
            this.battery = battery;
            this.readiness = readiness;
        }
    }

    private static class BitsWrittenTracker {
        public int bitsWritten = 0;
    }
}
