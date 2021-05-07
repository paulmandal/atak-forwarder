package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import java.util.Objects;

public class TrackerUserInfo extends UserInfo {
    public static final int NO_LAT_LON_ALT_VALUE = -1;

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

    @CallSuper
    public void update(TrackerUserInfo updatedUserInfo) {
        super.update(updatedUserInfo);

        if (updatedUserInfo.lat != NO_LAT_LON_ALT_VALUE && !Objects.equals(this.lat, updatedUserInfo.lat)) {
            this.lat = updatedUserInfo.lat;
        }

        if (updatedUserInfo.lon != NO_LAT_LON_ALT_VALUE && !Objects.equals(this.lon, updatedUserInfo.lon)) {
            this.lon = updatedUserInfo.lon;
        }

        if (updatedUserInfo.altitude != NO_LAT_LON_ALT_VALUE && !Objects.equals(this.altitude, updatedUserInfo.altitude)) {
            this.altitude = updatedUserInfo.altitude;
        }

        if (updatedUserInfo.gpsValid && !Objects.equals(this.gpsValid, updatedUserInfo.gpsValid)) {
            this.gpsValid = updatedUserInfo.gpsValid;
        }

        if (updatedUserInfo.shortName != null && !Objects.equals(this.shortName, updatedUserInfo.shortName)) {
            this.shortName = updatedUserInfo.shortName;
        }

        if (!Objects.equals(this.lastSeenTime, updatedUserInfo.lastSeenTime)) {
            this.lastSeenTime = updatedUserInfo.lastSeenTime;
        }
    }
}
