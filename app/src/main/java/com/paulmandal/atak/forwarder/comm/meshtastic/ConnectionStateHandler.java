package com.paulmandal.atak.forwarder.comm.meshtastic;

import androidx.annotation.Nullable;

import com.geeksville.mesh.ConfigProtos;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConnectionStateHandler implements MeshDeviceConfigurationController.Listener, MeshServiceController.Listener, DeviceConnectionHandler.Listener, DeviceConfigObserver.Listener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + ConnectionStateHandler.class.getSimpleName();

    public enum ConnectionState {
        NO_DEVICE_CONFIGURED,
        NO_SERVICE_CONNECTION,
        DEVICE_DISCONNECTED,
        WRITING_CONFIG,
        DEVICE_CONNECTED,
    }

    public interface Listener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

    private final Logger mLogger;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    private DeviceConnectionHandler.DeviceConnectionState mDeviceConnectionState;
    private MeshDeviceConfigurationController.ConfigurationState mDeviceConfigurationState;
    private MeshServiceController.ServiceConnectionState mServiceConnectionState;
    private MeshtasticDevice mMeshtasticDevice;

    public ConnectionStateHandler(Logger logger,
                                  @Nullable MeshtasticDevice meshtasticDevice,
                                  MeshDeviceConfigurationController meshDeviceConfigurationController,
                                  MeshServiceController meshServiceController,
                                  DeviceConnectionHandler deviceConnectionHandler,
                                  DeviceConfigObserver deviceConfigObserver) {
        mLogger = logger;
        mMeshtasticDevice = meshtasticDevice;

        meshDeviceConfigurationController.addListener(this);
        meshServiceController.addListener(this);
        deviceConnectionHandler.addListener(this);
        deviceConfigObserver.addListener(this);
    }

    @Override
    public void onDeviceConnectionStateChanged(DeviceConnectionHandler.DeviceConnectionState deviceConnectionState) {
        mDeviceConnectionState = deviceConnectionState;
        notifyListeners();
    }

    @Override
    public void onConfigurationStateChanged(MeshDeviceConfigurationController.ConfigurationState configurationState) {
        mDeviceConfigurationState = configurationState;
        notifyListeners();
    }

    @Override
    public void onServiceConnectionStateChanged(MeshServiceController.ServiceConnectionState serviceConnectionState) {
        mServiceConnectionState = serviceConnectionState;
        notifyListeners();
    }

    @Override
    public void onSelectedDeviceChanged(MeshtasticDevice meshtasticDevice) {
        mMeshtasticDevice = meshtasticDevice;
        notifyListeners();
    }

    @Override
    public void onDeviceConfigChanged(ConfigProtos.Config.LoRaConfig.RegionCode regionCode, String channelName, int channelMode, byte[] channelPsk, ConfigProtos.Config.DeviceConfig.Role routingRole) {
        // do nothing
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public ConnectionState getConnectionState() {
        if (mMeshtasticDevice == null) {
            return ConnectionState.NO_DEVICE_CONFIGURED;
        }

        if (mServiceConnectionState == MeshServiceController.ServiceConnectionState.DISCONNECTED) {
            return ConnectionState.NO_SERVICE_CONNECTION;
        }

        if (mDeviceConfigurationState != MeshDeviceConfigurationController.ConfigurationState.READY) {
            return ConnectionState.WRITING_CONFIG;
        }

        if (mDeviceConnectionState == DeviceConnectionHandler.DeviceConnectionState.DISCONNECTED) {
            return ConnectionState.DEVICE_DISCONNECTED;
        }

        if (mDeviceConnectionState == DeviceConnectionHandler.DeviceConnectionState.CONNECTED) {
            return ConnectionState.DEVICE_CONNECTED;
        }

        return ConnectionState.NO_DEVICE_CONFIGURED;
    }

    private void notifyListeners() {
        ConnectionState connectionState = getConnectionState();
        mLogger.v(TAG, "Notifying listeners of connection state: " + connectionState);
        for (Listener listener : mListeners) {
            listener.onConnectionStateChanged(connectionState);
        }
    }
}
