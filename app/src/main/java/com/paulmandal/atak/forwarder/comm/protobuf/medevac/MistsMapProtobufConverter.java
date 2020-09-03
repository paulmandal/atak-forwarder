package com.paulmandal.atak.forwarder.comm.protobuf.medevac;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufMist;
import com.paulmandal.atak.forwarder.protobufs.ProtobufMistsMap;

public class MistsMapProtobufConverter {
    private static final String KEY_MISTS_MAP = "zMistsMap";

    private static final String KEY_MIST = "zMist";

    private MistProtobufConverter mMistProtobufConverter;

    public MistsMapProtobufConverter(MistProtobufConverter mistProtobufConverter) {
        mMistProtobufConverter = mistProtobufConverter;
    }

    public ProtobufMistsMap.MistsMap toMistsMap(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufMistsMap.MistsMap.Builder builder = ProtobufMistsMap.MistsMap.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: medevac.zMistsMap." + attribute.getName());
            }
        }

        for (CotDetail child : cotDetail.getChildren()) {
            switch (child.getElementName()) {
                case KEY_MIST:
                    builder.addZMist(mMistProtobufConverter.toMist(child));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child detail object: medevac.zMistsMap." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddMistsMap(CotDetail cotDetail, ProtobufMistsMap.MistsMap mistsMap) {
        if (mistsMap == null || mistsMap == ProtobufMistsMap.MistsMap.getDefaultInstance()) {
            return;
        }

        CotDetail mistsMapDetail = new CotDetail(KEY_MISTS_MAP);

        for (ProtobufMist.Mist mist : mistsMap.getZMistList()) {
            mMistProtobufConverter.maybeAddMist(mistsMapDetail, mist);
        }

        cotDetail.addChild(mistsMapDetail);
    }

}
