package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufConnectionEntry;

import java.util.List;

public class ConnectionEntryProtobufConverter {
    private static final String KEY_CONNECTION_ENTRY = "ConnectionEntry";

    private static final String KEY_NETWORK_TIMEOUT = "networkTimeout";
    private static final String KEY_UID = "uid";
    private static final String KEY_PATH = "path";
    private static final String KEY_PROTOCOL = "protocol";
    private static final String KEY_BUFFER_TIME = "bufferTime";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ROVER_PORT = "roverPort";
    private static final String KEY_RTSP_RELIABLE = "rtspReliable";
    private static final String KEY_IGNORE_EMBEDDED_KLV = "ignoreEmbeddedKLV";
    private static final String KEY_ALIAS = "alias";

    private static final String VIDEO_UID_SUBSTITUTION_MARKER = "#u";

    public ProtobufConnectionEntry.ConnectionEntry toConnectionEntry(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufConnectionEntry.ConnectionEntry.Builder builder  = ProtobufConnectionEntry.ConnectionEntry.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_NETWORK_TIMEOUT:
                    builder.setNetworkTimeout(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_UID:
                    String uid = attribute.getValue();
                    if (uid.equals(substitutionValues.uidFromVideo)) {
                        uid = VIDEO_UID_SUBSTITUTION_MARKER;
                    }
                    builder.setUid(uid);
                    break;
                case KEY_PATH:
                    builder.setPath(attribute.getValue());
                    break;
                case KEY_PROTOCOL:
                    builder.setProtocol(attribute.getValue());
                    break;
                case KEY_BUFFER_TIME:
                    builder.setBufferTime(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_ADDRESS:
                    builder.setAddress(attribute.getValue());
                    break;
                case KEY_PORT:
                    builder.setPort(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_ROVER_PORT:
                    builder.setRoverPort(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_RTSP_RELIABLE:
                    builder.setRtspReliable(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_IGNORE_EMBEDDED_KLV:
                    builder.setIgnoreEmbeddedKLV(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_ALIAS:
                    builder.setAlias(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: __video.ConnectionEntry." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: __video.ConnectionEntry." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddConnectionEntry(CotDetail cotDetail, ProtobufConnectionEntry.ConnectionEntry connectionEntry, SubstitutionValues substitutionValues) {
        if (connectionEntry == null || connectionEntry == ProtobufConnectionEntry.ConnectionEntry.getDefaultInstance()) {
            return;
        }

        CotDetail connectionEntryDetail = new CotDetail(KEY_CONNECTION_ENTRY);

        connectionEntryDetail.setAttribute(KEY_NETWORK_TIMEOUT, Integer.toString(connectionEntry.getNetworkTimeout()));

        String uid = connectionEntry.getUid();
        if (uid.equals(VIDEO_UID_SUBSTITUTION_MARKER)) {
            uid = substitutionValues.uidFromVideo;
        }
        connectionEntryDetail.setAttribute(KEY_UID, uid);
        connectionEntryDetail.setAttribute(KEY_PATH, connectionEntry.getPath());
        connectionEntryDetail.setAttribute(KEY_PROTOCOL, connectionEntry.getProtocol());
        connectionEntryDetail.setAttribute(KEY_BUFFER_TIME, Integer.toString(connectionEntry.getBufferTime()));
        connectionEntryDetail.setAttribute(KEY_ADDRESS, connectionEntry.getAddress());
        connectionEntryDetail.setAttribute(KEY_PORT, Integer.toString(connectionEntry.getPort()));
        connectionEntryDetail.setAttribute(KEY_ROVER_PORT, Integer.toString(connectionEntry.getRoverPort()));
        connectionEntryDetail.setAttribute(KEY_RTSP_RELIABLE, Integer.toString(connectionEntry.getRtspReliable()));
        connectionEntryDetail.setAttribute(KEY_IGNORE_EMBEDDED_KLV, Boolean.toString(connectionEntry.getIgnoreEmbeddedKLV()));
        connectionEntryDetail.setAttribute(KEY_ALIAS, connectionEntry.getAlias());

        cotDetail.addChild(connectionEntryDetail);
    }
}

