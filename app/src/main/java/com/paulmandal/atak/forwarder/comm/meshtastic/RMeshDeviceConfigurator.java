package com.paulmandal.atak.forwarder.comm.meshtastic;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RMeshDeviceConfigurator implements RMeshConnectionHandler.DeviceConnectionStateListener {
    public enum ConfigurationState {
        CHECKING,
        WRITING,
        FINISHED
    }

    public interface ConfigurationStateListener {
        void onConfigurationStateChanged(ConfigurationState configurationState);
    }

    private final RMeshServiceController mMeshServiceController;
    private final Set<ConfigurationStateListener> mListeners = new CopyOnWriteArraySet<>();

    private boolean mConfigurationChecked;
    private boolean mConfigurationWritten;

    public RMeshDeviceConfigurator(RMeshConnectionHandler meshConnectionHandler,
                                   RMeshServiceController meshServiceController) {
        mMeshServiceController = meshServiceController;

        meshConnectionHandler.addListener(this);
    }

    @Override
    public void onDeviceConnectionStateChanged(RMeshConnectionHandler.DeviceConnectionState deviceConnectionState) {
        if (deviceConnectionState == RMeshConnectionHandler.DeviceConnectionState.CONNECTED) {
            if (!mConfigurationChecked) {
                notifyListeners(ConfigurationState.CHECKING);

                if (maybeWriteConfig()) {
                    return;
                }
            }

            if (mConfigurationWritten) {
                notifyListeners(ConfigurationState.FINISHED);
            }
        }
    }

    private boolean maybeWriteConfig() {
        mMeshServiceController.getMeshService().getConfig();
        // TODO: compare config
        // TODO: write config
        // TODO: notify listeners
    }

    private void notifyListeners(ConfigurationState configurationState) {
        for (ConfigurationStateListener listener : mListeners) {
            listener.onConfigurationStateChanged(configurationState);
        }
    }
}
