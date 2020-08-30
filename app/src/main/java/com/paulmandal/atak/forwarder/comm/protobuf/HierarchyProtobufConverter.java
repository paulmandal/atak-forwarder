package com.paulmandal.atak.forwarder.comm.protobuf;

import android.util.Log;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufGroup;
import com.paulmandal.atak.forwarder.protobufs.ProtobufHierarchy;

import java.util.List;

public class HierarchyProtobufConverter {
    private static final String KEY_HIERARCHY = "hierarchy";
    private static final String KEY_GROUP = "group";

    private final GroupProtobufConverter mGroupProtobufConverter;

    public HierarchyProtobufConverter(GroupProtobufConverter groupProtobufConverter) {
        mGroupProtobufConverter = groupProtobufConverter;
    }

    public ProtobufHierarchy.MinimalHierarchy toHierarchy(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufHierarchy.MinimalHierarchy.Builder builder = ProtobufHierarchy.MinimalHierarchy.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child attribute: chat.hierarchy." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_GROUP:
                    Log.d("HIERDBG", "adding group: " + child.getAttribute("uid"));
                    builder.setGroup(mGroupProtobufConverter.toGroup(child, substitutionValues));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child object: chat.hierarchy." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddHierarchy(CotDetail cotDetail, ProtobufHierarchy.MinimalHierarchy hierarchy, SubstitutionValues substitutionValues) {
        if (hierarchy != null && hierarchy != ProtobufHierarchy.MinimalHierarchy.getDefaultInstance()) {
            CotDetail hierarchyDetail = new CotDetail(KEY_HIERARCHY);

            ProtobufGroup.MinimalGroup hierarchyGroup = hierarchy.getGroup();
            mGroupProtobufConverter.maybeAddGroup(hierarchyDetail, hierarchyGroup.getGroup(), substitutionValues);

            cotDetail.addChild(hierarchyDetail);
        }
    }
}
