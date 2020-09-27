package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.protobufs.ProtobufTrack;

public class TrackProtobufConverter {
    private static final String KEY_TRACK = "track";

    private static final String KEY_COURSE = "course";
    private static final String KEY_SPEED = "speed";

    private static final double NULL_MARKER = -1.0;

    public ProtobufTrack.Track toTrack(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufTrack.Track.Builder builder = ProtobufTrack.Track.newBuilder();
        builder.setCourse(NULL_MARKER);
        builder.setSpeed(NULL_MARKER);
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

    public void maybeAddTrack(CotDetail cotDetail, ProtobufTrack.Track track) {
        if (track == null || track == ProtobufTrack.Track.getDefaultInstance()) {
            return;
        }

        CotDetail trackDetail = new CotDetail(KEY_TRACK);

        if (track.getCourse() != NULL_MARKER) {
            trackDetail.setAttribute(KEY_COURSE, Double.toString(track.getCourse()));
        }
        if (track.getSpeed() != NULL_MARKER) {
            trackDetail.setAttribute(KEY_SPEED, Double.toString(track.getSpeed()));
        }
        cotDetail.addChild(trackDetail);
    }
}
