package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.atakmap.math.Mesh;
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
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RMeshDeviceConfigurator implements RMeshConnectionHandler.DeviceConnectionStateListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + RMeshDeviceConfigurator.class.getSimpleName();

    public enum ConfigurationState {
        CHECKING,
        WRITING,
        FINISHED
    }

    public interface ConfigurationStateListener {
        void onConfigurationStateChanged(ConfigurationState configurationState);
    }

    private final RMeshServiceController mMeshServiceController;
    private final Logger mLogger;

    private final String mLongName;
    private final String mShortName;

    private final ConfigProtos.Config.LoRaConfig.RegionCode mRegionCode;

    private final String mChannelName;
    private final int mChannelMode;
    private final byte[] mChannelPsk;

    private final ConfigProtos.Config.DeviceConfig.Role mDeviceRole;

    private final Set<ConfigurationStateListener> mListeners = new CopyOnWriteArraySet<>();

    private boolean mConfigurationChecked;
    private boolean mConfigurationWritten;

    public RMeshDeviceConfigurator(RMeshConnectionHandler meshConnectionHandler,
                                   Logger logger,
                                   RMeshServiceController meshServiceController,
                                   String longName,
                                   String shortName,
                                   ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                                   String channelName,
                                   int channelMode,
                                   byte[] channelPsk,
                                   ConfigProtos.Config.DeviceConfig.Role deviceRole) {
        mMeshServiceController = meshServiceController;
        mLogger = logger;
        mLongName = longName;
        mShortName = shortName;
        mRegionCode = regionCode;
        mChannelName = channelName;
        mChannelMode = channelMode;
        mChannelPsk = channelPsk;
        mDeviceRole = deviceRole;

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
        try {
            IMeshService meshService = mMeshServiceController.getMeshService();
            LocalOnlyProtos.LocalConfig localConfig = LocalOnlyProtos.LocalConfig.parseFrom(meshService.getConfig());

            ConfigProtos.Config.LoRaConfig loRaConfig = localConfig.getLora();
            ConfigProtos.Config.DeviceConfig deviceConfig = localConfig.getDevice();
            if (mRegionCode != loRaConfig.getRegion()
                || mChannelMode != loRaConfig.getModemPresetValue()
                || !loRaConfig.getTxEnabled()
                || mDeviceRole != deviceConfig.getRole()) {

                ConfigProtos.Config.Builder configBuilder = ConfigProtos.Config.newBuilder();

                ConfigProtos.Config.DeviceConfig.Builder deviceConfigBuilder = ConfigProtos.Config.DeviceConfig.newBuilder();
                deviceConfigBuilder.setRole(mDeviceRole);
                configBuilder.setDevice(deviceConfigBuilder);

                ConfigProtos.Config.LoRaConfig.Builder loRaConfigBuilder = ConfigProtos.Config.LoRaConfig.newBuilder();
                loRaConfigBuilder.setRegion(mRegionCode);
                ConfigProtos.Config.LoRaConfig.ModemPreset modemPreset = ConfigProtos.Config.LoRaConfig.ModemPreset.forNumber(mChannelMode);
                loRaConfigBuilder.setModemPreset(modemPreset);
                loRaConfigBuilder.setTxEnabled(true);
                configBuilder.setLora(loRaConfigBuilder);

                ConfigProtos.Config config = configBuilder.build();

                meshService.setConfig(config.toByteArray());
                return true;
            }

            int nodeNum = meshService.getMyNodeInfo().getMyNodeNum();

            NodeInfo localNode = null;
            List<NodeInfo> nodes = meshService.getNodes();
            for (int i = 0 ; i < nodes.size() ; i++) {
                NodeInfo node = nodes.get(i);
                if (node.getNum() == nodeNum) {
                    localNode = node;
                }
            }

            if (localNode == null) {
                // TODO: error handling
                return true; // TODO: return code? or enum?
            }

            MeshUser meshUser = localNode.getUser();

            if (meshUser == null) {
                // TODO: error handling
                return true; // TODO: return code? or enum?
            }

            String longName = meshUser.getLongName();
            String shortName = meshUser.getShortName();

            if (!mLongName.equals(longName) || !mShortName.equals(shortName)) {
                meshService.setOwner(null, mLongName, mShortName, false);
                return true;
            }

            ChannelProtos.Channel.Builder channelBuilder = ChannelProtos.Channel.newBuilder();

            ChannelProtos.ChannelSettings.Builder channelSettingsBuilder = ChannelProtos.ChannelSettings.newBuilder();
            channelSettingsBuilder.setName(mChannelName);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(mChannelPsk));

            channelBuilder.setSettings(channelSettingsBuilder);
            channelBuilder.setRole(ChannelProtos.Channel.Role.PRIMARY);

            ChannelProtos.Channel channel = channelBuilder.build();

            meshService.setChannel(channel.toByteArray());

            // TODO: we don't have to do this in parts we can start a transaction and commit everything at the end

            mConfigurationWritten = true;
            mConfigurationChecked = true;
            return false;
        } catch (RemoteException | InvalidProtocolBufferException e) {
            mLogger.e(TAG, "Error getting/parsing config protocol buffer: " + e.getMessage());
            e.printStackTrace();
        }

        // TODO: error handling for try/catch
        // TODO: notify listeners
    }

    private void notifyListeners(ConfigurationState configurationState) {
        for (ConfigurationStateListener listener : mListeners) {
            listener.onConfigurationStateChanged(configurationState);
        }
    }
}
