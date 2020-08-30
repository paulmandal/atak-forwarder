package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTrack;

public class TrackProtobufConverter {
    private static final String KEY_TRACK = "track";

    private static final String KEY_COURSE = "course";
    private static final String KEY_SPEED = "speed";

    public ProtobufTrack.MinimalTrack toTrack(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufTrack.MinimalTrack.Builder builder = ProtobufTrack.MinimalTrack.newBuilder();
        builder.setSpeed(-1.0); // Set this to a default so we know if speed = 0.0 or was null when we're re-assembling
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_COURSE:
                    builder.setCourse(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_SPEED:
                    builder.setSpeed(Double.parseDouble(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: track." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddTrack(CotDetail cotDetail, ProtobufTrack.MinimalTrack track) {
        if (track != null && track != ProtobufTrack.MinimalTrack.getDefaultInstance()) {
            CotDetail trackDetail = new CotDetail(KEY_TRACK);

            if (track.getCourse() != 0.0) {
                trackDetail.setAttribute(KEY_COURSE, Double.toString(track.getCourse()));
            }
            if (track.getSpeed() != -1.0) {
                trackDetail.setAttribute(KEY_SPEED, Double.toString(track.getSpeed()));
            }
            cotDetail.addChild(trackDetail);
        }
    }

}
