package com.paulmandal.atak.forwarder.comm.queue.commands;

import com.atakmap.coremap.cot.event.CotEvent;

public class QueuedCommandFactory {
    public BroadcastDiscoveryCommand createBroadcastDiscoveryCommand(byte[] discoveryMessage) {
        return new BroadcastDiscoveryCommand(CommandType.BROADCAST_DISCOVERY_MSG, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis(), discoveryMessage);
    }

    public SendMessageCommand createSendMessageCommand(int priority, CotEvent cotEvent, byte[] message, String[] toUIDs) {
        CommandType commandType = toUIDs == null ? CommandType.SEND_TO_CHANNEL : CommandType.SEND_TO_INDIVIDUAL;
        return new SendMessageCommand(commandType, priority, System.currentTimeMillis(), cotEvent, message, toUIDs);
    }
}
