package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.Nullable;

public class NonAtakUserInfo extends UserInfo {
    public final double lat;
    public final double lon;
    public final int altitude;

    public NonAtakUserInfo(String callsign, String meshId, boolean isInGroup, @Nullable Integer batteryPercentage, double lat, double lon, int altitude) {
        super(callsign, meshId, null, isInGroup, batteryPercentage);

        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
    }
}
