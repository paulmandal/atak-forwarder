package com.paulmandal.atak.forwarder.comm.meshtastic;

public class RMeshDeviceConfigurator implements RMeshConnectionHandler.DeviceConnectionStateListener {
    public enum ConfigurationState {
        CHECKING,
        WRITING,
        FINISHED
    }

    public interface ConfigurationStateListener {
        void onConfigurationStateChanged(ConfigurationState configurationState);
    }

    public RMeshDeviceConfigurator(RMeshConnectionHandler meshConnectionHandler) {
        meshConnectionHandler.addListener(this);
    }

    @Override
    public void onDeviceConnectionStateChanged(RMeshConnectionHandler.DeviceConnectionState deviceConnectionState) {
        // TODO: check config
        // TODO: build and write config if necessary
        // TODO: validate config on reconnect
    }
}
