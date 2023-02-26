package com.paulmandal.atak.forwarder.comm.meshtastic;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class MeshtasticDevice {
    public enum DeviceType {
        BLUETOOTH,
        USB
    }

    @SerializedName("name")
    public final String name;
    @SerializedName("address")
    public final String address;
    @SerializedName("deviceType")
    public final DeviceType deviceType;

    public MeshtasticDevice(String name, String address, DeviceType deviceType) {
        this.name = name;
        this.address = address;
        this.deviceType = deviceType;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("%s - %s - %s", this.name, this.address, this.deviceType);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof MeshtasticDevice)) {
            return false;
        }

        MeshtasticDevice md = (MeshtasticDevice) o;

        return this.address.equals(md.address)
                && this.deviceType.equals(md.deviceType);
    }
}
