package com.paulmandal.atak.forwarder.group;

public class UserInfo {
    public String callsign;
    public long gId;
    public String atakUid;
    public boolean isInGroup;

    public UserInfo(String callsign, long gId, String atakUid, boolean isInGroup) {
        this.callsign = callsign;
        this.gId = gId;
        this.atakUid = atakUid;
        this.isInGroup = isInGroup;
    }

    public UserInfo clone() {
        return new UserInfo(this.callsign, this.gId, this.atakUid, this.isInGroup);
    }
}
