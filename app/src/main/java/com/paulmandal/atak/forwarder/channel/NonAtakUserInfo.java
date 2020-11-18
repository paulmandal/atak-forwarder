package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.Nullable;

public class NonAtakUserInfo extends UserInfo {
    public double lat;
    public double lon;
    public int altitude;
    public boolean gpsValid;
    public String shortName;

    public NonAtakUserInfo(String callsign, String meshId, @Nullable Integer batteryPercentage, double lat, double lon, int altitude, boolean gpsValid, String shortName) {
        super(callsign, meshId, null, batteryPercentage);

        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
        this.gpsValid = gpsValid;
        this.shortName = shortName;
    }
}
