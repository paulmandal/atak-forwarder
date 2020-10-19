package com.paulmandal.atak.forwarder.comm.commhardware.meshtastic;

public class MeshtasticDevice {
    public enum DeviceType {
        BLUETOOTH,
        USB
    }

    public final String name;
    public final String address;
    public final DeviceType deviceType;

    public MeshtasticDevice(String name, String address, DeviceType deviceType) {
        this.name = name;
        this.address = address;
        this.deviceType = deviceType;
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %s", this.name, this.address, this.deviceType);
    }
}
