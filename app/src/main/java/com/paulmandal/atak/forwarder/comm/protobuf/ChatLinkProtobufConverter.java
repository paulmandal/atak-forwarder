package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufChatLink;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.UID_SUBSTITUTION_MARKER;

public class ChatLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_UID = "uid";
    private static final String KEY_TYPE = "type";
    private static final String KEY_RELATION = "relation";

    public ProtobufChatLink.ChatLink toChatLink(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufChatLink.ChatLink.Builder builder = ProtobufChatLink.ChatLink.newBuilder();
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
                case KEY_RELATION:
                    builder.setRelation(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: link[chat]." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddChatLink(CotDetail cotDetail, ProtobufChatLink.ChatLink chatLink, SubstitutionValues substitutionValues) {
        if (chatLink == null || chatLink == ProtobufChatLink.ChatLink.getDefaultInstance()) {
            return;
        }

        CotDetail linkDetail = new CotDetail(KEY_LINK);

        String uid = chatLink.getUid();
        if (!StringUtils.isNullOrEmpty(uid)) {
            if (uid.equals(UID_SUBSTITUTION_MARKER)) {
                uid = substitutionValues.uidFromGeoChat;
            }
            linkDetail.setAttribute(KEY_UID, uid);
        }
        if (!StringUtils.isNullOrEmpty(chatLink.getType())) {
            linkDetail.setAttribute(KEY_TYPE, chatLink.getType());
        }
        if (!StringUtils.isNullOrEmpty(chatLink.getRelation())) {
            linkDetail.setAttribute(KEY_RELATION, chatLink.getRelation());
        }

        cotDetail.addChild(linkDetail);
    }
}
