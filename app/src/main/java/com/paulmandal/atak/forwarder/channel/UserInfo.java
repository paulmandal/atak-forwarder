package com.paulmandal.atak.forwarder.channel;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import java.util.Objects;

public class UserInfo implements Cloneable {
    public static final String CALLSIGN_UNKNOWN = "callsign-unknown";

    public String callsign;
    public final String meshId;

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

    @CallSuper
    public void update(UserInfo updatedUserInfo) {
        if (updatedUserInfo.callsign != null && !updatedUserInfo.callsign.equals(CALLSIGN_UNKNOWN) && !Objects.equals(this.callsign, updatedUserInfo.callsign)) {
            this.callsign = updatedUserInfo.callsign;
        }

        if (updatedUserInfo.atakUid != null && !Objects.equals(this.atakUid, updatedUserInfo.atakUid)) {
            this.atakUid = updatedUserInfo.atakUid;
        }

        if (updatedUserInfo.batteryPercentage != null && !Objects.equals(this.batteryPercentage, updatedUserInfo.batteryPercentage)) {
            this.batteryPercentage = updatedUserInfo.batteryPercentage;
        }
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
        return otherUserInfo.meshId.equals(this.meshId);
    }
}
