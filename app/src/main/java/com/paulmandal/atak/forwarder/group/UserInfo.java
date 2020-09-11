package com.paulmandal.atak.forwarder.group;

public class UserInfo {
    public String callsign;
    public String meshId;
    public String atakUid;
    public boolean isInGroup;

    public UserInfo(String callsign, String meshId, String atakUid, boolean isInGroup) {
        this.callsign = callsign;
        this.meshId = meshId;
        this.atakUid = atakUid;
        this.isInGroup = isInGroup;
    }

    public UserInfo clone() {
        return new UserInfo(this.callsign, this.meshId, this.atakUid, this.isInGroup);
    }
}
