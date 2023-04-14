package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.os.RemoteException;

import com.geeksville.mesh.AppOnlyProtos;
import com.geeksville.mesh.ChannelProtos;
import com.geeksville.mesh.ConfigProtos;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.LocalOnlyProtos;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.MeshUser;
import com.geeksville.mesh.NodeInfo;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.helpers.PskHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshDeviceConfigurator implements DeviceConnectionHandler.Listener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshDeviceConfigurator.class.getSimpleName();

    public enum ConfigurationState {
        STARTED,
        FINISHED,
        FAILED
    }

    public interface Listener {
        void onConfigurationStateChanged(ConfigurationState configurationState);
    }

    private final MeshServiceController mMeshServiceController;
    private final DeviceConnectionHandler mDeviceConnectionHandler;
    private final MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private final HashHelper mHashHelper;
    private final Logger mLogger;

    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    private final MeshtasticDevice mMeshtasticDevice;

    private final String mLongName;
    private final String mShortName;

    private final ConfigProtos.Config.LoRaConfig.RegionCode mRegionCode;
    private final int mPliUpdateInterval;
    private final boolean mGpsEnabled;
    private final int mScreenOnSecs;

    private final String mChannelName;
    private final int mChannelMode;
    private final byte[] mChannelPsk;

    private final ConfigProtos.Config.DeviceConfig.Role mRoutingRole;

    private final boolean mWriteToDevice;

    private boolean mStarted;

    public MeshDeviceConfigurator(MeshServiceController meshServiceController,
                                  DeviceConnectionHandler deviceConnectionHandler,
                                  MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                  HashHelper hashHelper,
                                  Logger logger,
                                  MeshtasticDevice meshtasticDevice,
                                  String longName,
                                  String shortName,
                                  ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                                  int pliUpdateInterval,
                                  boolean gpsEnabled,
                                  int screenOnSecs,
                                  String channelName,
                                  int channelMode,
                                  byte[] channelPsk,
                                  ConfigProtos.Config.DeviceConfig.Role routingRole,
                                  boolean writeToDevice) {
        mMeshServiceController = meshServiceController;
        mDeviceConnectionHandler = deviceConnectionHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mHashHelper = hashHelper;
        mLogger = logger;
        mMeshtasticDevice = meshtasticDevice;
        mLongName = longName;
        mShortName = shortName;
        mRegionCode = regionCode;
        mPliUpdateInterval = pliUpdateInterval;
        mGpsEnabled = gpsEnabled;
        mScreenOnSecs = screenOnSecs;
        mChannelName = channelName;
        mChannelMode = channelMode;
        mChannelPsk = channelPsk;
        mRoutingRole = routingRole;
        mWriteToDevice = writeToDevice;
    }

    @Override
    public void onDeviceConnectionStateChanged(DeviceConnectionHandler.DeviceConnectionState deviceConnectionState) {
        if (deviceConnectionState == DeviceConnectionHandler.DeviceConnectionState.CONNECTED) {
            if (!mStarted) {
                mStarted = true;
                notifyListeners(ConfigurationState.STARTED);
            }
            maybeWriteConfig();
        }
    }

    public void start() {
        mStarted = false;

        try {
            mLogger.v(TAG, "Calling setDeviceAddress: " + mMeshtasticDevice.address);
            mDeviceConnectionHandler.addListener(this);
            mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshServiceController.getMeshService(), mMeshtasticDevice);
        } catch (RemoteException e) {
            mLogger.e(TAG, "Error attempting to switch device: " + e.getMessage());
            e.printStackTrace();
            sendFailed();
        }
    }

    public void cancel() {
        mDeviceConnectionHandler.removeListener(this);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void maybeWriteConfig() {
        if (!mWriteToDevice) {
            sendFinished();
            return;
        }

        try {
            mLogger.v(TAG, "Checking existing device config.");
            IMeshService meshService = mMeshServiceController.getMeshService();

            LocalOnlyProtos.LocalConfig localConfig = LocalOnlyProtos.LocalConfig.parseFrom(meshService.getConfig());
            AppOnlyProtos.ChannelSet channelSet = AppOnlyProtos.ChannelSet.parseFrom(meshService.getChannelSet());

            ConfigProtos.Config.LoRaConfig loRaConfig = localConfig.getLora();
            ConfigProtos.Config.DeviceConfig deviceConfig = localConfig.getDevice();
            boolean needsMainConfig = mRegionCode != loRaConfig.getRegion()
                    || mChannelMode != loRaConfig.getModemPresetValue()
                    || !loRaConfig.getTxEnabled()
                    || mRoutingRole != deviceConfig.getRole();

            boolean needsChannelConfig = channelSet.getSettingsCount() < 1;

            if (channelSet.getSettingsCount() > 0) {
                ChannelProtos.ChannelSettings channelSettings = channelSet.getSettings(0);
                byte[] currentChannelPsk = channelSettings.getPsk().toByteArray();
                needsChannelConfig = !Arrays.equals(mChannelPsk, currentChannelPsk) || !mChannelName.equals(channelSettings.getName());
            }

            if (needsMainConfig) {
                mLogger.d(TAG, "regionCode: " + loRaConfig.getRegion() + " -> " + mRegionCode + ", channelMode: " + loRaConfig.getModemPresetValue() + " -> " + mChannelMode + ", txEnabled: " + loRaConfig.getTxEnabled() + " -> true, routingRole: " + deviceConfig.getRole() + " -> " + mRoutingRole);
            }

            if (needsChannelConfig) {
                mLogger.d(TAG, "channelName: " + channelSettings.getName() + " -> " + mChannelName + ", channelPsk: " + mHashHelper.hashFromBytes(currentChannelPsk) + " -> " + mHashHelper.hashFromBytes(mChannelPsk));
            }

            if (!needsMainConfig) {
                int nodeNum = meshService.getMyNodeInfo().getMyNodeNum();

                NodeInfo localNode = null;
                List<NodeInfo> nodes = meshService.getNodes();
                for (int i = 0; i < nodes.size(); i++) {
                    NodeInfo node = nodes.get(i);
                    if (node.getNum() == nodeNum) {
                        localNode = node;
                    }
                }

                if (localNode == null) {
                    sendFailed();
                    return;
                }

                MeshUser meshUser = localNode.getUser();

                if (meshUser == null) {
                    sendFailed();
                    return;
                }

                String longName = meshUser.getLongName();
                String shortName = meshUser.getShortName();

                if (!mLongName.equals(longName) || !mShortName.equals(shortName)) {
                    mLogger.d(TAG, "longName: " + longName + " -> " + mLongName + ", shortName: " + shortName + " -> " + mShortName);
                    needsMainConfig = true;
                }
            }

            if (!needsMainConfig && !needsChannelConfig) {
                mLogger.v(TAG, "Finished writing to device.");
                sendFinished();
                return;
            }

            meshService.beginEditSettings();

            if (needsMainConfig) {
                writeMainConfig(meshService);
            }

            if (needsChannelConfig) {
                writeChannelConfig(meshService);
            }

            meshService.commitEditSettings();
        } catch (RemoteException | InvalidProtocolBufferException e) {
            mLogger.e(TAG, "Error getting/parsing config protocol buffer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeMainConfig(IMeshService meshService) throws RemoteException {
        mLogger.d(TAG, "Writing config to device: " + mMeshtasticDevice.address + ", longName: " + mLongName + ", shortName: " + mShortName + ", role: " + mRoutingRole + ", regionCode: " + mRegionCode + ", channelMode: " + mChannelMode + ".");

        ConfigProtos.Config.Builder configBuilder = ConfigProtos.Config.newBuilder();

        ConfigProtos.Config.DeviceConfig.Builder deviceConfigBuilder = ConfigProtos.Config.DeviceConfig.newBuilder();
        deviceConfigBuilder.setRole(mRoutingRole);
        deviceConfigBuilder.setSerialEnabled(true);
        configBuilder.setDevice(deviceConfigBuilder);
        meshService.setConfig(configBuilder.build().toByteArray());

        configBuilder = ConfigProtos.Config.newBuilder();
        ConfigProtos.Config.LoRaConfig.Builder loRaConfigBuilder = ConfigProtos.Config.LoRaConfig.newBuilder();
        loRaConfigBuilder.setRegion(mRegionCode);
        ConfigProtos.Config.LoRaConfig.ModemPreset modemPreset = ConfigProtos.Config.LoRaConfig.ModemPreset.forNumber(mChannelMode);
        loRaConfigBuilder.setModemPreset(modemPreset);
        loRaConfigBuilder.setUsePreset(true);
        loRaConfigBuilder.setHopLimit(3);
        loRaConfigBuilder.setTxEnabled(true);
        configBuilder.setLora(loRaConfigBuilder);
        meshService.setConfig(configBuilder.build().toByteArray());

        configBuilder = ConfigProtos.Config.newBuilder();
        ConfigProtos.Config.PositionConfig.Builder positionConfigBuilder = ConfigProtos.Config.PositionConfig.newBuilder();
        positionConfigBuilder.setPositionBroadcastSecs(mPliUpdateInterval);
        positionConfigBuilder.setGpsEnabled(mGpsEnabled);
        positionConfigBuilder.setPositionBroadcastSmartEnabled(false);
        positionConfigBuilder.setGpsUpdateInterval(mPliUpdateInterval);
        positionConfigBuilder.setGpsAttemptTime(ForwarderConstants.GPS_ATTEMPT_TIME);
        positionConfigBuilder.setPositionFlags(ConfigProtos.Config.PositionConfig.PositionFlags.ALTITUDE_VALUE);
        configBuilder.setPosition(positionConfigBuilder);
        meshService.setConfig(configBuilder.build().toByteArray());

        configBuilder = ConfigProtos.Config.newBuilder();
        ConfigProtos.Config.DisplayConfig.Builder displayConfigBuilder = ConfigProtos.Config.DisplayConfig.newBuilder();
        displayConfigBuilder.setScreenOnSecs(mScreenOnSecs);
        configBuilder.setDisplay(displayConfigBuilder);
        meshService.setConfig(configBuilder.build().toByteArray());

        meshService.setOwner(new MeshUser(null, mLongName, mShortName, MeshProtos.HardwareModel.UNSET, false));
    }

    private void writeChannelConfig(IMeshService meshService) throws RemoteException {
        mLogger.d(TAG, "Writing channel to device: " + mMeshtasticDevice.address + ", channelName: " + mChannelName + ", psk: " + mHashHelper.hashFromBytes(mChannelPsk) + ".");

        ChannelProtos.Channel.Builder channelBuilder = ChannelProtos.Channel.newBuilder();

        ChannelProtos.ChannelSettings.Builder channelSettingsBuilder = ChannelProtos.ChannelSettings.newBuilder();
        channelSettingsBuilder.setName(mChannelName);
        channelSettingsBuilder.setPsk(ByteString.copyFrom(mChannelPsk));

        channelBuilder.setSettings(channelSettingsBuilder);
        channelBuilder.setRole(ChannelProtos.Channel.Role.PRIMARY);

        ChannelProtos.Channel channel = channelBuilder.build();

        meshService.setChannel(channel.toByteArray());
    }

    private void sendFinished() {
        mDeviceConnectionHandler.removeListener(this);
        notifyListeners(ConfigurationState.FINISHED);
    }

    private void sendFailed() {
        mDeviceConnectionHandler.removeListener(this);
        notifyListeners(ConfigurationState.FAILED);
    }

    private void notifyListeners(ConfigurationState configurationState) {
        for (Listener listener : mListeners) {
            listener.onConfigurationStateChanged(configurationState);
        }
    }
}
