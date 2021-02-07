package com.paulmandal.atak.forwarder.comm.meshtastic;

public class MessageChunk {
    public int index;
    public int count;
    public byte[] chunk;

    public MessageChunk(int index, int count, byte[] chunk) {
        this.index = index;
        this.count = count;
        this.chunk = chunk;
    }
}