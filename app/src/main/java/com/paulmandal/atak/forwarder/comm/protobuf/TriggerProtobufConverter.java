package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTrigger;

public class TriggerProtobufConverter {
    private static String KEY_TRIGGER = "trigger";

    private static final String KEY_MODE = "mode";
    private static final String KEY_VALUE = "value";

    public ProtobufTrigger.Trigger toTrigger(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufTrigger.Trigger.Builder builder = ProtobufTrigger.Trigger.newBuilder();

        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_MODE:
                    builder.setMode(attribute.getValue());
                    break;
                case KEY_VALUE:
                    builder.setValue(Integer.parseInt(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: trigger." + attribute.getName());
            }
        }

        return builder.build();
    }

    public void maybeAddTrigger(CotDetail cotDetail, ProtobufTrigger.Trigger trigger) {
        if (trigger == null || trigger == ProtobufTrigger.Trigger.getDefaultInstance()) {
            return;
        }

        CotDetail triggerDetail = new CotDetail(KEY_TRIGGER);

        if (!StringUtils.isNullOrEmpty(trigger.getMode())) {
            triggerDetail.setAttribute(KEY_MODE, trigger.getMode());
        }
        if (trigger.getValue() != 0) {
            triggerDetail.setAttribute(KEY_VALUE, Integer.toString(trigger.getValue()));
        }

        cotDetail.addChild(triggerDetail);
    }

}
