package com.paulmandal.atak.forwarder.comm.queue.commands;

public class UpdateChannelCommand extends QueuedCommand {
    public String channelName;
    public byte[] psk;

    public UpdateChannelCommand(CommandType commandType,
                                int priority,
                                long queuedTime,
                                String channelName,
                                byte[] psk) {
        super(commandType, priority, queuedTime);

        this.channelName = channelName;
        this.psk = psk;
    }
}
