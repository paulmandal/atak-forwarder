package com.paulmandal.atak.forwarder.channel;

public class ChannelConfig implements Cloneable {
    public String name;
    public byte[] psk;
    public int modemConfig;
    public boolean isDefault;

    public ChannelConfig(String name, byte[] psk, int modemConfig, boolean isDefault) {
        this.name = name;
        this.psk = psk;
        this.modemConfig = modemConfig;
        this.isDefault = isDefault;
    }

    public ChannelConfig clone() {
        return new ChannelConfig(name, psk, modemConfig, isDefault);
    }
}
