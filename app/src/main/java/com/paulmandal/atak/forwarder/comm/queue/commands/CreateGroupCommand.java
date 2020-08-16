package com.paulmandal.atak.forwarder.comm.queue.commands;

import java.util.List;

public class CreateGroupCommand extends QueuedCommand {
    public List<Long> memberGids;

    public CreateGroupCommand(CommandType commandType,
                              int priority,
                              long queuedTime,
                              List<Long> memberGids) {
        super(commandType, priority, queuedTime);
        this.memberGids = memberGids;
    }
}
