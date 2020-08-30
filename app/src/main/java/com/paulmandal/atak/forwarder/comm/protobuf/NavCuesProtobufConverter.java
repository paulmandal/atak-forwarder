package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufNavCue;
import com.paulmandal.atak.forwarder.protobufs.ProtobufNavCues;

import java.util.List;

public class NavCuesProtobufConverter {
    private static final String KEY_NAV_CUES = "__navcues";

    private static final String KEY_NAV_CUE = "__navcue";

    private NavCueProtobufConverter mNavCueProtobufConverter;

    public NavCuesProtobufConverter(NavCueProtobufConverter navCueProtobufConverter) {
        mNavCueProtobufConverter = navCueProtobufConverter;
    }

    public ProtobufNavCues.NavCues toNavCues(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufNavCues.NavCues.Builder builder = ProtobufNavCues.NavCues.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: __navcues." + attribute.getName());
            }
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_NAV_CUE:
                    builder.addNavCues(mNavCueProtobufConverter.toNavCue(child, substitutionValues));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child detail object: __navcues." + child.getElementName());
            }
        }
        return builder.build();
    }

    public void maybeAddNavCues(CotDetail cotDetail, ProtobufNavCues.NavCues navCues, SubstitutionValues substitutionValues) {
        if (navCues == null || navCues == ProtobufNavCues.NavCues.getDefaultInstance()) {
            return;
        }

        CotDetail navCuesDetail = new CotDetail(KEY_NAV_CUES);

        List<ProtobufNavCue.NavCue> navCuesList = navCues.getNavCuesList();
        for (ProtobufNavCue.NavCue navCue : navCuesList) {
            mNavCueProtobufConverter.maybeAddNavCue(navCuesDetail, navCue, substitutionValues);
        }

        cotDetail.addChild(navCuesDetail);
    }
}
