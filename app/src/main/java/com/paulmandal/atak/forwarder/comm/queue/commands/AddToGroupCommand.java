package com.paulmandal.atak.forwarder.comm.queue.commands;

import java.util.List;

public class AddToGroupCommand extends QueuedCommand {
    public long groupId;
    public List<Long> allMemberGids;
    public List<Long> newMemberGids;

    public AddToGroupCommand(CommandType commandType,
                              int priority,
                              long queuedTime,
                              long groupId,
                              List<Long> allMemberGids,
                              List<Long> newMemberGids) {
        super(commandType, priority, queuedTime);
        this.groupId = groupId;
        this.allMemberGids = allMemberGids;
        this.newMemberGids = newMemberGids;
    }
}
