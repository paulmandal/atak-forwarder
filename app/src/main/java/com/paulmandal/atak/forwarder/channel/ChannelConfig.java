package com.paulmandal.atak.forwarder.channel;

public class ChannelConfig {
    public String name;
    public byte[] psk;
    public int mode;
    public boolean isDefault;

    public ChannelConfig(String name, byte[] psk, int mode, boolean isDefault) {
        this.name = name;
        this.psk = psk;
        this.mode = mode;
        this.isDefault = isDefault;
    }
}
