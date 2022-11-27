package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.geeksville.mesh.ConfigProtos;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshDeviceConfigurationController implements MeshConnectionHandler.Listener, DeviceConfigObserver.Listener, MeshServiceController.Listener, MeshDeviceConfigurator.Listener {
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
    private final MeshConnectionHandler mMeshConnectionHandler;
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

    private boolean mSetDeviceAddressCalled;

    private MeshDeviceConfigurator mActiveConfigurator;
    private MeshDeviceConfigurator mStagedTrackerConfigurator;
    private MeshDeviceConfigurator mStagedCommConfigurator;

    public MeshDeviceConfigurationController(MeshServiceController meshServiceController,
                                             MeshConnectionHandler meshConnectionHandler,
                                             MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                             MeshDeviceConfiguratorFactory meshDeviceConfiguratorFactory,
                                             DeviceConfigObserver deviceConfigObserver,
                                             HashHelper hashHelper,
                                             Gson gson,
                                             Logger logger,
                                             SharedPreferences sharedPreferences,
                                             @Nullable MeshtasticDevice meshtasticDevice,
                                             String callsign) {
        mMeshServiceController = meshServiceController;
        mMeshConnectionHandler = meshConnectionHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mMeshDeviceConfiguratorFactory = meshDeviceConfiguratorFactory;
        mHashHelper = hashHelper;
        mLogger = logger;
        mMeshtasticDevice = meshtasticDevice;
        mCallsign = callsign;

        meshServiceController.addListener(this);
        meshConnectionHandler.addListener(this);
        deviceConfigObserver.addListener(this);

        mRegionCode = ConfigProtos.Config.LoRaConfig.RegionCode.forNumber(Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_REGION, PreferencesDefaults.DEFAULT_REGION)));
        mChannelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
        mChannelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
        mChannelPsk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);
        mRoutingRole = sharedPreferences.getBoolean(PreferencesKeys.KEY_COMM_DEVICE_IS_ROUTER, PreferencesDefaults.DEFAULT_COMM_DEVICE_IS_ROUTER) ? ConfigProtos.Config.DeviceConfig.Role.ROUTER_CLIENT : ConfigProtos.Config.DeviceConfig.Role.CLIENT;
    }

    @Override
    public void onServiceConnectionStateChanged(MeshServiceController.ServiceConnectionState serviceConnectionState) {
        // TODO: is there a timing issue here?
        if (serviceConnectionState == MeshServiceController.ServiceConnectionState.CONNECTED && !mSetDeviceAddressCalled) {
            if (mMeshtasticDevice == null) {
                return;
            }

            try {
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
    public void onDeviceConnectionStateChanged(MeshConnectionHandler.DeviceConnectionState deviceConnectionState) {
        if (deviceConnectionState != MeshConnectionHandler.DeviceConnectionState.CONNECTED) {
            return;
        }

        String meshId = "00000";
        try {
            meshId = mMeshServiceController.getMeshService().getMyId();
        } catch (RemoteException e) {
            mLogger.e(TAG, "RemoteException calling getMyId(): " + e.getMessage());
            e.printStackTrace();
        }
        if (meshId == null) {
            meshId = "00000";
        }
        String shortMeshId = meshId.replaceAll("!", "").substring(meshId.length() - 5);
        mLongName = String.format("%s-MX-%s", mCallsign, shortMeshId);
        mShortName = mCallsign.substring(0, 1);
    }

    @Override
    public void onSelectedDeviceChanged(MeshtasticDevice meshtasticDevice) {
        mMeshtasticDevice = meshtasticDevice;

        MeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
                mMeshServiceController,
                mMeshConnectionHandler,
                mMeshtasticDeviceSwitcher,
                mHashHelper,
                mLogger,
                meshtasticDevice,
                mLongName,
                mShortName,
                mRegionCode,
                mChannelName,
                mChannelMode,
                mChannelPsk,
                mRoutingRole);

        if (mActiveConfigurator == null) {
            setActiveConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_COMM);
            return;
        }

        mStagedCommConfigurator = meshDeviceConfigurator;
    }

    @Override
    public void onDeviceConfigChanged(ConfigProtos.Config.LoRaConfig.RegionCode regionCode, String channelName, int channelMode, byte[] channelPsk, ConfigProtos.Config.DeviceConfig.Role routingRole) {
        mRegionCode = regionCode;
        mChannelName = channelName;
        mChannelMode = channelMode;
        mChannelPsk = channelPsk;
        mRoutingRole = routingRole;

        MeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
                mMeshServiceController,
                mMeshConnectionHandler,
                mMeshtasticDeviceSwitcher,
                mHashHelper,
                mLogger,
                mMeshtasticDevice,
                mLongName,
                mShortName,
                mRegionCode,
                mChannelName,
                mChannelMode,
                mChannelPsk,
                mRoutingRole);

        if (mActiveConfigurator == null) {
            setActiveConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_COMM);
            return;
        }

        mStagedCommConfigurator = meshDeviceConfigurator;
    }

    @Override
    public void onConfigurationStateChanged(MeshDeviceConfigurator.ConfigurationState configurationState) {
        if (configurationState == MeshDeviceConfigurator.ConfigurationState.FINISHED) {
            mActiveConfigurator.removeListener(this);
            mActiveConfigurator = null;

            if (mStagedTrackerConfigurator != null) {
                setActiveConfigurator(mStagedTrackerConfigurator, ConfigurationState.WRITING_TRACKER);
                mStagedTrackerConfigurator = null;
                return;
            }

            if (mStagedCommConfigurator != null) {
                setActiveConfigurator(mStagedCommConfigurator, ConfigurationState.WRITING_COMM);
                mStagedCommConfigurator = null;
                return;
            }

            notifyListeners(ConfigurationState.READY);
        }

        if (configurationState == MeshDeviceConfigurator.ConfigurationState.FAILED) {
            // TODO: max retries?
            mActiveConfigurator.start();
        }
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void writeTracker(MeshtasticDevice meshtasticDevice,
                             String longName,
                             String shortName,
                             ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                             String channelName,
                             int channelMode,
                             byte[] channelPsk,
                             ConfigProtos.Config.DeviceConfig.Role routingRole,
                             MeshDeviceConfigurator.Listener listener) {

        MeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
                mMeshServiceController,
                mMeshConnectionHandler,
                mMeshtasticDeviceSwitcher,
                mHashHelper,
                mLogger,
                meshtasticDevice,
                longName,
                shortName,
                regionCode,
                channelName,
                channelMode,
                channelPsk,
                routingRole);
        meshDeviceConfigurator.addListener(listener);

        if (mActiveConfigurator == null) {
            setActiveConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_TRACKER);
            return;
        }

        mStagedTrackerConfigurator = meshDeviceConfigurator;
    }

    private void setActiveConfigurator(MeshDeviceConfigurator meshDeviceConfigurator, ConfigurationState configurationState) {
        mActiveConfigurator = meshDeviceConfigurator;
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
