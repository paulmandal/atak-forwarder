package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufGroupContact;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.SENDER_CALLSIGN_SUBSTITUION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.UID_SUBSTITUTION_MARKER;

public class GroupContactProtobufConverter {
    private static final String KEY_CONTACT = "contact";

    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";

    public ProtobufGroupContact.GroupContact toGroupContact(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufGroupContact.GroupContact.Builder builder = ProtobufGroupContact.GroupContact.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_UID:
                    String uid = attribute.getValue();
                    if (uid.equals(substitutionValues.uidFromGeoChat)) {
                        uid = UID_SUBSTITUTION_MARKER;
                    } else if (substitutionValues.uidsFromChatGroup.contains(uid)) {
                        uid = UID_SUBSTITUTION_MARKER + substitutionValues.uidsFromChatGroup.indexOf(uid);
                    }
                    builder.setUid(uid);
                    break;
                case KEY_NAME:
                    String name = attribute.getValue();
                    if (name.equals(substitutionValues.senderCallsignFromChat)) {
                        name = SENDER_CALLSIGN_SUBSTITUION_MARKER;
                    }
                    builder.setName(name);
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: contact." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddGroupContact(CotDetail cotDetail, ProtobufGroupContact.GroupContact groupContact, SubstitutionValues substitutionValues) {
        if (groupContact == null || groupContact == ProtobufGroupContact.GroupContact.getDefaultInstance()) {
            return;
        }

        CotDetail contactDetail = new CotDetail(KEY_CONTACT);

        String name = groupContact.getName();
        if (!StringUtils.isNullOrEmpty(name)) {
            if (name.equals(SENDER_CALLSIGN_SUBSTITUION_MARKER)) {
                name = substitutionValues.senderCallsignFromChat;
            }
            contactDetail.setAttribute(KEY_NAME, name);
        }

        String uid = groupContact.getUid();
        if (!StringUtils.isNullOrEmpty(uid)) {
            if (uid.equals(UID_SUBSTITUTION_MARKER)) {
                uid = substitutionValues.uidFromGeoChat;
            } else if (uid.startsWith(UID_SUBSTITUTION_MARKER)) {
                int index = Integer.parseInt(uid.replace(UID_SUBSTITUTION_MARKER, ""));
                uid = substitutionValues.uidsFromChatGroup.get(index);
            }
            contactDetail.setAttribute(KEY_UID, uid);
        }

        cotDetail.addChild(contactDetail);
    }
}
