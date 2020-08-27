package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufContact;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ContactProtobufConverter {
    private static final String KEY_CONTACT = "contact";

    private static final String KEY_CALLSIGN = "callsign";
    private static final String KEY_ENDPOINT = "endpoint";

    private static final String FAKE_ENDPOINT_ADDRESS = "10.254.254.254:4242:tcp";
    private static final String DEFAULT_CHAT_PORT_AND_PROTO = ":4242:tcp";

    public ProtobufContact.MinimalContact toContact(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufContact.MinimalContact.Builder builder = ProtobufContact.MinimalContact.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_CALLSIGN:
                    builder.setCallsign(attribute.getValue());
                    break;
                case KEY_ENDPOINT:
                    try {
                        String[] connectionStrSplit = attribute.getValue().split(":");
                        byte[] endpointAddrAsBytes = InetAddress.getByName(connectionStrSplit[0]).getAddress();
                        int endpointAddr = new BigInteger(endpointAddrAsBytes).intValue();
                        builder.setEndpointAddr(endpointAddr);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: contact." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddContact(CotDetail cotDetail, ProtobufContact.MinimalContact contact) {
        if (contact != null && contact != ProtobufContact.MinimalContact.getDefaultInstance()) {
            CotDetail contactDetail = new CotDetail(KEY_CONTACT);

            if (!StringUtils.isNullOrEmpty(contact.getCallsign())) {
                contactDetail.setAttribute(KEY_CALLSIGN, contact.getCallsign());
            }

            if (contact.getEndpointAddr() != 0) {
                try {
                    byte[] endpointAddrAsBytes = BigInteger.valueOf(contact.getEndpointAddr()).toByteArray();
                    String ipAddress = InetAddress.getByAddress(endpointAddrAsBytes).getHostAddress();
                    contactDetail.setAttribute(KEY_ENDPOINT, ipAddress + DEFAULT_CHAT_PORT_AND_PROTO);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
                contactDetail.setAttribute(KEY_ENDPOINT, FAKE_ENDPOINT_ADDRESS);
            }

            cotDetail.addChild(contactDetail);
        }
    }
}
