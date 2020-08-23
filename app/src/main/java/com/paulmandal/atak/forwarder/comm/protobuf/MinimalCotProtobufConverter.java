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
import com.paulmandal.atak.forwarder.protobufs.Minimaltakv;
import com.paulmandal.atak.forwarder.protobufs.Minimaltrack;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MinimalCotProtobufConverter {
    private static final String TAG = "ATAKDBG." + MinimalCotProtobufConverter.class.getSimpleName();

    private static final List<String> SUPPORTED_COT_TYPES = new ArrayList<>(Arrays.asList(CotMessageTypes.TYPE_PLI));

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

    private static final String KEY_CALLSIGN = "callsign";
    private static final String KEY_ENDPOINT = "endpoint";

    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    private static final String KEY_GEOPOINTSRC = "geopointsrc";
    private static final String KEY_ALTSRC = "altsrc";

    private static final String KEY_BATTERY = "battery";
    private static final String KEY_READINESS = "readiness";

    private static final String KEY_DEVICE = "device";
    private static final String KEY_OS = "os";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_VERSION = "version";

    private static final String KEY_COURSE = "course";
    private static final String KEY_SPEED = "speed";

    private static final String VALUE_TRUE = "true";

    /**
     * Mappings
     */
    private static final String[] MAPPING_TYPE = {
            "a-f-G-U-C"
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

    public boolean isSupportedType(String type) {
        Log.d(TAG, "isSupportedType: " + type + ": " + SUPPORTED_COT_TYPES.contains(type));
        return SUPPORTED_COT_TYPES.contains(type);
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

        int battery = 0;
        CotDetail status = cotEvent.getDetail().getFirstChildByName(0, KEY_STATUS);
        if (status != null) {
            battery = Integer.parseInt(status.getAttribute(KEY_BATTERY));
        }

        builder.setCustomBytesExt(packCustomBytesExt(cotEvent.getHow(), geoPointSrc, altSrc, groupRole, battery));

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
     * geopointsrc      (5 bits, mapping)
     * altsrc           (5 bits, mapping)
     * group.role       (4 bits, mapping)
     * status.battery   (7 bits, int)
     * status.readiness (1 bit, int)
     */

    private static final int CUSTOM_FIELD_TYPE_LENGTH = 6;
    private static final int CUSTOM_FIELD_TIME_LENGTH = 25;
    private static final int CUSTOM_FIELD_STALE_LENGTH = 17;
    private static final int CUSTOM_FIELD_HAE_LENGTH = 14;

    private static final int CUSTOM_FIELD_HOW_LENGTH = 3;
    private static final int CUSTOM_FIELD_GEOPOINTSRC_LENGTH = 5;
    private static final int CUSTOM_FIELD_ALTSRC_LENGTH = 5;
    private static final int CUSTOM_FIELD_ROLE_LENGTH = 4;
    private static final int CUSTOM_FIELD_BATTERY_LENGTH = 7;
    private static final int CUSTOM_FIELD_READINESS_LENGTH = 1;

    private long packCustomBytes(String type, CoordinatedTime time, CoordinatedTime stale, double hae) {
        long customBytes = 0;
        int accumlatedShift = 0;

        int typeAsInt = findMappingForArray("type", MAPPING_TYPE, type);
        customBytes |= (long)typeAsInt << LONG_INT_LENGTH - CUSTOM_FIELD_TYPE_LENGTH;
        accumlatedShift += CUSTOM_FIELD_TYPE_LENGTH;

        // TODO: move this to ctor so we don't have to calculate it over and over
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfYear = cal.getTime().getTime();
        long timeSinceStartOfYear = (time.getMilliseconds() - startOfYear) / 1000L;

        customBytes |= timeSinceStartOfYear << LONG_INT_LENGTH - accumlatedShift - CUSTOM_FIELD_TIME_LENGTH;
        accumlatedShift += CUSTOM_FIELD_TIME_LENGTH;

        long timeUntilStale = (stale.getMilliseconds() - time.getMilliseconds()) / 1000L;
        customBytes |= timeUntilStale << LONG_INT_LENGTH - accumlatedShift - CUSTOM_FIELD_STALE_LENGTH;
        accumlatedShift += CUSTOM_FIELD_STALE_LENGTH;

        customBytes |= (long)hae << LONG_INT_LENGTH - accumlatedShift - CUSTOM_FIELD_HAE_LENGTH;
        accumlatedShift += CUSTOM_FIELD_HAE_LENGTH;

        return customBytes;
    }

    private int packCustomBytesExt(String how, String geoPointSrc, String altSrc, String groupRole, int battery) {
        int customBytes = 0;
        int accumulatedShift = 0;

        int howAsInt = findMappingForArray("how", MAPPING_HOW, how);
        customBytes |= howAsInt << INT_LENGTH - CUSTOM_FIELD_HOW_LENGTH;
        accumulatedShift += CUSTOM_FIELD_HOW_LENGTH;

        int geoPointSrcAsInt = findMappingForArray("geopointsrc", MAPPING_ALTSRC_AND_GEOPOINTSRC, geoPointSrc);
        customBytes |= geoPointSrcAsInt << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_GEOPOINTSRC_LENGTH;
        accumulatedShift += CUSTOM_FIELD_GEOPOINTSRC_LENGTH;

        int altSrcAsInt = findMappingForArray("altsrc", MAPPING_ALTSRC_AND_GEOPOINTSRC, altSrc);
        customBytes |= altSrcAsInt << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ALTSRC_LENGTH;
        accumulatedShift += CUSTOM_FIELD_ALTSRC_LENGTH;

        int groupRoleAsInt = findMappingForArray("role", MAPPING_GROUP_ROLE, groupRole);
        customBytes |= groupRoleAsInt << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ROLE_LENGTH;
        accumulatedShift += CUSTOM_FIELD_ROLE_LENGTH;

        customBytes |= battery << INT_LENGTH - accumulatedShift - CUSTOM_FIELD_BATTERY_LENGTH;
        accumulatedShift += CUSTOM_FIELD_BATTERY_LENGTH;

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

        // TODO: how to handle null for this and other fields?
        if (customBytesExtFields.battery > 0) {
            CotDetail statusDetail = new CotDetail(KEY_STATUS);

            statusDetail.setAttribute(KEY_BATTERY, Integer.toString(customBytesExtFields.battery));

            cotDetail.addChild(statusDetail);
        }

        Minimaltrack.MinimalTrack track = detail.getTrack();

        if (track != null && track != Minimaltrack.MinimalTrack.getDefaultInstance()) {
            CotDetail trackDetail = new CotDetail(KEY_TRACK);

            if (track.getCourse() != 0.0) {
                trackDetail.setAttribute(KEY_COURSE, Double.toString(track.getCourse()));
            }
            if (track.getSpeed() != 0.0) {
                trackDetail.setAttribute(KEY_SPEED, Double.toString(track.getSpeed()));
            }
            cotDetail.addChild(trackDetail);
        }

        return cotDetail;
    }

    private CustomBytesFields unpackCustomBytes(long customBytes) {
        int accumulatedShift = 0;

        Log.d(TAG, "unpackCustomBytes: " + Long.toBinaryString(customBytes));

        Log.d(TAG, "unpackCustomBytes: " + Long.toBinaryString(customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_TYPE_LENGTH & createBitMask(CUSTOM_FIELD_TYPE_LENGTH)));

        int typeAsInt = (int)(customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_TYPE_LENGTH & createBitMask(CUSTOM_FIELD_TYPE_LENGTH));
        Log.d(TAG, "typeAsInt: " + typeAsInt);
        accumulatedShift += CUSTOM_FIELD_TYPE_LENGTH;
        String type = MAPPING_TYPE[typeAsInt];

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfYear = cal.getTime().getTime();
        long timeSinceStartOfYear = customBytes >>> LONG_INT_LENGTH - accumulatedShift - CUSTOM_FIELD_TIME_LENGTH & createBitMask(CUSTOM_FIELD_TIME_LENGTH);
        accumulatedShift += CUSTOM_FIELD_TIME_LENGTH;

        CoordinatedTime time = new CoordinatedTime(startOfYear + timeSinceStartOfYear * 1000);

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

        int geoPointSrcAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_GEOPOINTSRC_LENGTH & createBitMask(CUSTOM_FIELD_GEOPOINTSRC_LENGTH);
        accumulatedShift += CUSTOM_FIELD_GEOPOINTSRC_LENGTH;

        String geoPointSrc = MAPPING_ALTSRC_AND_GEOPOINTSRC[geoPointSrcAsInt];

        int altSrcAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ALTSRC_LENGTH & createBitMask(CUSTOM_FIELD_ALTSRC_LENGTH);
        accumulatedShift += CUSTOM_FIELD_ALTSRC_LENGTH;

        String altSrc = MAPPING_ALTSRC_AND_GEOPOINTSRC[altSrcAsInt];

        int groupRoleAsInt = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_ROLE_LENGTH & createBitMask(CUSTOM_FIELD_ROLE_LENGTH);
        accumulatedShift += CUSTOM_FIELD_ROLE_LENGTH;

        String role = MAPPING_GROUP_ROLE[groupRoleAsInt];

        int battery = customBytesExt >>> INT_LENGTH - accumulatedShift - CUSTOM_FIELD_BATTERY_LENGTH & createBitMask(CUSTOM_FIELD_BATTERY_LENGTH);
        accumulatedShift += CUSTOM_FIELD_BATTERY_LENGTH;

        return new CustomBytesExtFields(how, geoPointSrc, altSrc, role, battery);
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
        public final int battery;

        public CustomBytesExtFields(String how, String geoPointSrc, String altSrc, String role, int battery) {
            this.how = how;
            this.geoPointSrc = geoPointSrc;
            this.altSrc = altSrc;
            this.role = role;
            this.battery = battery;
        }
    }
}
