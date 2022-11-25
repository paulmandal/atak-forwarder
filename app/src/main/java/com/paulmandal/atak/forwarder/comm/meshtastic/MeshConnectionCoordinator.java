package com.paulmandal.atak.forwarder.comm.meshtastic;

public class MeshConnectionCoordinator {
    public enum ConnectionState {
        DISCONNECTED,
        CONFIGURATION_IN_PROGRESS,
        CONNECTED
    }

    public interface ConnectionStateListener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

    // TODO: move connectionState into MeshServiceController
    // TODO: emit events from MeshDeviceConfigurer
    // TODO: emit events from MeshTrackerConfigurer
    // TODO: merge events -> listeners
    // TODO: remove MeshSuspendController
}
