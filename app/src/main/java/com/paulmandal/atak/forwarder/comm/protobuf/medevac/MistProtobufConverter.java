package com.paulmandal.atak.forwarder.comm.protobuf.medevac;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.StringUtils;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufMist;

public class MistProtobufConverter {
    private static final String KEY_MIST = "zMist";

    private static final String KEY_SYMPTOMS = "s";
    private static final String KEY_TREATMENT = "t";
    private static final String KEY_INJURY = "i";
    private static final String KEY_ZAP_NUMBER = "z";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MECHANISM_OF_INJURY = "m";

    public ProtobufMist.Mist toMist(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufMist.Mist.Builder builder = ProtobufMist.Mist.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_SYMPTOMS:
                    builder.setSymptoms(attribute.getValue());
                    break;
                case KEY_TREATMENT:
                    builder.setTreatment(attribute.getValue());
                    break;
                case KEY_INJURY:
                    builder.setInjury(attribute.getValue());
                    break;
                case KEY_ZAP_NUMBER:
                    builder.setZapNumber(attribute.getValue());
                    break;
                case KEY_TITLE:
                    builder.setTitle(attribute.getValue());
                    break;
                case KEY_MECHANISM_OF_INJURY:
                    builder.setMechanismOfInjury(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: medevac.zMistsMap.zMist." + attribute.getName());
            }
        }

        for (CotDetail child : cotDetail.getChildren()) {
            switch (child.getElementName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child detail object: medevac.zMistsMap.zMist." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddMist(CotDetail cotDetail, ProtobufMist.Mist mist) {
        if (mist == null || mist == ProtobufMist.Mist.getDefaultInstance()) {
            return;
        }

        CotDetail mistDetail = new CotDetail(KEY_MIST);

        String[][] stringValues = {
                {KEY_SYMPTOMS, mist.getSymptoms()},
                {KEY_TREATMENT, mist.getTreatment()},
                {KEY_INJURY, mist.getInjury()},
                {KEY_ZAP_NUMBER, mist.getZapNumber()},
                {KEY_TITLE, mist.getTitle()},
                {KEY_MECHANISM_OF_INJURY, mist.getMechanismOfInjury()}
        };

        for (String[] stringValue : stringValues) {
            String key = stringValue[0];
            String value = stringValue[1];

            if (!StringUtils.isNullOrEmpty(value)) {
                mistDetail.setAttribute(key, value);
            }
        }

        cotDetail.addChild(mistDetail);
    }
}
