package com.paulmandal.atak.forwarder.comm.meshtastic;

public class MessageChunk {
    public final int index;
    public final int count;
    public final byte[] chunk;

    public MessageChunk(int index, int count, byte[] chunk) {
        this.index = index;
        this.count = count;
        this.chunk = chunk;
    }
}