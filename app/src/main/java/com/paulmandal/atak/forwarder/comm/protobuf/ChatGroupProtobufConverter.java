package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufChatGroup;

import java.util.List;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.ID_SUBSTITUTION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.UID_SUBSTITUTION_MARKER;

public class ChatGroupProtobufConverter {
    private static final String KEY_CHAT_GROUP = "chatgrp";

    private static final String KEY_ID = "id";
    private static final String KEY_UID = "uid";

    public ProtobufChatGroup.ChatGroup toChatGroup(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufChatGroup.ChatGroup.Builder builder = ProtobufChatGroup.ChatGroup.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_ID:
                    String id = attribute.getValue();
                    if (id.equals(substitutionValues.idFromChat)) {
                        id = ID_SUBSTITUTION_MARKER;
                    }
                    builder.setId(id);
                    break;
                default:
                    if (attribute.getName().startsWith(KEY_UID)) {
                        String uid = attribute.getValue();
                        if (uid.equals(substitutionValues.uidFromGeoChat)) {
                            uid = UID_SUBSTITUTION_MARKER;
                        } else {
                            substitutionValues.uidsFromChatGroup.add(uid);
                        }
                        builder.addUid(uid);
                        break;
                    }
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: chat.chatgrp." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddChatGroup(CotDetail cotDetail, ProtobufChatGroup.ChatGroup chatGroup, SubstitutionValues substitutionValues) {
        if (chatGroup == null || chatGroup == ProtobufChatGroup.ChatGroup.getDefaultInstance()) {
            return;
        }

        CotDetail chatGroupDetail = new CotDetail(KEY_CHAT_GROUP);

        String id = chatGroup.getId();
        if (!StringUtils.isNullOrEmpty(id)) {
            if (id.equals(ID_SUBSTITUTION_MARKER)) {
                id = substitutionValues.idFromChat;
            }
            chatGroupDetail.setAttribute(KEY_ID, id);
        }
        List<String> uidList = chatGroup.getUidList();
        for (int i = 0; i < uidList.size(); i++) {
            String uid = uidList.get(i);
            if (uid.equals(UID_SUBSTITUTION_MARKER)) {
                uid = substitutionValues.uidFromGeoChat;
            } else {
                substitutionValues.uidsFromChatGroup.add(uid);
            }
            chatGroupDetail.setAttribute(KEY_UID + i, uid);
        }

        cotDetail.addChild(chatGroupDetail);
    }
}
