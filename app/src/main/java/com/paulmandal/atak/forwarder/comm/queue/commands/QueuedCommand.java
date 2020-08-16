package com.paulmandal.atak.forwarder.comm.queue.commands;

public class QueuedCommand {
    public static final int PRIORITY_LOWEST  = 0; // Unused
    public static final int PRIORITY_LOW     = 1; // Map Markers, PLI
    public static final int PRIORITY_MEDIUM  = 2; // Chat Msgs
    public static final int PRIORITY_HIGH    = 3; // Things like add/remove from group, broadcast discovery
    public static final int PRIORITY_HIGHEST = 4; // Things like connect/disconnect from the device

    public final CommandType commandType;
    public final int priority;
    public final long queuedTime;

    public QueuedCommand(CommandType commandType, int priority, long queuedTime) {
        this.commandType = commandType;
        this.priority = priority;
        this.queuedTime = queuedTime;
    }
}
