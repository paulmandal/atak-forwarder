package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.Nullable;

public class UserInfo {
    public String callsign;
    public String meshId;

    @Nullable
    public String atakUid;

    @Nullable
    public Integer batteryPercentage;

    public UserInfo(String callsign, String meshId, @Nullable String atakUid, @Nullable Integer batteryPercentage) {
        this.callsign = callsign;
        this.meshId = meshId;
        this.atakUid = atakUid;
        this.batteryPercentage = batteryPercentage;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public UserInfo clone() {
        return new UserInfo(this.callsign, this.meshId, this.atakUid, this.batteryPercentage);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UserInfo)) {
            return false;
        }

        UserInfo otherUserInfo = (UserInfo) other;
        return otherUserInfo.callsign.equals(this.callsign)
                && otherUserInfo.meshId.equals(this.meshId)
                && (otherUserInfo.atakUid == null || otherUserInfo.atakUid.equals(this.atakUid));
    }
}
