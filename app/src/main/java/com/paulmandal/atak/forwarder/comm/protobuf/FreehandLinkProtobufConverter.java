package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufFreehandLink;

public class FreehandLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_LINE = "line";

    public ProtobufFreehandLink.MinimalFreehandLink toFreehandLink(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufFreehandLink.MinimalFreehandLink.Builder builder = ProtobufFreehandLink.MinimalFreehandLink.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_LINE:
                    builder.setLine(attribute.getValue()); // TODO: gzip or EXI this
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: freehandlink." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddFreehandLink(CotDetail cotDetail, ProtobufFreehandLink.MinimalFreehandLink freehandLink) {
        if (freehandLink != null && freehandLink != ProtobufFreehandLink.MinimalFreehandLink.getDefaultInstance()) {
            CotDetail freehandLinkDetail = new CotDetail(KEY_LINK);

            if (!StringUtils.isNullOrEmpty(freehandLink.getLine())) {
                freehandLinkDetail.setAttribute(KEY_LINE, freehandLink.getLine());
            }

            cotDetail.addChild(freehandLinkDetail);
        }
    }
}
