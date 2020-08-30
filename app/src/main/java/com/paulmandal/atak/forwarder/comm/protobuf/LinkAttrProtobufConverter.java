package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufLinkAttr;
import com.paulmandal.atak.forwarder.protobufs.ProtobufRoute;

public class LinkAttrProtobufConverter {
    private static final String KEY_LINK_ATTR = "link_attr";

    private static final String KEY_COLOR = "color";
    private static final String KEY_PREFIX = "prefix";
    private static final String KEY_PLANNING_METHOD = "planningmethod";
    private static final String KEY_METHOD = "method";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ROUTE_TYPE = "routetype";
    private static final String KEY_ORDER = "order";
    private static final String KEY_STROKE = "stroke";
    private static final String KEY_DIRECTION = "direction";

    public void maybeGetLinkAttrValues(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail linkAttr = cotDetail.getFirstChildByName(0, KEY_LINK_ATTR);
        if (linkAttr != null) {
            customBytesExtFields.routePlanningMethod = linkAttr.getAttribute(KEY_PLANNING_METHOD);
            customBytesExtFields.routeMethod = linkAttr.getAttribute(KEY_METHOD);
            customBytesExtFields.routeType = linkAttr.getAttribute(KEY_TYPE);
            customBytesExtFields.routeRouteType = linkAttr.getAttribute(KEY_ROUTE_TYPE);
            customBytesExtFields.routeOrder = linkAttr.getAttribute(KEY_ORDER);
            customBytesExtFields.routeStroke = Integer.parseInt(linkAttr.getAttribute(KEY_STROKE));
        }
    }

    public void toLinkAttr(CotDetail cotDetail, ProtobufRoute.Route.Builder routeBuilder) throws UnknownDetailFieldException {
        ProtobufLinkAttr.LinkAttr.Builder builder = ProtobufLinkAttr.LinkAttr.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_COLOR:
                    builder.setColor(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_PREFIX:
                    builder.setPrefix(attribute.getValue());
                    break;
                case KEY_PLANNING_METHOD:
                case KEY_METHOD:
                case KEY_TYPE:
                case KEY_ROUTE_TYPE:
                case KEY_ORDER:
                case KEY_STROKE:
                case KEY_DIRECTION:
                    // Do nothing, we are packing these fields into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: link_attr." + attribute.getName());
            }
        }

        if (!cotDetail.getAttribute(KEY_DIRECTION).equals(cotDetail.getAttribute(KEY_PLANNING_METHOD))) {
            throw new UnknownDetailFieldException("link_attr.direction did not match link_attr.planning_method!");
        }

        routeBuilder.setLinkAttr(builder);
    }

    public void maybeAddLinkAttr(CotDetail cotDetail, ProtobufRoute.Route route, CustomBytesExtFields customBytesExtFields) {
        if (route == null || route == ProtobufRoute.Route.getDefaultInstance()) {
            return;
        }

        ProtobufLinkAttr.LinkAttr linkAttr = route.getLinkAttr();
        if (linkAttr == null || linkAttr == ProtobufLinkAttr.LinkAttr.getDefaultInstance()) {
            return;
        }

        CotDetail linkAttrDetail = new CotDetail(KEY_LINK_ATTR);

        if (!StringUtils.isNullOrEmpty(linkAttr.getPrefix())) {
            linkAttrDetail.setAttribute(KEY_PREFIX, linkAttr.getPrefix());
        }
        if (linkAttr.getColor() != 0) {
            linkAttrDetail.setAttribute(KEY_COLOR, Integer.toString(linkAttr.getColor()));
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.routePlanningMethod)) {
            linkAttrDetail.setAttribute(KEY_PLANNING_METHOD, customBytesExtFields.routePlanningMethod);
            linkAttrDetail.setAttribute(KEY_DIRECTION, customBytesExtFields.routePlanningMethod);
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.routeMethod)) {
            linkAttrDetail.setAttribute(KEY_METHOD, customBytesExtFields.routeMethod);
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.routeType)) {
            linkAttrDetail.setAttribute(KEY_TYPE, customBytesExtFields.routeType);
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.routeRouteType)) {
            linkAttrDetail.setAttribute(KEY_ROUTE_TYPE, customBytesExtFields.routeRouteType);
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.routeOrder)) {
            linkAttrDetail.setAttribute(KEY_ORDER, customBytesExtFields.routeOrder);
        }
        if (customBytesExtFields.routeStroke != null) {
            linkAttrDetail.setAttribute(KEY_STROKE, Integer.toString(customBytesExtFields.routeStroke));
        }

        cotDetail.addChild(linkAttrDetail);
    }
}
