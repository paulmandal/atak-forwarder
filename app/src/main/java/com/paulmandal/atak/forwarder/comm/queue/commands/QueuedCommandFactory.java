package com.paulmandal.atak.forwarder.comm.queue.commands;

import com.atakmap.coremap.cot.event.CotEvent;
import com.geeksville.mesh.MeshProtos;

public class QueuedCommandFactory {
    public QueuedCommand createScanForCommDeviceCommand() {
        return new QueuedCommand(CommandType.SCAN_FOR_COMM_DEVICE, QueuedCommand.PRIORITY_HIGHEST, System.currentTimeMillis());
    }

    public BroadcastDiscoveryCommand createBroadcastDiscoveryCommand(byte[] discoveryMessage) {
        return new BroadcastDiscoveryCommand(CommandType.BROADCAST_DISCOVERY_MSG, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis(), discoveryMessage);
    }

    public UpdateChannelCommand createUpdateChannelCommand(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        return new UpdateChannelCommand(CommandType.UPDATE_CHANNEL, QueuedCommand.PRIORITY_HIGH, System.currentTimeMillis(), channelName, psk, modemConfig);
    }

    public SendMessageCommand createSendMessageCommand(int priority, CotEvent cotEvent, byte[] message, String[] toUIDs) {
        CommandType commandType = toUIDs == null ? CommandType.SEND_TO_CHANNEL : CommandType.SEND_TO_INDIVIDUAL;
        return new SendMessageCommand(commandType, priority, System.currentTimeMillis(), cotEvent, message, toUIDs);
    }
}
