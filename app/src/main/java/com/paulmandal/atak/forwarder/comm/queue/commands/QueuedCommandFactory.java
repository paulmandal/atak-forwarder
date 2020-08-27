package com.paulmandal.atak.forwarder.comm.queue.commands;

import com.atakmap.coremap.cot.event.CotEvent;

import java.util.List;

public class QueuedCommandFactory {
    public QueuedCommand createScanForCommDeviceCommand() {
        return new QueuedCommand(CommandType.SCAN_FOR_COMM_DEVICE, QueuedCommand.PRIORITY_HIGHEST, System.currentTimeMillis());
    }

    public QueuedCommand createDisconnectFromCommDeviceCommand() {
        return new QueuedCommand(CommandType.DISCONNECT_FROM_COMM_DEVICE, QueuedCommand.PRIORITY_HIGHEST, System.currentTimeMillis());
    }

    public BroadcastDiscoveryCommand createBroadcastDiscoveryCommand(byte[] discoveryMessage) {
        return new BroadcastDiscoveryCommand(CommandType.BROADCAST_DISCOVERY_MSG, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis(), discoveryMessage);
    }

    public CreateGroupCommand createCreateGroupCommand(List<Long> memberGids) {
        return new CreateGroupCommand(CommandType.CREATE_GROUP, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis(), memberGids);
    }

    public AddToGroupCommand createAddToGroupCommand(long groupId, List<Long> allMemberGids, List<Long> newMemberGids) {
        return new AddToGroupCommand(CommandType.ADD_TO_GROUP, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis(), groupId, allMemberGids, newMemberGids);
    }

    public SendMessageCommand createSendMessageCommand(int priority, CotEvent cotEvent, byte[] message, String[] toUIDs) {
        CommandType commandType = toUIDs == null ? CommandType.SEND_TO_GROUP : CommandType.SEND_TO_INDIVIDUAL;
        return new SendMessageCommand(commandType, priority, System.currentTimeMillis(), cotEvent, message, toUIDs);
    }

    public QueuedCommand createRequestBatteryStatusCommand() {
        return new QueuedCommand(CommandType.GET_BATTERY_STATUS, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis());
    }
}
