package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufGroup;
import com.paulmandal.atak.forwarder.protobufs.ProtobufGroupContact;

import java.util.List;

import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.CHATROOM_SUBSTITUTION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.GROUPS_SUBSTITUION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.ID_SUBSTITUTION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.USER_GROUPS_SUBSTITUTION_MARKER;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.VALUE_GROUPS;
import static com.paulmandal.atak.forwarder.comm.protobuf.SubstitutionValues.VALUE_USER_GROUPS;

public class GroupProtobufConverter {
    private static final String KEY_GROUP = "group";

    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";
    private static final String KEY_CONTACT = "contact";

    private final GroupContactProtobufConverter mGroupContactProtobufConverter;

    public GroupProtobufConverter(GroupContactProtobufConverter groupContactProtobufConverter) {
        mGroupContactProtobufConverter = groupContactProtobufConverter;
    }

    public ProtobufGroup.Group toGroup(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufGroup.Group.Builder builder = ProtobufGroup.Group.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_NAME:
                    String name = attribute.getValue();
                    if (name.equals(substitutionValues.chatroomFromGeoChat)) {
                        name = CHATROOM_SUBSTITUTION_MARKER;
                    } else if (name.equals(VALUE_GROUPS)) {
                        name = GROUPS_SUBSTITUION_MARKER;
                    }
                    builder.setName(name);
                    break;
                case KEY_UID:
                    String uid = attribute.getValue();
                    if (uid.equals(substitutionValues.idFromChat)) {
                        uid = ID_SUBSTITUTION_MARKER;
                    } else if (uid.equals(VALUE_USER_GROUPS)) {
                        uid = USER_GROUPS_SUBSTITUTION_MARKER;
                    }
                    builder.setUid(uid);
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: group." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_GROUP:
                    builder.setGroup(toGroup(child, substitutionValues));
                    break;
                case KEY_CONTACT:
                    builder.addContact(mGroupContactProtobufConverter.toGroupContact(child, substitutionValues));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child detail object: group." + child.getElementName());
            }
        }
        return builder.build();
    }

    public void maybeAddGroup(CotDetail cotDetail, ProtobufGroup.Group group, SubstitutionValues substitutionValues) {
        if (group == null || group == ProtobufGroup.Group.getDefaultInstance()) {
            return;
        }

        CotDetail groupDetail = new CotDetail(KEY_GROUP);

        String name = group.getName();
        if (!StringUtils.isNullOrEmpty(name)) {
            if (name.equals(CHATROOM_SUBSTITUTION_MARKER)) {
                name = substitutionValues.chatroomFromGeoChat;
            } else if (name.equals(GROUPS_SUBSTITUION_MARKER)) {
                name = VALUE_GROUPS;
            }
            groupDetail.setAttribute(KEY_NAME, name);
        }

        String uid = group.getUid();
        if (!StringUtils.isNullOrEmpty(uid)) {
            if (uid.equals(ID_SUBSTITUTION_MARKER)) {
                uid = substitutionValues.idFromChat;
            } else if (uid.equals(USER_GROUPS_SUBSTITUTION_MARKER)) {
                uid = VALUE_USER_GROUPS;
            }
            groupDetail.setAttribute(KEY_UID, uid);
        }


        List<ProtobufGroupContact.GroupContact> contacts = group.getContactList();
        for (ProtobufGroupContact.GroupContact contact : contacts) {
            mGroupContactProtobufConverter.maybeAddGroupContact(groupDetail, contact, substitutionValues);
        }

        ProtobufGroup.Group nestedGroup = group.getGroup();
        if (nestedGroup != null && nestedGroup != ProtobufGroup.Group.getDefaultInstance()) {
            maybeAddGroup(groupDetail, nestedGroup, substitutionValues);
        }

        cotDetail.addChild(groupDetail);
    }
}
