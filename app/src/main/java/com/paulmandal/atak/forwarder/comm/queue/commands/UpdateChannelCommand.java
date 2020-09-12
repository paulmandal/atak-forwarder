package com.paulmandal.atak.forwarder.comm.queue.commands;

import com.geeksville.mesh.MeshProtos;

public class UpdateChannelCommand extends QueuedCommand {
    public String channelName;
    public byte[] psk;
    public MeshProtos.ChannelSettings.ModemConfig modemConfig;

    public UpdateChannelCommand(CommandType commandType,
                                int priority,
                                long queuedTime,
                                String channelName,
                                byte[] psk,
                                MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        super(commandType, priority, queuedTime);

        this.channelName = channelName;
        this.psk = psk;
        this.modemConfig = modemConfig;
    }
}
