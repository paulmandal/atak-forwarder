package com.paulmandal.atak.forwarder.comm.protobuf.medevac;

import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufMedevac;

public class MedevacProtobufConverter {
    private static final String KEY_MEDEVAC = "_medevac_";

    private MistsMapProtobufConverter mMistsMapProtobufConverter;

    public MedevacProtobufConverter(MistsMapProtobufConverter mistsMapProtobufConverter) {
        mMistsMapProtobufConverter = mistsMapProtobufConverter;
    }

    public ProtobufMedevac.Medevac toMedevac(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufMedevac.Medevac.Builder builder = ProtobufMedevac.Medevac.newBuilder();

        // TODO: do everything

        return builder.build();
    }

    public void maybeAddMedevac(CotDetail cotDetail, ProtobufMedevac.Medevac medevac) {
        if (medevac == null || medevac == ProtobufMedevac.Medevac.getDefaultInstance()) {
            return;
        }

        CotDetail medevacDetail = new CotDetail(KEY_MEDEVAC);

        // TODO: everything

        cotDetail.addChild(medevacDetail);
    }
}
