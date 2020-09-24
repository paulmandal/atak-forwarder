package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.Nullable;

public class NonAtakUserInfo extends UserInfo {
    public final double lat;
    public final double lon;
    public final int altitude;
    public final String shortName;

    public NonAtakUserInfo(String callsign, String meshId, boolean isInGroup, @Nullable Integer batteryPercentage, double lat, double lon, int altitude, String shortName) {
        super(callsign, meshId, null, isInGroup, batteryPercentage);

        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
        this.shortName = shortName;
    }
}
