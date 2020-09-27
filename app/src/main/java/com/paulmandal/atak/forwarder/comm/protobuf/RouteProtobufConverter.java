package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufRoute;
import com.paulmandal.atak.forwarder.protobufs.ProtobufRouteInfo;
import com.paulmandal.atak.forwarder.protobufs.ProtobufRouteLink;

import java.util.List;

public class RouteProtobufConverter {
    private static final String KEY_LINK = "link";
    private static final String KEY_ROUTE_INFO = "__routeinfo";

    private static final String KEY_UID = "uid";
    private static final String KEY_TYPE = "type";
    private static final String KEY_POINT = "point";
    private static final String KEY_RELATION = "relation";
    private static final String KEY_CALLSIGN = "callsign";
    private static final String KEY_REMARKS = "remarks";

    private static final String KEY_NAV_CUES = "__navcues";

    private static final double NULL_VALUE = -1.0;

    private NavCuesProtobufConverter mNavCuesProtobufConverter;

    public RouteProtobufConverter(NavCuesProtobufConverter navCuesProtobufConverter) {
        mNavCuesProtobufConverter = navCuesProtobufConverter;
    }

    public void toRouteLink(CotDetail cotDetail, ProtobufRoute.Route.Builder routeBuilder, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufRouteLink.RouteLink.Builder builder = ProtobufRouteLink.RouteLink.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();

        builder.setLat(NULL_VALUE);
        builder.setLon(NULL_VALUE);
        builder.setHae(NULL_VALUE);

        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UID:
                    String uid = attribute.getValue();
                    String[] uidSplit = uid.split("-");
                    uid = uidSplit[uidSplit.length - 1];
                    substitutionValues.uidsFromRoute.add(uid);
                    builder.setUid(uid);
                    break;
                case KEY_TYPE:
                    builder.setType(attribute.getValue());
                    break;
                case KEY_POINT:
                    String[] splitPoint = attribute.getValue().split(",");
                    if (splitPoint.length > 0) {
                        builder.setLat(Double.parseDouble(splitPoint[0]));
                    }
                    if (splitPoint.length > 1) {
                        builder.setLon(Double.parseDouble(splitPoint[1]));
                    }
                    if (splitPoint.length > 2) {
                        builder.setHae(Double.parseDouble(splitPoint[2]));
                    }
                    break;
                case KEY_RELATION:
                    builder.setRelation(attribute.getValue());
                    break;
                case KEY_CALLSIGN:
                    builder.setCallsign(attribute.getValue());
                    break;
                case KEY_REMARKS:
                    builder.setRemarks(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: link[route]." + attribute.getName());
            }
        }

        routeBuilder.addLink(builder);
    }

    public void toRouteInfo(CotDetail cotDetail, ProtobufRoute.Route.Builder routeBuilder, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufRouteInfo.RouteInfo.Builder builder = ProtobufRouteInfo.RouteInfo.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: __routeinfo." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_NAV_CUES:
                    builder.setNavCues(mNavCuesProtobufConverter.toNavCues(child, substitutionValues));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: __routeinfo." + child.getElementName());
            }
        }

        routeBuilder.setRouteInfo(builder);
    }

    public void maybeAddRoute(CotDetail cotDetail, ProtobufRoute.Route route, SubstitutionValues substitutionValues) {
        if (route == null || route == ProtobufRoute.Route.getDefaultInstance() || route.getLinkList() == ProtobufRoute.Route.getDefaultInstance().getLinkList()) {
            return;
        }

        for (ProtobufRouteLink.RouteLink link : route.getLinkList()) {
            CotDetail linkDetail = new CotDetail(KEY_LINK);

            String uid = link.getUid();
            if (!StringUtils.isNullOrEmpty(uid)) {
                linkDetail.setAttribute(KEY_UID, uid);
            }
            if (!StringUtils.isNullOrEmpty(link.getType())) {
                linkDetail.setAttribute(KEY_TYPE, link.getType());
            }

            String point = "";
            if (link.getLat() != NULL_VALUE) {
                point += Double.toString(link.getLat());
            }
            if (link.getLon() != NULL_VALUE) {
                point += ((point.length() > 0 ? "," : "") + link.getLon());
            }
            if (link.getHae() != NULL_VALUE) {
                point += ((point.length() > 0 ? "," : "") + link.getHae());
            }
            linkDetail.setAttribute(KEY_POINT, point);

            if (!StringUtils.isNullOrEmpty(link.getRelation())) {
                linkDetail.setAttribute(KEY_RELATION, link.getRelation());
            }
            if (!StringUtils.isNullOrEmpty(link.getCallsign())) {
                linkDetail.setAttribute(KEY_CALLSIGN, link.getCallsign());
            }
            linkDetail.setAttribute(KEY_REMARKS, link.getRemarks());

            cotDetail.addChild(linkDetail);
        }

        ProtobufRouteInfo.RouteInfo routeInfo = route.getRouteInfo();
        if (routeInfo == null || routeInfo == ProtobufRouteInfo.RouteInfo.getDefaultInstance()) {
            return;
        }

        CotDetail routeInfoDetail = new CotDetail(KEY_ROUTE_INFO);

        mNavCuesProtobufConverter.maybeAddNavCues(routeInfoDetail, routeInfo.getNavCues(), substitutionValues);

        cotDetail.addChild(routeInfoDetail);
    }
}
