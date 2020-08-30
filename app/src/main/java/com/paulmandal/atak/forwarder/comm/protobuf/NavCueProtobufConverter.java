package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufNavCue;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTrigger;

import java.util.List;

public class NavCueProtobufConverter {
    private static final String KEY_NAV_CUE = "__navcue";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_ID = "id";
    private static final String KEY_TEXT = "text";

    private static final String KEY_TRIGGER = "trigger";

    private static final String ID_SUBSTITUTION_MARKER = "#i";

    private TriggerProtobufConverter mTriggerProtobufConverter;

    public NavCueProtobufConverter(TriggerProtobufConverter triggerProtobufConverter) {
        mTriggerProtobufConverter = triggerProtobufConverter;
    }

    public ProtobufNavCue.NavCue toNavCue(CotDetail cotDetail, SubstitutionValues substitutionValues) throws UnknownDetailFieldException {
        ProtobufNavCue.NavCue.Builder builder = ProtobufNavCue.NavCue.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_VOICE:
                    builder.setVoice(attribute.getValue());
                    break;
                case KEY_ID:
                    String id = attribute.getValue();
                    if (substitutionValues.uidsFromRoute.contains(id)) {
                        id = ID_SUBSTITUTION_MARKER + substitutionValues.uidsFromRoute.indexOf(id);
                    }
                    builder.setId(id);
                    break;
                case KEY_TEXT:
                    // Do nothing, we use the voice field for this
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: __navcue." + attribute.getName());
            }
        }

        // Validate that VOICE == TEXT
        if (!cotDetail.getAttribute(KEY_TEXT).equals(cotDetail.getAttribute(KEY_VOICE))) {
            throw new UnknownDetailFieldException("__navcue.text was not equal to __navcue.voice");
        }

        List<CotDetail> children = cotDetail.getChildren();
        for (CotDetail child : children) {
            switch (child.getElementName()) {
                case KEY_TRIGGER:
                    builder.addTrigger(mTriggerProtobufConverter.toTrigger(child));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child detail object: __navcue." + child.getElementName());
            }
        }
        return builder.build();
    }

    public void maybeAddNavCue(CotDetail cotDetail, ProtobufNavCue.NavCue navCue, SubstitutionValues substitutionValues) {
        if (navCue == null || navCue == ProtobufNavCue.NavCue.getDefaultInstance()) {
            return;
        }

        CotDetail navCueDetail = new CotDetail(KEY_NAV_CUE);

        String id = navCue.getId();
        if(!StringUtils.isNullOrEmpty(id)) {
            if (id.startsWith(ID_SUBSTITUTION_MARKER)) {
                int index = Integer.parseInt(id.replace(ID_SUBSTITUTION_MARKER, ""));
                id = substitutionValues.uidsFromRoute.get(index);
            }
            navCueDetail.setAttribute(KEY_ID, id);
        }
        if(!StringUtils.isNullOrEmpty(navCue.getVoice())) {
            navCueDetail.setAttribute(KEY_VOICE, navCue.getVoice());
            navCueDetail.setAttribute(KEY_TEXT, navCue.getVoice());
        }

        List<ProtobufTrigger.Trigger> triggers = navCue.getTriggerList();
        for (ProtobufTrigger.Trigger trigger : triggers) {
            mTriggerProtobufConverter.maybeAddTrigger(navCueDetail, trigger);
        }

        cotDetail.addChild(navCueDetail);
    }

}
