package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTakv;

public class TakvProtobufConverter {
    private static final String KEY_TAKV = "takv";

    private static final String KEY_DEVICE = "device";
    private static final String KEY_OS = "os";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_VERSION = "version";

    public ProtobufTakv.Takv toTakv(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufTakv.Takv.Builder builder = ProtobufTakv.Takv.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_DEVICE:
                    builder.setDevice(attribute.getValue());
                    break;
                case KEY_OS:
                    builder.setOs(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_PLATFORM:
                    builder.setPlatform(attribute.getValue());
                    break;
                case KEY_VERSION:
                    builder.setVersion(attribute.getValue());
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: takv." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddTakv(CotDetail cotDetail, ProtobufTakv.Takv takv) {
        if (takv == null || takv == ProtobufTakv.Takv.getDefaultInstance()) {
            return;
        }

        CotDetail takvDetail = new CotDetail(KEY_TAKV);

        takvDetail.setAttribute(KEY_OS, Integer.toString(takv.getOs()));
        if (!StringUtils.isNullOrEmpty(takv.getVersion())) {
            takvDetail.setAttribute(KEY_VERSION, takv.getVersion());
        }
        if (!StringUtils.isNullOrEmpty(takv.getDevice())) {
            takvDetail.setAttribute(KEY_DEVICE, takv.getDevice());
        }
        if (!StringUtils.isNullOrEmpty(takv.getPlatform())) {
            takvDetail.setAttribute(KEY_PLATFORM, takv.getPlatform());
        }
        cotDetail.addChild(takvDetail);
    }
}
