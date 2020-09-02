package com.paulmandal.atak.forwarder.comm.protobuf.medevac;

import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufMist;

public class MistProtobufConverter {
    private static final String KEY_MIST = "zMist";

    public ProtobufMist.Mist toMist(CotDetail cotDetail) {
        ProtobufMist.Mist.Builder builder = ProtobufMist.Mist.newBuilder();

        // TODO: everything

        return builder.build();
    }

    public void maybeAddMist(CotDetail cotDetail, ProtobufMist.Mist mist) {
        if (mist == null || mist == ProtobufMist.Mist.getDefaultInstance()) {
            return;
        }

        CotDetail mistDetail = new CotDetail(KEY_MIST);

        // TODO: everything

        cotDetail.addChild(mistDetail);
    }
}
