package com.paulmandal.atak.forwarder.gson;

import com.google.gson.annotations.SerializedName;

public class ChannelConfig implements Cloneable {
    @SerializedName("name")
    public String name;
    @SerializedName("psk")
    public byte[] psk;
    @SerializedName("modemConfig")
    public int modemConfig;
    @SerializedName("isDefault")
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
