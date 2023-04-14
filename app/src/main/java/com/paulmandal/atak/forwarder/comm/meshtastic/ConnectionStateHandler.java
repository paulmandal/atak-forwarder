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

    private DeviceConnectionHandler.DeviceConnectionState mDeviceConnectionState = DeviceConnectionHandler.DeviceConnectionState.DISCONNECTED;
    private MeshDeviceConfigurationController.ConfigurationState mDeviceConfigurationState = MeshDeviceConfigurationController.ConfigurationState.DISCONNECTED;
    private MeshServiceController.ServiceConnectionState mServiceConnectionState = MeshServiceController.ServiceConnectionState.DISCONNECTED;
    private MeshtasticDevice mMeshtasticDevice;
    private ConfigProtos.Config.LoRaConfig.RegionCode mRegionCode;
    private boolean mPluginManagesDevice;

    public ConnectionStateHandler(Logger logger,
                                  MeshDeviceConfigurationController meshDeviceConfigurationController,
                                  MeshServiceController meshServiceController,
                                  DeviceConnectionHandler deviceConnectionHandler,
                                  DeviceConfigObserver deviceConfigObserver,
                                  @Nullable MeshtasticDevice meshtasticDevice,
                                  ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                                  boolean pluginManagesDevice) {
        mLogger = logger;
        mMeshtasticDevice = meshtasticDevice;
        mRegionCode = regionCode;
        mPluginManagesDevice = pluginManagesDevice;

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
        mRegionCode = regionCode;
        notifyListeners();
    }

    @Override
    public void onPluginManagesDeviceChanged(boolean pluginManagesDevice) {
        mPluginManagesDevice = pluginManagesDevice;
        notifyListeners();
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public ConnectionState getConnectionState() {
        if (mMeshtasticDevice == null || (mPluginManagesDevice && mRegionCode == ConfigProtos.Config.LoRaConfig.RegionCode.UNSET)) {
            return ConnectionState.NO_DEVICE_CONFIGURED;
        }

        if (mServiceConnectionState == MeshServiceController.ServiceConnectionState.DISCONNECTED) {
            return ConnectionState.NO_SERVICE_CONNECTION;
        }

        if (mDeviceConnectionState == DeviceConnectionHandler.DeviceConnectionState.DISCONNECTED) {
            return ConnectionState.DEVICE_DISCONNECTED;
        }

        if (mDeviceConfigurationState != MeshDeviceConfigurationController.ConfigurationState.READY) {
            return ConnectionState.WRITING_CONFIG;
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
