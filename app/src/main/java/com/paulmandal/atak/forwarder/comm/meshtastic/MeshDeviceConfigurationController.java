package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.ekito.simpleKML.model.GroundOverlay;
import com.geeksville.mesh.ConfigProtos;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshDeviceConfigurationController implements DeviceConnectionHandler.Listener, DeviceConfigObserver.Listener, MeshServiceController.Listener, MeshDeviceConfigurator.Listener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshDeviceConfigurationController.class.getSimpleName();

    public enum ConfigurationState {
        DISCONNECTED,
        WRITING_TRACKER,
        WRITING_COMM,
        READY
    }

    public interface Listener {
        void onConfigurationStateChanged(ConfigurationState configurationState);
    }

    private final MeshServiceController mMeshServiceController;
    private final DeviceConnectionHandler mDeviceConnectionHandler;
    private final MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private final MeshDeviceConfiguratorFactory mMeshDeviceConfiguratorFactory;
    private final HashHelper mHashHelper;
    private final Logger mLogger;
    private final String mCallsign;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    @Nullable
    private MeshtasticDevice mMeshtasticDevice;
    private ConfigProtos.Config.DeviceConfig.Role mRoutingRole;
    private ConfigProtos.Config.LoRaConfig.RegionCode mRegionCode;

    private String mChannelName;
    private int mChannelMode;
    private byte[] mChannelPsk;

    private String mLongName;
    private String mShortName;

    private DeviceConnectionHandler.DeviceConnectionState mDeviceConnectionState = DeviceConnectionHandler.DeviceConnectionState.DISCONNECTED;

    private boolean mPluginManagesDevice;
    private boolean mSetDeviceAddressCalled;
    private boolean mInitialDeviceWriteStarted;

    private MeshDeviceConfigurator mActiveCommConfigurator;
    private MeshDeviceConfigurator mActiveTrackerConfigurator;
    private MeshDeviceConfigurator mStagedCommConfigurator;
    private MeshDeviceConfigurator mStagedTrackerConfigurator;

    public MeshDeviceConfigurationController(MeshServiceController meshServiceController,
                                             DeviceConnectionHandler deviceConnectionHandler,
                                             MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                             MeshDeviceConfiguratorFactory meshDeviceConfiguratorFactory,
                                             DeviceConfigObserver deviceConfigObserver,
                                             HashHelper hashHelper,
                                             Logger logger,
                                             @Nullable MeshtasticDevice meshtasticDevice,
                                             ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                                             String channelName,
                                             int channelMode,
                                             byte[] psk,
                                             ConfigProtos.Config.DeviceConfig.Role routingRole,
                                             boolean pluginManagesDevice,
                                             String callsign) {
        mMeshServiceController = meshServiceController;
        mDeviceConnectionHandler = deviceConnectionHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mMeshDeviceConfiguratorFactory = meshDeviceConfiguratorFactory;
        mHashHelper = hashHelper;
        mLogger = logger;
        mMeshtasticDevice = meshtasticDevice;
        mRegionCode = regionCode;
        mChannelName = channelName;
        mChannelMode = channelMode;
        mChannelPsk = psk;
        mRoutingRole = routingRole;
        mPluginManagesDevice = pluginManagesDevice;
        mCallsign = callsign;

        meshServiceController.addListener(this);
        deviceConnectionHandler.addListener(this);
        deviceConfigObserver.addListener(this);

        if (meshtasticDevice != null) {
            updateLongNameAndShortName();
        }
    }

    @Override
    public void onServiceConnectionStateChanged(MeshServiceController.ServiceConnectionState serviceConnectionState) {
        if (serviceConnectionState == MeshServiceController.ServiceConnectionState.CONNECTED && !mSetDeviceAddressCalled) {
            if (mMeshtasticDevice == null) {
                return;
            }

            try {
                mLogger.v(TAG, "Calling setDeviceAddress: " + mMeshtasticDevice.address);
                mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshServiceController.getMeshService(), mMeshtasticDevice);
            } catch (RemoteException e) {
                mLogger.e(TAG, "RemoteException calling setDeviceAddress: " + e.getMessage());
                e.printStackTrace();
            }

            // TODO: delayed retry on failure?

            mSetDeviceAddressCalled = true;
        }
    }

    @Override
    public void onDeviceConnectionStateChanged(DeviceConnectionHandler.DeviceConnectionState deviceConnectionState) {
        mDeviceConnectionState = deviceConnectionState;

        if (deviceConnectionState != DeviceConnectionHandler.DeviceConnectionState.CONNECTED) {
            return;
        }

        if (!mInitialDeviceWriteStarted) {
            mLogger.v(TAG, "Doing initial comm device check/write");
            mInitialDeviceWriteStarted = true;
            if (mMeshtasticDevice != null) {
                writeCommDevice();
            }
        }
    }

    @Override
    public void onSelectedDeviceChanged(MeshtasticDevice meshtasticDevice) {
        mLogger.v(TAG, "Selected device changed: " + meshtasticDevice.address);
        mMeshtasticDevice = meshtasticDevice;

        updateLongNameAndShortName();
        writeCommDevice();
    }

    @Override
    public void onDeviceConfigChanged(ConfigProtos.Config.LoRaConfig.RegionCode regionCode, String channelName, int channelMode, byte[] channelPsk, ConfigProtos.Config.DeviceConfig.Role routingRole) {
        mLogger.v(TAG, "Device configuration changed");
        mRegionCode = regionCode;
        mChannelName = channelName;
        mChannelMode = channelMode;
        mChannelPsk = channelPsk;
        mRoutingRole = routingRole;

        writeCommDevice();
    }

    @Override
    public void onPluginManagesDeviceChanged(boolean pluginManagesDevice) {
        mPluginManagesDevice = pluginManagesDevice;

        if (pluginManagesDevice) {
            writeCommDevice();
        }
    }

    @Override
    public void onConfigurationStateChanged(MeshDeviceConfigurator.ConfigurationState configurationState) {
        if (configurationState == MeshDeviceConfigurator.ConfigurationState.FINISHED) {
            mLogger.v(TAG, "Device writing finished, checking for any further configurators");
            if (mActiveCommConfigurator != null) {
                mActiveCommConfigurator.removeListener(this);
                mActiveCommConfigurator = null;
            }

            // Promote another configurator
            if (mStagedCommConfigurator != null) {
                mLogger.v(TAG, "Promoting staged comm configurator");
                setActiveCommConfigurator(mStagedCommConfigurator, ConfigurationState.WRITING_COMM);
                mStagedCommConfigurator = null;
                return;
            }

            if (mActiveTrackerConfigurator != null) {
                mLogger.v(TAG, "Removing listeners for tracker configurator");
                mActiveTrackerConfigurator.removeListener(this);
                mActiveTrackerConfigurator = null;
            }

            if (mStagedTrackerConfigurator != null) {
                mLogger.v(TAG, "Promoting staged tracker configurator");
                mActiveTrackerConfigurator = mStagedTrackerConfigurator;
                mActiveTrackerConfigurator.addListener(this);
                mActiveTrackerConfigurator.start();
                notifyListeners(ConfigurationState.WRITING_TRACKER);
                return;
            }

            notifyListeners(ConfigurationState.READY);
        }

        if (configurationState == MeshDeviceConfigurator.ConfigurationState.FAILED) {
            mLogger.e(TAG, "Writing to device failed");
        }
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void writeTracker(MeshtasticDevice meshtasticDevice,
                             String longName,
                             String shortName,
                             ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                             int pliUpdateInterval,
                             int screenOnSecs,
                             String channelName,
                             int channelMode,
                             byte[] channelPsk,
                             ConfigProtos.Config.DeviceConfig.Role routingRole,
                             MeshDeviceConfigurator.Listener listener) {

        MeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
                mMeshServiceController,
                mDeviceConnectionHandler,
                mMeshtasticDeviceSwitcher,
                mHashHelper,
                mLogger,
                meshtasticDevice,
                longName,
                shortName,
                regionCode,
                pliUpdateInterval,
                true,
                screenOnSecs,
                channelName,
                channelMode,
                channelPsk,
                routingRole,
                true);
        meshDeviceConfigurator.addListener(listener);

        if (mActiveCommConfigurator != null || mActiveTrackerConfigurator != null) {
            mStagedTrackerConfigurator = meshDeviceConfigurator;
            return;
        }

        mLogger.v(TAG, "Starting tracker configurator");
        mActiveTrackerConfigurator = meshDeviceConfigurator;
        meshDeviceConfigurator.addListener(this);
        notifyListeners(ConfigurationState.WRITING_TRACKER);
        meshDeviceConfigurator.start();

        mStagedCommConfigurator = createCommDeviceConfigurator();
    }

    private void updateLongNameAndShortName() {
        if (mMeshtasticDevice == null) {
            return;
        }

        String meshId = mMeshtasticDevice.address.replace(":", "").toLowerCase();
        String shortMeshId = meshId.replaceAll("!", "").substring(meshId.length() - 4);
        char lastChar = (char) (shortMeshId.charAt(shortMeshId.length() - 1) - 2);
        if (lastChar < '0') {
            lastChar = '0';
        }
        shortMeshId = shortMeshId.substring(0, shortMeshId.length() - 1) + lastChar; // TODO: need to find a way to exclude some devices (e.g. RAK)
        mLongName = String.format("%s-MX-%s", mCallsign, shortMeshId);
        mShortName = mCallsign.substring(0, 1);
    }

    private void writeCommDevice() {
        if (mDeviceConnectionState != DeviceConnectionHandler.DeviceConnectionState.CONNECTED) {
            return;
        }

        mLogger.v(TAG, "Spawning comm device configurator");
        MeshDeviceConfigurator meshDeviceConfigurator = createCommDeviceConfigurator();

        if (mActiveCommConfigurator != null) {
            mStagedCommConfigurator = meshDeviceConfigurator;
            return;
        }

        maybeCancelAndRemoveActiveCommConfigurator();
        setActiveCommConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_COMM);
    }

    private MeshDeviceConfigurator createCommDeviceConfigurator() {
        mLogger.d(TAG, "Creating comm device configurator with writeToCommDevice: " + mPluginManagesDevice);
        return mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
                mMeshServiceController,
                mDeviceConnectionHandler,
                mMeshtasticDeviceSwitcher,
                mHashHelper,
                mLogger,
                mMeshtasticDevice,
                mLongName,
                mShortName,
                mRegionCode,
                ForwarderConstants.POSITION_BROADCAST_INTERVAL_S,
                ForwarderConstants.GPS_ENABLED,
                ForwarderConstants.LCD_SCREEN_ON_S,
                mChannelName,
                mChannelMode,
                mChannelPsk,
                mRoutingRole,
                mPluginManagesDevice);
    }

    private void maybeCancelAndRemoveActiveCommConfigurator() {
        if (mActiveCommConfigurator != null) {
            mActiveCommConfigurator.cancel();
            mActiveCommConfigurator.removeListener(this);
        }
    }

    private void setActiveCommConfigurator(MeshDeviceConfigurator meshDeviceConfigurator, ConfigurationState configurationState) {
        mActiveCommConfigurator = meshDeviceConfigurator;
        meshDeviceConfigurator.addListener(this);
        notifyListeners(configurationState);
        meshDeviceConfigurator.start();
    }

    private void notifyListeners(ConfigurationState configurationState) {
        for (Listener listener : mListeners) {
            listener.onConfigurationStateChanged(configurationState);
        }
    }
}
