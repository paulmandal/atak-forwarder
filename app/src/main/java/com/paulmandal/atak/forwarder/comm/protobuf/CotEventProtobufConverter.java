package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.cotutils.CotMessageTypes;
import com.paulmandal.atak.forwarder.protobufs.ProtobufContact;
import com.paulmandal.atak.forwarder.protobufs.ProtobufCotEvent;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDetailStyle;
import com.paulmandal.atak.forwarder.protobufs.ProtobufDrawnShape;
import com.paulmandal.atak.forwarder.protobufs.ProtobufRoute;

public class CotEventProtobufConverter {
    private static final String TAG = "ATAKDBG." + CotEventProtobufConverter.class.getSimpleName();

    /**
     * CotDetail fields
     */
    private static final String KEY_CONTACT = "contact";
    private static final String KEY_UNDERSCORED_GROUP = "__group";
    private static final String KEY_PRECISIONLOCATION = "precisionlocation";
    private static final String KEY_COLOR = "color";
    private static final String KEY_STROKE_WEIGHT = "strokeWeight";
    private static final String KEY_STROKE_COLOR = "strokeColor";
    private static final String KEY_FILL_COLOR = "fillColor";
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
    private static final String KEY_CE_HUMAN_INPUT = "ce_human_input";
    private static final String KEY_HEIGHT = "height";

    // Fields
    private static final String KEY_ICON_SET_PATH = "iconsetpath";
    private static final String KEY_UID = "uid";
    private static final String KEY_DROID = "Droid";
    private static final String KEY_LINE = "line";
    private static final String KEY_PRODUCTION_TIME = "production_time";

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
    private final LabelsOnProtobufConverter mLabelsOnProtobufConverter;
    private final PrecisionLocationProtobufConverter mPrecisionLocationProtobufConverter;
    private final DroppedFieldConverter mDroppedFieldConverter;
    private final StatusProtobufConverter mStatusProtobufConverter;
    private final HeightAndHeightUnitProtobufConverter mHeightAndHeightUnitProtobufConverter;
    private final ModelProtobufConverter mModelProtobufConverter;
    private final DetailStyleProtobufConverter mDetailStyleProtobufConverter;
    private final CeHumanInputProtobufConverter mCeHumanInputProtobufConverter;
    private final FreehandLinkProtobufConverter mFreehandLinkProtobufConverter;
    private final ChatLinkProtobufConverter mChatLinkProtobufConverter;
    private final ComplexLinkProtobufConverter mComplexLinkProtobufConverter;
    private final ShapeLinkProtobufConverter mShapeLinkProtobufConverter;
    private final RouteLinkProtobufConverter mRouteLinkProtobufConverter;
    private final RouteLinkAttrProtobufConverter mRouteLinkAttrProtobufConverter;
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
                                     LabelsOnProtobufConverter labelsOnProtobufConverter,
                                     PrecisionLocationProtobufConverter precisionLocationProtobufConverter,
                                     DroppedFieldConverter droppedFieldConverter,
                                     StatusProtobufConverter statusProtobufConverter,
                                     HeightAndHeightUnitProtobufConverter heightAndHeightUnitProtobufConverter,
                                     ModelProtobufConverter modelProtobufConverter,
                                     DetailStyleProtobufConverter detailStyleProtobufConverter,
                                     CeHumanInputProtobufConverter ceHumanInputProtobufConverter,
                                     FreehandLinkProtobufConverter freehandLinkProtobufConverter,
                                     ChatLinkProtobufConverter chatLinkProtobufConverter,
                                     ComplexLinkProtobufConverter complexLinkProtobufConverter,
                                     ShapeLinkProtobufConverter shapeLinkProtobufConverter,
                                     RouteLinkProtobufConverter routeLinkProtobufConverter,
                                     RouteLinkAttrProtobufConverter routeLinkAttrProtobufConverter,
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
        mLabelsOnProtobufConverter = labelsOnProtobufConverter;
        mPrecisionLocationProtobufConverter = precisionLocationProtobufConverter;
        mDroppedFieldConverter = droppedFieldConverter;
        mStatusProtobufConverter = statusProtobufConverter;
        mHeightAndHeightUnitProtobufConverter = heightAndHeightUnitProtobufConverter;
        mModelProtobufConverter = modelProtobufConverter;
        mDetailStyleProtobufConverter = detailStyleProtobufConverter;
        mCeHumanInputProtobufConverter = ceHumanInputProtobufConverter;
        mFreehandLinkProtobufConverter = freehandLinkProtobufConverter;
        mChatLinkProtobufConverter = chatLinkProtobufConverter;
        mComplexLinkProtobufConverter = complexLinkProtobufConverter;
        mShapeLinkProtobufConverter = shapeLinkProtobufConverter;
        mRouteLinkProtobufConverter = routeLinkProtobufConverter;
        mRouteLinkAttrProtobufConverter = routeLinkAttrProtobufConverter;
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
            cotEvent.setDetail(cotDetailFromProtoDetail(cotEvent, protoCotEvent.getDetail(), customBytesExtFields, substitutionValues));
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
        mCeHumanInputProtobufConverter.maybeGetCeHumanInputValues(cotDetail, customBytesExtFields);
        mHeightAndHeightUnitProtobufConverter.maybeGetHeightUnitValues(cotDetail, customBytesExtFields);
        mHeightAndHeightUnitProtobufConverter.maybeGetHeightValues(cotDetail, customBytesExtFields);

        builder.setCustomBytesExt(mCustomBytesExtConverter.packCustomBytesExt(customBytesExtFields));

        builder.setDetail(toDetail(cotDetail, substitutionValues));

        return builder.build();
    }

    private ProtobufDetail.MinimalDetail toDetail(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufDetail.MinimalDetail.Builder builder = ProtobufDetail.MinimalDetail.newBuilder();
        ProtobufDetailStyle.MinimalDetailStyle.Builder detailStyleBuilder = null;
        ProtobufDrawnShape.MinimalDrawnShape.Builder drawnShapeBuilder = null;
        ProtobufRoute.MinimalRoute.Builder routeBuilder = null;

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
                    if (innerDetail.getAttribute(KEY_PRODUCTION_TIME) != null) {
                        builder.setComplexLink(mComplexLinkProtobufConverter.toComplexLink(innerDetail, substitutionValues, mStartOfYearMs));
                    } else if (innerDetail.getAttribute(KEY_REMARKS) != null) {
                        routeBuilder = routeBuilder != null ? routeBuilder : ProtobufRoute.MinimalRoute.newBuilder();
                        mRouteLinkProtobufConverter.toRouteLink(innerDetail, routeBuilder);
                    } else if (innerDetail.getAttribute(KEY_UID) != null) {
                        builder.setChatLink(mChatLinkProtobufConverter.toChatLink(innerDetail, substitutionValues));
                    } else if (innerDetail.getAttribute(KEY_LINE) != null) {
                        builder.setFreehandLink(mFreehandLinkProtobufConverter.toFreehandLink(innerDetail));
                    } else {
                        drawnShapeBuilder = drawnShapeBuilder != null ? drawnShapeBuilder : ProtobufDrawnShape.MinimalDrawnShape.newBuilder();
                        mShapeLinkProtobufConverter.toShapeLink(innerDetail, drawnShapeBuilder);
                    }
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
                case KEY_CE_HUMAN_INPUT:
                    mCeHumanInputProtobufConverter.toCeHumanInput(innerDetail);
                    break;
                case KEY_HEIGHT_UNIT:
                    mHeightAndHeightUnitProtobufConverter.toHeightUnit(innerDetail);
                    break;
                case KEY_HEIGHT:
                    builder.setHeight(mHeightAndHeightUnitProtobufConverter.toHeight(innerDetail));
                    break;
                case KEY_MODEL:
                    builder.setModel(mModelProtobufConverter.toModel(innerDetail));
                    break;
                case KEY_USER_ICON:
                    builder.setIconSetPath(innerDetail.getAttribute(KEY_ICON_SET_PATH));
                    break;
                case KEY_COLOR:
                    detailStyleBuilder = maybeMakeDetailStyleBuilder(detailStyleBuilder);
                    mDetailStyleProtobufConverter.toColor(innerDetail, detailStyleBuilder);
                    break;
                case KEY_STROKE_WEIGHT:
                    detailStyleBuilder = maybeMakeDetailStyleBuilder(detailStyleBuilder);
                    mDetailStyleProtobufConverter.toStrokeWeight(innerDetail, detailStyleBuilder);
                    break;
                case KEY_STROKE_COLOR:
                    detailStyleBuilder = maybeMakeDetailStyleBuilder(detailStyleBuilder);
                    mDetailStyleProtobufConverter.toStrokeColor(innerDetail, detailStyleBuilder);
                    break;
                case KEY_FILL_COLOR:
                    detailStyleBuilder = maybeMakeDetailStyleBuilder(detailStyleBuilder);
                    mDetailStyleProtobufConverter.toFillColor(innerDetail, detailStyleBuilder);
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

        if (detailStyleBuilder != null) {
            builder.setDetailStyle(detailStyleBuilder);
        }

        if (drawnShapeBuilder != null) {
            builder.setDrawnShape(drawnShapeBuilder);
        }

        if (routeBuilder != null) {
            builder.setRoute(routeBuilder);
        }

        return builder.build();
    }

    /**
     * toCotEvent
     */

    private CotDetail cotDetailFromProtoDetail(CotEvent cotEvent, ProtobufDetail.MinimalDetail detail, CustomBytesExtFields customBytesExtFields, SubstitutionValues substitutionValues) {
        CotDetail cotDetail = new CotDetail();

        mTakvProtobufConverter.maybeAddTakv(cotDetail, detail.getTakv());
        mContactProtobufConverter.maybeAddContact(cotDetail, detail.getContact(), customBytesExtFields);
        mPrecisionLocationProtobufConverter.maybeAddPrecisionLocation(cotDetail, customBytesExtFields);
        mUnderscoreGroupProtobufConverter.maybeAddUnderscoreGroup(cotDetail, detail.getGroup(), customBytesExtFields);
        mStatusProtobufConverter.maybeAddStatus(cotDetail, customBytesExtFields);
        mTrackProtobufConverter.maybeAddTrack(cotDetail, detail.getTrack());
        mRemarksProtobufConverter.maybeAddRemarks(cotDetail, detail.getRemarks(), substitutionValues, mStartOfYearMs);
        mChatLinkProtobufConverter.maybeAddChatLink(cotDetail, detail.getChatLink(), substitutionValues);
        mComplexLinkProtobufConverter.maybeAddComplexLink(cotDetail, detail.getComplexLink(), substitutionValues, mStartOfYearMs);
        mFreehandLinkProtobufConverter.maybeAddFreehandLink(cotDetail, detail.getFreehandLink());
        mRouteLinkProtobufConverter.maybeAddRoute(cotDetail, detail.getRoute());
        mShapeLinkProtobufConverter.maybeAddShape(cotDetail, detail.getShape());
        mChatProtobufConverter.maybeAddChat(cotDetail, detail.getChat(), substitutionValues);
        mServerDestinationProtobufConverter.maybeAddServerDestination(cotDetail, detail.getServerDestination(), substitutionValues);
        mModelProtobufConverter.maybeAddModel(cotDetail, detail.getModel());
        mLabelsOnProtobufConverter.maybeAddLabelsOn(cotDetail, customBytesExtFields);
        mCeHumanInputProtobufConverter.maybeAddCeHumanInput(cotDetail, customBytesExtFields);
        mHeightAndHeightUnitProtobufConverter.maybeAddHeightAndHeightUnit(cotDetail, detail.getHeight(), customBytesExtFields);
        mDetailStyleProtobufConverter.maybeAddColor(cotDetail, detail.getDetailStyle().getColor());
        mDetailStyleProtobufConverter.maybeAddStrokeWeight(cotDetail, detail.getDetailStyle());
        mDetailStyleProtobufConverter.maybeAddStrokeColor(cotDetail, detail.getDetailStyle());

        maybeAddUidDroid(cotEvent, cotDetail, detail.getContact());

        maybeAddUserIcon(cotDetail, detail.getIconSetPath());

        return cotDetail;
    }

    private void maybeAddUidDroid(CotEvent cotEvent, CotDetail cotDetail, ProtobufContact.MinimalContact contact) {
        boolean isPli = cotEvent.getType().equals(CotMessageTypes.TYPE_PLI);
        String callsign = contact.getCallsign();

        if (!StringUtils.isNullOrEmpty(callsign) && isPli) {
            CotDetail uidDetail = new CotDetail(KEY_UID);

            uidDetail.setAttribute(KEY_DROID, callsign);

            cotDetail.addChild(uidDetail);
        }
    }

    private void maybeAddUserIcon(CotDetail cotDetail, String iconSetPath) {
        if (!StringUtils.isNullOrEmpty(iconSetPath)) {
            CotDetail userIconDetail = new CotDetail(KEY_USER_ICON);

            userIconDetail.setAttribute(KEY_ICON_SET_PATH, iconSetPath);

            cotDetail.addChild(userIconDetail);
        }
    }

    private ProtobufDetailStyle.MinimalDetailStyle.Builder maybeMakeDetailStyleBuilder(ProtobufDetailStyle.MinimalDetailStyle.Builder builder) {
        return builder != null ? builder : ProtobufDetailStyle.MinimalDetailStyle.newBuilder();
    }
}
