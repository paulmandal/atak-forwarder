package com.paulmandal.atak.forwarder.comm.queue.commands;

import com.atakmap.coremap.cot.event.CotEvent;

public class SendMessageCommand extends QueuedCommand {
    public CotEvent cotEvent;
    public byte[] message;
    public String[] toUIDs;

    public SendMessageCommand(CommandType commandType, int priority, long queuedTime, CotEvent cotEvent, byte[] message, String[] toUIDs) {
        super(commandType, priority, queuedTime);
        this.cotEvent = cotEvent;
        this.message = message;
        this.toUIDs = toUIDs;
    }

    public void takeStateFrom(SendMessageCommand sendMessageCommand) {
        this.cotEvent = sendMessageCommand.cotEvent;
        this.toUIDs = sendMessageCommand.toUIDs;
        this.message = sendMessageCommand.message;
    }
}
