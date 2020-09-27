package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufChat;

import java.util.List;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.CHATROOM_SUBSTITUTION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.USER_GROUPS_SUBSTITUTION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.VALUE_USER_GROUPS;

public class ChatProtobufConverter {
    private static final String KEY_CHAT = "__chat";
    private static final String KEY_CHAT_GROUP = "chatgrp";
    private static final String KEY_HIERARCHY = "hierarchy";

    private static final String KEY_PARENT = "parent";
    private static final String KEY_GROUP_OWNER = "groupOwner";
    private static final String KEY_CHATROOM = "chatroom";
    private static final String KEY_ID = "id";
    private static final String KEY_SENDER_CALLSIGN = "senderCallsign";

    private static final String VALUE_TRUE = "true";

    private final ChatGroupProtobufConverter mChatGroupProtobufConverter;
    private final HierarchyProtobufConverter mHierarchyProtobufConverter;

    public ChatProtobufConverter(ChatGroupProtobufConverter chatGroupProtobufConverter,
                                 HierarchyProtobufConverter hierarchyProtobufConverter) {
        mChatGroupProtobufConverter = chatGroupProtobufConverter;
        mHierarchyProtobufConverter = hierarchyProtobufConverter;
    }

    public ProtobufChat.Chat toChat(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufChat.Chat.Builder builder = ProtobufChat.Chat.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_PARENT:
                    String parent = attribute.getValue();
                    if (parent.equals(VALUE_USER_GROUPS)) {
                        parent = USER_GROUPS_SUBSTITUTION_MARKER;
                    }
                    builder.setParent(parent);
                    break;
                case KEY_GROUP_OWNER:
                    builder.setGroupOwner(1 << 1 | (attribute.getValue().equals(VALUE_TRUE) ? 1 : 0));
                    break;
                case KEY_CHATROOM:
                    String chatroom = attribute.getValue();
                    if (chatroom.equals(substitutionValues.chatroomFromGeoChat)) {
                        chatroom = CHATROOM_SUBSTITUTION_MARKER;
                    }
                    builder.setChatroom(chatroom);
                    break;
                case KEY_ID:
                    substitutionValues.idFromChat = attribute.getValue();
                    builder.setId(substitutionValues.idFromChat);
                    break;
                case KEY_SENDER_CALLSIGN:
                    substitutionValues.senderCallsignFromChat = attribute.getValue();
                    builder.setSenderCallsign(substitutionValues.senderCallsignFromChat);
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: chat." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail childDetail : children) {
            switch (childDetail.getElementName()) {
                case KEY_CHAT_GROUP:
                    builder.setChatGroup(mChatGroupProtobufConverter.toChatGroup(childDetail, substitutionValues));
                    break;
                case KEY_HIERARCHY:
                    builder.setHierarchy(mHierarchyProtobufConverter.toHierarchy(childDetail, substitutionValues));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: chat." + childDetail.getElementName());
            }
        }
        return builder.build();
    }


    public void maybeAddChat(CotDetail cotDetail, ProtobufChat.Chat chat, SubstitutionValues substitutionValues) {
        if (chat == null || chat == ProtobufChat.Chat.getDefaultInstance()) {
            return;
        }

        CotDetail chatDetail = new CotDetail(KEY_CHAT);

        String parent = chat.getParent();
        if (!StringUtils.isNullOrEmpty(parent)) {
            if (parent.equals(USER_GROUPS_SUBSTITUTION_MARKER)) {
                parent = VALUE_USER_GROUPS;
            }
            chatDetail.setAttribute(KEY_PARENT, parent);
        }
        if (chat.getGroupOwner() > 0) {
            chatDetail.setAttribute(KEY_GROUP_OWNER, Boolean.toString((chat.getGroupOwner() & BitUtils.createBitmask(1)) == 1));
        }
        String chatroom = chat.getChatroom();
        if (!StringUtils.isNullOrEmpty(chatroom)) {
            if (chatroom.equals(CHATROOM_SUBSTITUTION_MARKER)) {
                chatroom = substitutionValues.chatroomFromGeoChat;
            }
            chatDetail.setAttribute(KEY_CHATROOM, chatroom);
        }
        substitutionValues.idFromChat = chat.getId();
        if (!StringUtils.isNullOrEmpty(substitutionValues.idFromChat)) {
            chatDetail.setAttribute(KEY_ID, substitutionValues.idFromChat);
        }
        substitutionValues.senderCallsignFromChat = chat.getSenderCallsign();
        if (!StringUtils.isNullOrEmpty(substitutionValues.senderCallsignFromChat)) {
            chatDetail.setAttribute(KEY_SENDER_CALLSIGN, substitutionValues.senderCallsignFromChat);
        }

        mChatGroupProtobufConverter.maybeAddChatGroup(chatDetail, chat.getChatGroup(), substitutionValues);
        mHierarchyProtobufConverter.maybeAddHierarchy(chatDetail, chat.getHierarchy(), substitutionValues);

        cotDetail.addChild(chatDetail);
    }
}
