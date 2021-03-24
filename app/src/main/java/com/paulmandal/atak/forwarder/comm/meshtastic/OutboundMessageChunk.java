package com.paulmandal.atak.forwarder.comm.meshtastic;

import com.paulmandal.atak.forwarder.comm.MessageType;

public class OutboundMessageChunk extends MessageChunk {
    public final MessageType messageType;
    public final String targetUid;

    public OutboundMessageChunk(MessageType messageType, int index, int count, byte[] chunk, String targetUid) {
        super(index, count, chunk);

        this.messageType = messageType;
        this.targetUid = targetUid;
    }
}
