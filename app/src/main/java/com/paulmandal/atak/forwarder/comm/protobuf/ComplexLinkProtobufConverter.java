package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.forwarder.protobufs.ProtobufComplexLink;

import java.text.ParseException;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.UID_SUBSTITUTION_MARKER;

public class ComplexLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_UID = "uid";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PARENT_CALLSIGN = "parent_callsign";
    private static final String KEY_RELATION = "relation";
    private static final String KEY_PRODUCTION_TIME = "production_time";

    public ProtobufComplexLink.ComplexLink toComplexLink(CotDetail cotDetail, SubstitutionValues substitutionValues, long startOfYearMs) throws UnknownDetailFieldException {
        ProtobufComplexLink.ComplexLink.Builder builder = ProtobufComplexLink.ComplexLink.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UID:
                    String uid = attribute.getValue();
                    if (uid.equals(substitutionValues.uidFromGeoChat)) {
                        uid = UID_SUBSTITUTION_MARKER;
                    }
                    builder.setUid(uid);
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
                        long sinceStartOfYear = (productionTime - startOfYearMs) / 1000;
                        builder.setProductionTime((int) sinceStartOfYear);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: link[complex]." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddComplexLink(CotDetail cotDetail, ProtobufComplexLink.ComplexLink link, SubstitutionValues substitutionValues, long startOfYearMs) {
        if (link == null || link == ProtobufComplexLink.ComplexLink.getDefaultInstance()) {
            return;
        }

        CotDetail linkDetail = new CotDetail(KEY_LINK);

        String uid = link.getUid();
        if (!StringUtils.isNullOrEmpty(uid)) {
            if (uid.equals(UID_SUBSTITUTION_MARKER)) {
                uid = substitutionValues.uidFromGeoChat;
            }
            linkDetail.setAttribute(KEY_UID, uid);
        }
        if (!StringUtils.isNullOrEmpty(link.getType())) {
            linkDetail.setAttribute(KEY_TYPE, link.getType());
        }
        if (!StringUtils.isNullOrEmpty(link.getParentCallsign())) {
            linkDetail.setAttribute(KEY_PARENT_CALLSIGN, link.getParentCallsign());
        }
        if (!StringUtils.isNullOrEmpty(link.getRelation())) {
            linkDetail.setAttribute(KEY_RELATION, link.getRelation());
        }
        long productionTimeOffset = link.getProductionTime();
        if (productionTimeOffset > 0) {
            long productionTimeMs = startOfYearMs + (productionTimeOffset * 1000);
            CoordinatedTime productionTime = new CoordinatedTime(productionTimeMs);
            linkDetail.setAttribute(KEY_PRODUCTION_TIME, productionTime.toString());
        }

        cotDetail.addChild(linkDetail);
    }
}
