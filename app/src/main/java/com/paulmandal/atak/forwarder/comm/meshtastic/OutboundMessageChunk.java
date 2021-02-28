package com.paulmandal.atak.forwarder.comm.meshtastic;

public class OutboundMessageChunk extends MessageChunk {
    public final String targetUid;

    public OutboundMessageChunk(int index, int count, byte[] chunk, String targetUid) {
        super(index, count, chunk);

        this.targetUid = targetUid;
    }
}
