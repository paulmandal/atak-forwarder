package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufCotEvent;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDetail;

public class CotEventProtobufConverter {
    private static final String TAG = "ATAKDBG." + CotEventProtobufConverter.class.getSimpleName();

    /**
     * CotDetail fields
     */
    private static final String KEY_DETAIL = "detail";
    private static final String KEY_CONTACT = "contact";
    private static final String KEY_UNDERSCORED_GROUP = "__group";
    private static final String KEY_PRECISIONLOCATION = "precisionlocation";
    private static final String KEY_COLOR = "color";
    private static final String KEY_ARCHIVE = "archive";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TAKV = "takv";
    private static final String KEY_TRACK = "track";
    private static final String KEY_REMARKS = "remarks";
    private static final String KEY_LINK = "link";
    private static final String KEY_USER_ICON = "usericon";
    private static final String KEY_CHAT = "__chat";
    private static final String KEY_SERVER_DESTINATION = "__serverdestination";
    private static final String KEY_MODEL = "model";
    private static final String KEY_LABELS_ON = "labels_on";
    private static final String KEY_HEIGHT_UNIT = "height_unit";
    private static final String KEY_HEIGHT = "height";

    // Fields
    private static final String KEY_ICON_SET_PATH = "iconsetpath";
    private static final String KEY_UID = "uid";

    /**
     * Special Values
     */
    private static final String GEOCHAT_MARKER = "GeoChat";

    private final TakvProtobufConverter mTakvProtobufConverter;
    private final TrackProtobufConverter mTrackProtobufConverter;
    private final ServerDestinationProtobufConverter mServerDestinationProtobufConverter;
    private final RemarksProtobufConverter mRemarksProtobufConverter;
    private final ContactProtobufConverter mContactProtobufConverter;
    private final UnderscoreGroupProtobufConverter mUnderscoreGroupProtobufConverter;
    private final CustomBytesConverter mCustomBytesConverter;
    private final CustomBytesExtConverter mCustomBytesExtConverter;
    private final ChatProtobufConverter mChatProtobufConverter;
    private final LinkProtobufConverter mLinkProtobufConverter;
    private final LabelsOnProtobufConverter mLabelsOnProtobufConverter;
    private final PrecisionLocationProtobufConverter mPrecisionLocationProtobufConverter;
    private final DroppedFieldConverter mDroppedFieldConverter;
    private final StatusProtobufConverter mStatusProtobufConverter;
    private final HeightAndHeightUnitProtobufConverter mHeightAndHeightUnitProtobufConverter;
    private final ModelProtobufConverter mModelProtobufConverter;
    private final long mStartOfYearMs;

    public CotEventProtobufConverter(TakvProtobufConverter takvProtobufConverter,
                                     TrackProtobufConverter trackProtobufConverter,
                                     ServerDestinationProtobufConverter serverDestinationProtobufConverter,
                                     RemarksProtobufConverter remarksProtobufConverter,
                                     ContactProtobufConverter contactProtobufConverter,
                                     UnderscoreGroupProtobufConverter underscoreGroupProtobufConverter,
                                     CustomBytesConverter customBytesConverter,
                                     CustomBytesExtConverter customBytesExtConverter,
                                     ChatProtobufConverter chatProtobufConverter,
                                     LinkProtobufConverter linkProtobufConverter,
                                     LabelsOnProtobufConverter labelsOnProtobufConverter,
                                     PrecisionLocationProtobufConverter precisionLocationProtobufConverter,
                                     DroppedFieldConverter droppedFieldConverter,
                                     StatusProtobufConverter statusProtobufConverter,
                                     HeightAndHeightUnitProtobufConverter heightAndHeightUnitProtobufConverter,
                                     ModelProtobufConverter modelProtobufConverter,
                                     long startOfYearMs) {
        mTakvProtobufConverter = takvProtobufConverter;
        mTrackProtobufConverter = trackProtobufConverter;
        mServerDestinationProtobufConverter = serverDestinationProtobufConverter;
        mRemarksProtobufConverter = remarksProtobufConverter;
        mContactProtobufConverter = contactProtobufConverter;
        mUnderscoreGroupProtobufConverter = underscoreGroupProtobufConverter;
        mCustomBytesConverter = customBytesConverter;
        mCustomBytesExtConverter = customBytesExtConverter;
        mChatProtobufConverter = chatProtobufConverter;
        mLinkProtobufConverter = linkProtobufConverter;
        mLabelsOnProtobufConverter = labelsOnProtobufConverter;
        mPrecisionLocationProtobufConverter = precisionLocationProtobufConverter;
        mDroppedFieldConverter = droppedFieldConverter;
        mStatusProtobufConverter = statusProtobufConverter;
        mHeightAndHeightUnitProtobufConverter = heightAndHeightUnitProtobufConverter;
        mModelProtobufConverter = modelProtobufConverter;
        mStartOfYearMs = startOfYearMs;
    }

    public byte[] toByteArray(CotEvent cotEvent) throws MappingNotFoundException, UnknownDetailFieldException {
        ProtobufCotEvent.MinimalCotEvent cotEventProtobuf = toCotEventProtobuf(cotEvent);
        return cotEventProtobuf.toByteArray();
    }

    public CotEvent toCotEvent(byte[] cotProtobuf) {
        CotEvent cotEvent = null;

        try {
            ProtobufCotEvent.MinimalCotEvent protoCotEvent = ProtobufCotEvent.MinimalCotEvent.parseFrom(cotProtobuf);

            CustomBytesFields customBytesFields = mCustomBytesConverter.unpackCustomBytes(protoCotEvent.getCustomBytes(), mStartOfYearMs);
            CustomBytesExtFields customBytesExtFields = mCustomBytesExtConverter.unpackCustomBytesExt(protoCotEvent.getCustomBytesExt());

            cotEvent = new CotEvent();

            SubstitutionValues substitutionValues = new SubstitutionValues();
            String uid = protoCotEvent.getUid();
            if (uid.startsWith(GEOCHAT_MARKER)) {
                String[] geoChatSplit = uid.split("\\.");
                substitutionValues.uidFromGeoChat = geoChatSplit[1];
                substitutionValues.chatroomFromGeoChat = geoChatSplit[2];
            }
            cotEvent.setUID(uid);
            cotEvent.setType(protoCotEvent.getType());
            cotEvent.setTime(customBytesFields.time);
            cotEvent.setStart(customBytesFields.time);
            cotEvent.setStale(customBytesFields.stale);
            cotEvent.setHow(customBytesExtFields.how);
            cotEvent.setDetail(cotDetailFromProtoDetail(protoCotEvent.getDetail(), customBytesExtFields, substitutionValues));
            cotEvent.setPoint(new CotPoint(protoCotEvent.getLat(), protoCotEvent.getLon(), customBytesFields.hae, protoCotEvent.getCe(), protoCotEvent.getLe()));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return cotEvent;
    }

    /**
     * toByteArray
     */
    private ProtobufCotEvent.MinimalCotEvent toCotEventProtobuf(CotEvent cotEvent) throws MappingNotFoundException, UnknownDetailFieldException {
        ProtobufCotEvent.MinimalCotEvent.Builder builder = ProtobufCotEvent.MinimalCotEvent.newBuilder();

        SubstitutionValues substitutionValues = new SubstitutionValues();
        String uid = cotEvent.getUID();
        if (uid != null) {
            builder.setUid(uid);
            if (uid.startsWith(GEOCHAT_MARKER)) {
                String[] geoChatSplit = uid.split("\\.");
                substitutionValues.uidFromGeoChat = geoChatSplit[1];
                substitutionValues.chatroomFromGeoChat = geoChatSplit[2];
            }
        }

        if (cotEvent.getType() != null) {
            builder.setType(cotEvent.getType());
        }

        CotPoint cotPoint = cotEvent.getCotPoint();
        if (cotPoint != null) {
            builder.setLat(cotPoint.getLat());
            builder.setLon(cotPoint.getLon());
            builder.setCe((int)cotPoint.getCe());
            builder.setLe((int)cotPoint.getLe());
        }

        double hae = cotPoint != null ? cotPoint.getHae() : 0;
        builder.setCustomBytes(mCustomBytesConverter.packCustomBytes(cotEvent.getTime(), cotEvent.getStale(), hae, mStartOfYearMs));

        CustomBytesExtFields customBytesExtFields = new CustomBytesExtFields(cotEvent.getHow(), null, null, null, null, null, null, null, null);

        CotDetail cotDetail = cotEvent.getDetail();

        mPrecisionLocationProtobufConverter.maybeGetPrecisionLocationValues(cotDetail, customBytesExtFields);
        mUnderscoreGroupProtobufConverter.maybeGetRoleValue(cotDetail, customBytesExtFields);
        mStatusProtobufConverter.maybeGetStatusValues(cotDetail, customBytesExtFields);
        mLabelsOnProtobufConverter.maybeGetLabelsOnValue(cotDetail, customBytesExtFields);
        mHeightAndHeightUnitProtobufConverter.maybeGetHeightUnitValues(cotDetail, customBytesExtFields);
        mHeightAndHeightUnitProtobufConverter.maybeGetHeightValues(cotDetail, customBytesExtFields);

        builder.setCustomBytesExt(mCustomBytesExtConverter.packCustomBytesExt(customBytesExtFields));

        builder.setDetail(toDetail(cotDetail, substitutionValues));

        return builder.build();
    }

    private ProtobufDetail.MinimalDetail toDetail(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufDetail.MinimalDetail.Builder builder = ProtobufDetail.MinimalDetail.newBuilder();
        for (CotDetail innerDetail : cotDetail.getChildren()) {
            switch (innerDetail.getElementName()) {
                case KEY_CONTACT:
                    builder.setContact(mContactProtobufConverter.toContact(innerDetail));
                    break;
                case KEY_UNDERSCORED_GROUP:
                    builder.setGroup(mUnderscoreGroupProtobufConverter.toUnderscoreGroup(innerDetail, substitutionValues));
                    break;
                case KEY_TAKV:
                    builder.setTakv(mTakvProtobufConverter.toTakv(innerDetail));
                    break;
                case KEY_TRACK:
                    builder.setTrack(mTrackProtobufConverter.toTrack(innerDetail));
                    break;
                case KEY_REMARKS:
                    builder.setRemarks(mRemarksProtobufConverter.toRemarks(innerDetail, substitutionValues, mStartOfYearMs));
                    break;
                case KEY_LINK:
                    builder.setLink(mLinkProtobufConverter.toLink(innerDetail, substitutionValues, mStartOfYearMs));
                    break;
                case KEY_CHAT:
                    builder.setChat(mChatProtobufConverter.toChat(innerDetail, substitutionValues));
                    break;
                case KEY_SERVER_DESTINATION:
                    builder.setServerDestination(mServerDestinationProtobufConverter.toServerDestination(innerDetail, substitutionValues));
                    break;
                case KEY_LABELS_ON:
                    mLabelsOnProtobufConverter.toLabelsOn(innerDetail);
                    break;
                case KEY_HEIGHT_UNIT:
                    mHeightAndHeightUnitProtobufConverter.toHeightUnit(innerDetail);
                    break;
                case KEY_HEIGHT:
                    mHeightAndHeightUnitProtobufConverter.toHeight(innerDetail);
                    break;
                case KEY_MODEL:
                    builder.setModel(mModelProtobufConverter.toModel(innerDetail));
                    break;
                case KEY_USER_ICON:
                    builder.setIconSetPath(innerDetail.getAttribute(KEY_ICON_SET_PATH));
                    break;
                case KEY_COLOR:
                    mDroppedFieldConverter.toColor(innerDetail);
                    break;
                case KEY_ARCHIVE:
                    mDroppedFieldConverter.toArchive(innerDetail);
                    break;
                case KEY_STATUS:
                    mStatusProtobufConverter.toStatus(innerDetail);
                    break;
                case KEY_UID:
                    mDroppedFieldConverter.toUid(innerDetail);
                    break;
                case KEY_PRECISIONLOCATION:
                    mPrecisionLocationProtobufConverter.toPrecisionLocation(innerDetail);
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail subobject: " + innerDetail.getElementName());
            }
        }
        return builder.build();
    }

    /**
     * toCotEvent
     */

    private CotDetail cotDetailFromProtoDetail(ProtobufDetail.MinimalDetail detail, CustomBytesExtFields customBytesExtFields, SubstitutionValues substitutionValues) {
        CotDetail cotDetail = new CotDetail();

        mTakvProtobufConverter.maybeAddTakv(cotDetail, detail.getTakv());
        mContactProtobufConverter.maybeAddContact(cotDetail, detail.getContact());
        mPrecisionLocationProtobufConverter.maybeAddPrecisionLocation(cotDetail, customBytesExtFields);
        mUnderscoreGroupProtobufConverter.maybeAddUnderscoreGroup(cotDetail, detail.getGroup(), customBytesExtFields);
        mStatusProtobufConverter.maybeAddStatus(cotDetail, customBytesExtFields);
        mTrackProtobufConverter.maybeAddTrack(cotDetail, detail.getTrack());
        mRemarksProtobufConverter.maybeAddRemarks(cotDetail, detail.getRemarks(), substitutionValues, mStartOfYearMs);
        mLinkProtobufConverter.maybeAddLink(cotDetail, detail.getLink(), substitutionValues, mStartOfYearMs);
        mChatProtobufConverter.maybeAddChat(cotDetail, detail.getChat(), substitutionValues);
        mServerDestinationProtobufConverter.maybeAddServerDestination(cotDetail, detail.getServerDestination(), substitutionValues);
        mModelProtobufConverter.maybeAddModel(cotDetail, detail.getModel());
        mLabelsOnProtobufConverter.maybeAddLabelsOn(cotDetail, customBytesExtFields);
        mHeightAndHeightUnitProtobufConverter.maybeAddHeightAndHeightUnit(cotDetail, customBytesExtFields);

        String iconSetPath = detail.getIconSetPath();
        if (!StringUtils.isNullOrEmpty(iconSetPath)) {
            CotDetail userIconDetail = new CotDetail(KEY_USER_ICON);

            userIconDetail.setAttribute(KEY_ICON_SET_PATH, iconSetPath);

            cotDetail.addChild(userIconDetail);
        }

        return cotDetail;
    }
}
