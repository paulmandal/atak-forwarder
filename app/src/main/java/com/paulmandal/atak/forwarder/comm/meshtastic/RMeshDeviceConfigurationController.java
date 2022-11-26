package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.geeksville.mesh.ConfigProtos;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RMeshDeviceConfigurationController implements RMeshConnectionHandler.DeviceConnectionStateListener, RDeviceConfigObserver.DeviceConfigListener, RMeshServiceController.ServiceConnectionStateListener, RMeshDeviceConfigurator.ConfigurationStateListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + RMeshDeviceConfigurationController.class.getSimpleName();

    public enum ConfigurationState {
        DISCONNECTED,
        WRITING_TRACKER,
        WRITING_COMM,
        READY
    }

    public interface Listener {
        void onConfigurationStateChanged(ConfigurationState configurationState);
    }

    private final RMeshServiceController mMeshServiceController;
    private final RMeshConnectionHandler mMeshConnectionHandler;
    private final MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private final MeshDeviceConfiguratorFactory mMeshDeviceConfiguratorFactory;
    private final HashHelper mHashHelper;
    private final Logger mLogger;
    private final String mCallsign;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    @Nullable
    private MeshtasticDevice mMeshtasticDevice;
    private ConfigProtos.Config.DeviceConfig.Role mDeviceRole;
    private ConfigProtos.Config.LoRaConfig.RegionCode mRegionCode;

    private String mChannelName;
    private int mChannelMode;
    private byte[] mChannelPsk;

    private String mLongName;
    private String mShortName;

    private boolean mSetDeviceAddressCalled;

    private RMeshDeviceConfigurator mActiveConfigurator;
    private RMeshDeviceConfigurator mStagedTrackerConfigurator;
    private RMeshDeviceConfigurator mStagedCommConfigurator;

    public RMeshDeviceConfigurationController(RMeshServiceController meshServiceController,
                                              RMeshConnectionHandler meshConnectionHandler,
                                              MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                              @Nullable MeshtasticDevice meshtasticDevice,
                                              MeshDeviceConfiguratorFactory meshDeviceConfiguratorFactory,
                                              RDeviceConfigObserver deviceConfigObserver,
                                              HashHelper hashHelper,
                                              Logger logger,
                                              String callsign) {
        mMeshServiceController = meshServiceController;
        mMeshConnectionHandler = meshConnectionHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mMeshtasticDevice = meshtasticDevice;
        mMeshDeviceConfiguratorFactory = meshDeviceConfiguratorFactory;
        mHashHelper = hashHelper;
        mLogger = logger;
        mCallsign = callsign;

        meshServiceController.addConnectionStateListener(this);
        meshConnectionHandler.addListener(this);
        deviceConfigObserver.addListener(this);
    }

    @Override
    public void onServiceConnectionStateChanged(RMeshServiceController.ServiceConnectionState serviceConnectionState) {
        if (serviceConnectionState == RMeshServiceController.ServiceConnectionState.CONNECTED && !mSetDeviceAddressCalled) {
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
    public void onDeviceConnectionStateChanged(RMeshConnectionHandler.DeviceConnectionState deviceConnectionState) {
        if (deviceConnectionState != RMeshConnectionHandler.DeviceConnectionState.CONNECTED) {
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

        RMeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
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
                mDeviceRole);

        if (mActiveConfigurator == null) {
            setActiveConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_COMM);
            return;
        }

        mStagedCommConfigurator = meshDeviceConfigurator;
    }

    @Override
    public void onDeviceConfigChanged(ConfigProtos.Config.LoRaConfig.RegionCode regionCode, String channelName, int channelMode, byte[] channelPsk, ConfigProtos.Config.DeviceConfig.Role deviceRole) {
        mRegionCode = regionCode;
        mChannelName = channelName;
        mChannelMode = channelMode;
        mChannelPsk = channelPsk;
        mDeviceRole = deviceRole;

        RMeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
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
                mDeviceRole);

        if (mActiveConfigurator == null) {
            setActiveConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_COMM);
            return;
        }

        mStagedCommConfigurator = meshDeviceConfigurator;
    }

    @Override
    public void onConfigurationStateChanged(RMeshDeviceConfigurator.ConfigurationState configurationState) {
        if (configurationState == RMeshDeviceConfigurator.ConfigurationState.FINISHED) {
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

        if (configurationState == RMeshDeviceConfigurator.ConfigurationState.FAILED) {
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
                        ConfigProtos.Config.DeviceConfig.Role deviceRole) {

        RMeshDeviceConfigurator meshDeviceConfigurator = mMeshDeviceConfiguratorFactory.createMeshDeviceConfigurator(
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
                deviceRole);

        if (mActiveConfigurator == null) {
            setActiveConfigurator(meshDeviceConfigurator, ConfigurationState.WRITING_TRACKER);
            return;
        }

        mStagedTrackerConfigurator = meshDeviceConfigurator;
    }

    private void setActiveConfigurator(RMeshDeviceConfigurator meshDeviceConfigurator, ConfigurationState configurationState) {
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
