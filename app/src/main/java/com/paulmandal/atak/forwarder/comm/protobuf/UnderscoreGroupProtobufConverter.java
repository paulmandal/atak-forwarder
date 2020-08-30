package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufUnderscoreGroup;

public class UnderscoreGroupProtobufConverter {
    private static final String KEY_UNDERSCORED_GROUP = "__group";

    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    public void maybeGetRoleValue(CotDetail cotDetail, CustomBytesExtFields customBytesExtFields) {
        CotDetail group = cotDetail.getFirstChildByName(0, KEY_UNDERSCORED_GROUP);
        if (group != null) {
            customBytesExtFields.role = group.getAttribute(KEY_ROLE);
        }
    }

    public ProtobufUnderscoreGroup.UnderscoreGroup toUnderscoreGroup(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufUnderscoreGroup.UnderscoreGroup.Builder builder = ProtobufUnderscoreGroup.UnderscoreGroup.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_NAME:
                    builder.setName(attribute.getValue());
                    break;
                case KEY_ROLE:
                    // Do nothing, we pack this field into bits
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: __group." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddUnderscoreGroup(CotDetail cotDetail, ProtobufUnderscoreGroup.UnderscoreGroup group, CustomBytesExtFields customBytesExtFields) {
        if (group == null || group == ProtobufUnderscoreGroup.UnderscoreGroup.getDefaultInstance()) {
            return;
        }

        CotDetail groupDetail = new CotDetail(KEY_UNDERSCORED_GROUP);

        if (!StringUtils.isNullOrEmpty(group.getName())) {
            groupDetail.setAttribute(KEY_NAME, group.getName());
        }
        if (!StringUtils.isNullOrEmpty(customBytesExtFields.role)) {
            groupDetail.setAttribute(KEY_ROLE, customBytesExtFields.role);
        }

        cotDetail.addChild(groupDetail);
    }
}
