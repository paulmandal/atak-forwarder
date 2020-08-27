package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.maps.time.CoordinatedTime;

public class CustomBytesFields {
    public final CoordinatedTime time;
    public final CoordinatedTime stale;
    public final double hae;

    public CustomBytesFields(CoordinatedTime time, CoordinatedTime stale, double hae) {
        this.time = time;
        this.stale = stale;
        this.hae = hae;
    }
}