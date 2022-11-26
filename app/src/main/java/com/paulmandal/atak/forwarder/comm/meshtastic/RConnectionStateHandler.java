package com.paulmandal.atak.forwarder.comm.meshtastic;

public class RConnectionStateHandler {
    public enum ConnectionState {
        NO_DEVICE_CONFIGURED,
        NO_SERVICE_CONNECTION,
        DEVICE_DISCONNECTED,
        DEVICE_UNCONFIGURED,
        DEVICE_CONNECTED,
    }

    public interface ConnectionStateListener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

}
