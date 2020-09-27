package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufServerDestination;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.UID_SUBSTITUTION_MARKER;

public class ServerDestinationProtobufConverter {
    private static final String KEY_SERVER_DESTINATION = "__serverdestination";

    private static final String KEY_DESTINATIONS = "destinations";

    public ProtobufServerDestination.ServerDestination toServerDestination(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufServerDestination.ServerDestination.Builder builder = ProtobufServerDestination.ServerDestination.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_DESTINATIONS:
                    String destinations = attribute.getValue();
                    if (destinations.contains(substitutionValues.uidFromGeoChat)) {
                        destinations = destinations.replace(substitutionValues.uidFromGeoChat, UID_SUBSTITUTION_MARKER);
                    }
                    builder.setDestinations(destinations);
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: serverdestination." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddServerDestination(CotDetail cotDetail, ProtobufServerDestination.ServerDestination serverDestination, SubstitutionValues substitutionValues) {
        if (serverDestination == null || serverDestination == ProtobufServerDestination.ServerDestination.getDefaultInstance()) {
            return;
        }

        CotDetail serverDestinationDetail = new CotDetail(KEY_SERVER_DESTINATION);

        String destinations = serverDestination.getDestinations();
        if (!StringUtils.isNullOrEmpty(destinations)) {
            if (destinations.contains(UID_SUBSTITUTION_MARKER)) {
                destinations = destinations.replace(UID_SUBSTITUTION_MARKER, substitutionValues.uidFromGeoChat);
            }
            serverDestinationDetail.setAttribute(KEY_DESTINATIONS, destinations);
        }

        cotDetail.addChild(serverDestinationDetail);
    }
}

