package com.paulmandal.atak.forwarder.comm.queue.commands;

public class BroadcastDiscoveryCommand extends QueuedCommand {
    public final byte[] discoveryMessage;

    public BroadcastDiscoveryCommand(CommandType commandType, int priority, long queuedTime, byte[] discoveryMessage) {
        super(commandType, priority, queuedTime);
        this.discoveryMessage = discoveryMessage;
    }
}
