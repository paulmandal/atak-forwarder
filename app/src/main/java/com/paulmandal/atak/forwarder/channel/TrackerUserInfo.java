package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.Nullable;

public class TrackerUserInfo extends UserInfo {
    public double lat;
    public double lon;
    public int altitude;
    public boolean gpsValid;
    public String shortName;
    public long lastSeenTime;

    public TrackerUserInfo(String callsign, String meshId, @Nullable Integer batteryPercentage, double lat, double lon, int altitude, boolean gpsValid, String shortName, long lastSeenTime) {
        super(callsign, meshId, null, batteryPercentage);

        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
        this.gpsValid = gpsValid;
        this.shortName = shortName;
        this.lastSeenTime = lastSeenTime;
    }
}
