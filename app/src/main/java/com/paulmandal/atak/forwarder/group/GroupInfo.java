package com.paulmandal.atak.forwarder.group;

import java.util.List;

public class GroupInfo {
    public long groupId;
    public List<Long> memberGids;

    public GroupInfo(long groupId, List<Long> memberGids) {
        this.groupId = groupId;
        this.memberGids = memberGids;
    }
}
