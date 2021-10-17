package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.SharedPreferences;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.geeksville.mesh.AppOnlyProtos;
import com.geeksville.mesh.ChannelProtos;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.RadioConfigProtos;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.ChannelConfig;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.ArrayList;
import java.util.List;

public class MeshDeviceConfigurer extends DestroyableSharedPrefsListener implements MeshServiceController.ConnectionStateListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshDeviceConfigurer.class.getSimpleName();

    private final SharedPreferences mSharedPreferences;
    private final MeshServiceController mMeshServiceController;
    private final MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private final HashHelper mHashHelper;
    private final Logger mLogger;
    private final String mCallsign;

    @Nullable
    private IMeshService mMeshService;

    @Nullable
    private MeshtasticDevice mMeshDevice;

    private RadioConfigProtos.RegionCode mRegionCode;

    private List<ChannelConfig> mChannelConfigs;

    private boolean mIsRouter;

    private boolean mSetDeviceAddressCalled;

    public MeshDeviceConfigurer(List<Destroyable> destroyables,
                                SharedPreferences sharedPreferences,
                                MeshServiceController meshServiceController,
                                MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                HashHelper hashHelper,
                                Logger logger,
                                String callsign) {
        super(destroyables,
                sharedPreferences,
                new String[] {},
                new String[]{
                        PreferencesKeys.KEY_SET_COMM_DEVICE,
                        PreferencesKeys.KEY_COMM_DEVICE_IS_ROUTER,
                        PreferencesKeys.KEY_CHANNEL_DATA,
                        PreferencesKeys.KEY_REGION
                });

        mSharedPreferences = sharedPreferences;
        mMeshServiceController = meshServiceController;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mHashHelper = hashHelper;
        mLogger = logger;
        mCallsign = callsign;

        meshServiceController.addConnectionStateListener(this);

        // TODO: clean up this hacks
        mIsRouter = sharedPreferences.getBoolean(PreferencesKeys.KEY_COMM_DEVICE_IS_ROUTER, PreferencesDefaults.DEFAULT_COMM_DEVICE_IS_ROUTER);
        mRegionCode = RadioConfigProtos.RegionCode.forNumber(Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_REGION, PreferencesDefaults.DEFAULT_REGION)));
        mChannelConfigs = new Gson().fromJson(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_DATA, PreferencesDefaults.DEFAULT_CHANNEL_DATA), new TypeToken<ArrayList<ChannelConfig>>() {}.getType());
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        // Do nothing
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferencesKeys.KEY_SET_COMM_DEVICE:
                String commDeviceStr = sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE);
                Gson gson = new Gson();
                MeshtasticDevice meshtasticDevice = gson.fromJson(commDeviceStr, MeshtasticDevice.class);

                if (meshtasticDevice == null) {
                    mLogger.v(TAG, "complexUpdate, no device configured, exiting");
                    return;
                }

                mMeshDevice = meshtasticDevice;

                if (mMeshService == null) {
                    mLogger.v(TAG, "complexUpdate, device configured but no service connection, exiting");
                    return;
                }

                try {
                    mLogger.v(TAG, "complexUpdate, calling setDeviceAddress: " + meshtasticDevice);
                    mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, meshtasticDevice);
                    mSetDeviceAddressCalled = true;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case PreferencesKeys.KEY_COMM_DEVICE_IS_ROUTER:
                boolean isRouter = sharedPreferences.getBoolean(PreferencesKeys.KEY_COMM_DEVICE_IS_ROUTER, PreferencesDefaults.DEFAULT_COMM_DEVICE_IS_ROUTER);

                if (isRouter != mIsRouter) {
                    mLogger.d(TAG, "Router config changed, isRouter: " + isRouter);
                    mIsRouter = isRouter;

                    checkRadioConfig();
                }
                break;
            case PreferencesKeys.KEY_REGION:
                RadioConfigProtos.RegionCode regionCode = RadioConfigProtos.RegionCode.forNumber(Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_REGION, PreferencesDefaults.DEFAULT_REGION)));

                if (regionCode != mRegionCode) {
                    mLogger.d(TAG, "Region config changed, new region: " + regionCode + ", checking if radio is up to date");
                    mRegionCode = regionCode;

                    checkRadioConfig();
                }
                break;
            case PreferencesKeys.KEY_CHANNEL_DATA:
                mLogger.v(TAG, "Channel config updated, checking if we need to update the radio");

                List<ChannelConfig> channelConfigs = new Gson().fromJson(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_DATA, PreferencesDefaults.DEFAULT_CHANNEL_DATA), new TypeToken<ArrayList<ChannelConfig>>() {}.getType());

                if (channelConfigs == null) {
                    mLogger.e(TAG, "Returning from complexUpdate on KEY_CHANNEL_DATA, channelConfigs was null, this should never happen!");
                    return;
                }

                boolean changed = false;

                if (channelConfigs.size() != mChannelConfigs.size()) {
                    mLogger.v(TAG, "  Channel count changed, need to check against radio");
                    changed = true;
                }

                for (ChannelConfig channelConfig : channelConfigs) {
                    for (ChannelConfig testChannelConfig : mChannelConfigs) {
                        if (channelConfig.name.equals(testChannelConfig.name)) {
                            if (!areByteArraysEqual(channelConfig.psk, testChannelConfig.psk)
                                    || channelConfig.modemConfig != testChannelConfig.modemConfig
                                    || channelConfig.isDefault != testChannelConfig.isDefault) {
                                mLogger.v(TAG, "  Channel config for " + channelConfig.name + " changed, need to check against radio");
                                changed = true;
                                break;
                            }
                        }
                    }
                }

                if (changed) {
                    mLogger.d(TAG, "channelConfig changed, checking if radio is up to date");
                    mChannelConfigs = channelConfigs;

                    checkChannelConfig();
                }
                break;
        }
    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {
        mMeshService = mMeshServiceController.getMeshService();
        if (connectionState == ConnectionState.NO_SERVICE_CONNECTION
                || connectionState == ConnectionState.NO_DEVICE_CONFIGURED) {
            mLogger.d(TAG, "onConnectionStateChanged: no service connection or no device configured");
            return;
        }

        if (!mSetDeviceAddressCalled) {
            mLogger.d(TAG, "onConnectionStateChanged: set device address not called yet, calling complexUpdate()");
            complexUpdate(mSharedPreferences, PreferencesKeys.KEY_SET_COMM_DEVICE);
        }

        if (connectionState == ConnectionState.DEVICE_CONNECTED) {
            checkOwner();
            checkRadioConfig();
            checkChannelConfig();
        }
    }

    private void checkRadioConfig() {
        mLogger.v(TAG, "  Checking radio config");
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        byte[] radioConfigBytes = null;
        try {
            radioConfigBytes = mMeshService.getRadioConfig();
        } catch (RemoteException e) {
            mLogger.e(TAG, "  checkRadioConfig() - RemoteException!");
            e.printStackTrace();
        }

        if (radioConfigBytes == null) {
            mLogger.e(TAG, "  checkRadioConfig(), radioConfig was null");
            return;
        }

        RadioConfigProtos.RadioConfig radioConfig = null;
        try {
            radioConfig = RadioConfigProtos.RadioConfig.parseFrom(radioConfigBytes);
        } catch (InvalidProtocolBufferException e) {
            mLogger.e(TAG, "  checkRadioConfig() - exception parsing radioConfig protobuf");
            e.printStackTrace();
            return;
        }

        RadioConfigProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();

        if (userPreferences.getRegion() != mRegionCode
                || userPreferences.getIsRouter() != mIsRouter
                || userPreferences.getPositionBroadcastSecs() != ForwarderConstants.POSITION_BROADCAST_INTERVAL_S
                || userPreferences.getScreenOnSecs() != ForwarderConstants.LCD_SCREEN_ON_S
                || userPreferences.getWaitBluetoothSecs() != ForwarderConstants.WAIT_BLUETOOTH_S
                || userPreferences.getPhoneTimeoutSecs() != ForwarderConstants.PHONE_TIMEOUT_S) {
            writeRadioConfig(radioConfig);
        }
    }

    private void checkChannelConfig() {
        mLogger.v(TAG, "  Checking channel config for channels: " + mChannelConfigs.size());
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        byte[] channelSetBytes = null;
        try {
            channelSetBytes = mMeshService.getChannels();
        } catch (RemoteException e) {
            mLogger.e(TAG, "  checkChannelConfig() - RemoteException!");
            e.printStackTrace();
        }

        if (channelSetBytes == null) {
            mLogger.e(TAG, "  checkChannelConfig(), channelSetBytes was null");
            return;
        }

        AppOnlyProtos.ChannelSet channelSet = null;
        try {
            channelSet = AppOnlyProtos.ChannelSet.parseFrom(channelSetBytes);
        } catch (InvalidProtocolBufferException e) {
            mLogger.e(TAG, "  checkChannelConfig() - exception parsing channelSet protobuf");
            e.printStackTrace();
            return;
        }

        for (int i = 0 ; i < channelSet.getSettingsCount() ; i++) {
            mLogger.v(TAG, "  channel from radio: " + channelSet.getSettings(i).getName());
        }

        boolean needsUpdate = false;
        for (ChannelConfig channelConfig : mChannelConfigs) {
            boolean found = false;
            for (int i = 0 ; i < channelSet.getSettingsCount() ; i++) {
                ChannelProtos.ChannelSettings channelSettings = channelSet.getSettings(i);
                if (channelConfig.name.equals(channelSettings.getName())) {
                    found = true;

                    if (!areByteArraysEqual(channelConfig.psk, channelSettings.getPsk().toByteArray()) || channelConfig.modemConfig != channelSettings.getModemConfigValue()) {
                        needsUpdate = true;
                        mLogger.v(TAG, "    channel: " + channelConfig.name + " changed!");
                        break;
                    }
                }
            }

            if (!found) {
                mLogger.v(TAG, "    channel: " + channelConfig.name + " not found on radio!");
                needsUpdate = true;
            }
        }

        mLogger.v(TAG, "    needsUpdate: " + needsUpdate);
        if (needsUpdate) {
            writeChannelConfig(channelSet);
        }
    }

    private void checkOwner() {
        mLogger.v(TAG, "  Checking owner");
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        try {
            String meshId = mMeshService.getMyId();
            String shortMeshId = meshId.replaceAll("!", "").substring(meshId.length() - 5);
            String longName = String.format("%s-MX-%s", mCallsign, shortMeshId);
            String shortName = mCallsign.substring(0, 1);
            mLogger.v(TAG, "  Setting radio owner longName: " + longName + ", shortName: " + shortName);
            mMeshService.setOwner(null, longName, shortName);
        } catch (RemoteException e) {
            mLogger.e(TAG, "checkOwner() -- RemoteException!");
            e.printStackTrace();
        }
    }

    private void writeRadioConfig(RadioConfigProtos.RadioConfig radioConfig) {
        mLogger.v(TAG, "  Writing radio config to device, region: " + mRegionCode + ", isRouter: " + mIsRouter);
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        RadioConfigProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();

        RadioConfigProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
        RadioConfigProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();

        userPreferencesBuilder.setGpsUpdateInterval(ForwarderConstants.GPS_UPDATE_INTERVAL);
        userPreferencesBuilder.setSendOwnerInterval(ForwarderConstants.SEND_OWNER_INTERVAL);
        userPreferencesBuilder.setLocationShare(RadioConfigProtos.LocationSharing.LocDisabled);
        userPreferencesBuilder.setPositionBroadcastSecs(ForwarderConstants.POSITION_BROADCAST_INTERVAL_S);
        userPreferencesBuilder.setScreenOnSecs(ForwarderConstants.LCD_SCREEN_ON_S);
        userPreferencesBuilder.setWaitBluetoothSecs(ForwarderConstants.WAIT_BLUETOOTH_S);
        userPreferencesBuilder.setPhoneTimeoutSecs(ForwarderConstants.PHONE_TIMEOUT_S);
        userPreferencesBuilder.setRegion(mRegionCode);
        userPreferencesBuilder.setIsRouter(mIsRouter);

        radioConfigBuilder.setPreferences(userPreferencesBuilder.build());

        radioConfig = radioConfigBuilder.build();

        try {
            mMeshService.setRadioConfig(radioConfig.toByteArray());
        } catch (RemoteException e) {
            mLogger.e(TAG, "writeRadioConfig() - exception while writing config");
            e.printStackTrace();
        }
    }

    private void writeChannelConfig(AppOnlyProtos.ChannelSet channelSet) {
        mLogger.v(TAG, "  Writing " + mChannelConfigs.size() + " channels to device");
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        AppOnlyProtos.ChannelSet.Builder channelSetBuilder = channelSet.toBuilder();

        channelSetBuilder.clearSettings();

        for (ChannelConfig channelConfig : mChannelConfigs) {
            mLogger.v(TAG, "    Adding channel: " + channelConfig.name);
            ChannelProtos.ChannelSettings.Builder channelSettingsBuilder = ChannelProtos.ChannelSettings.newBuilder();
            channelSettingsBuilder.setName(channelConfig.name);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(channelConfig.psk));
            channelSettingsBuilder.setModemConfig(ChannelProtos.ChannelSettings.ModemConfig.forNumber(channelConfig.modemConfig));

            if (channelConfig.isDefault) {
                channelSetBuilder.addSettings(0, channelSettingsBuilder.build());
            } else {
                channelSetBuilder.addSettings(channelSettingsBuilder.build());
            }
        }

        channelSet = channelSetBuilder.build();

        try {
            mMeshService.setChannels(channelSet.toByteArray());
        } catch (RemoteException e) {
            mLogger.e(TAG, "writeChannelConfig() - exception while writing channels");
            e.printStackTrace();
        }

        checkChannelConfig();
    }

    private boolean areByteArraysEqual(byte[] lhs, byte[] rhs) {
        if (lhs.length != rhs.length) {
            return false;
        }

        for (int i = 0 ; i < lhs.length ; i++) {
            if (lhs[i] != rhs[i]) {
                return false;
            }
        }
        return true;
    }
}
