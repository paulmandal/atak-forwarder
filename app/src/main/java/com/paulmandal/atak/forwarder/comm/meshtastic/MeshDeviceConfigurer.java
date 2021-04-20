package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.geeksville.mesh.AppOnlyProtos;
import com.geeksville.mesh.ChannelProtos;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.RadioConfigProtos;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

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

    private String mChannelName;
    private int mChannelMode;
    private byte[] mChannelPsk;

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
                        PreferencesKeys.KEY_CHANNEL_NAME,
                        PreferencesKeys.KEY_CHANNEL_MODE,
                        PreferencesKeys.KEY_CHANNEL_PSK
                });

        mSharedPreferences = sharedPreferences;
        mMeshServiceController = meshServiceController;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mHashHelper = hashHelper;
        mLogger = logger;
        mCallsign = callsign;

        meshServiceController.addConnectionStateListener(this);

        // TODO: clean up this hacks
        mRegionCode = RadioConfigProtos.RegionCode.forNumber(Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_REGION, PreferencesDefaults.DEFAULT_REGION)));
        mChannelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
        mChannelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
        mChannelPsk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);
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

                try {
                    mLogger.v(TAG, "complexUpdate, calling setDeviceAddress: " + meshtasticDevice);
                    mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, meshtasticDevice);
                    mMeshDevice = meshtasticDevice;
                    mSetDeviceAddressCalled = true;
                } catch (RemoteException e) {
                    e.printStackTrace();
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
            case PreferencesKeys.KEY_CHANNEL_NAME:
            case PreferencesKeys.KEY_CHANNEL_MODE:
            case PreferencesKeys.KEY_CHANNEL_PSK:
                String channelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
                int channelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
                byte[] psk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);

                boolean changed = !channelName.equals(mChannelName) || channelMode != mChannelMode || !areByteArraysEqual(psk, mChannelPsk);

                if (changed) {
                    mLogger.d(TAG, "channelConfig changed, checking if radio is up to date");
                    mChannelName = channelName;
                    mChannelMode = channelMode;
                    mChannelPsk = psk;

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

        if (userPreferences.getPositionBroadcastSecs() != ForwarderConstants.POSITION_BROADCAST_INTERVAL_S
                || userPreferences.getScreenOnSecs() != ForwarderConstants.LCD_SCREEN_ON_S
                || userPreferences.getWaitBluetoothSecs() != ForwarderConstants.WAIT_BLUETOOTH_S
                || userPreferences.getPhoneTimeoutSecs() != ForwarderConstants.PHONE_TIMEOUT_S) {
            writeRadioConfig(radioConfig);
        }
    }

    private void checkChannelConfig() {
        mLogger.v(TAG, "  Checking channel config");
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

        ChannelProtos.ChannelSettings.ModemConfig modemConfig = ChannelProtos.ChannelSettings.ModemConfig.forNumber(mChannelMode);

        List<ChannelProtos.ChannelSettings> channelSettings = channelSet.getSettingsList();

        boolean needsUpdate = true;
        for (ChannelProtos.ChannelSettings channelSetting : channelSettings) {
            if (!mChannelName.equals(channelSetting.getName())) {
                continue;
            }

            needsUpdate = !(areByteArraysEqual(channelSetting.getPsk().toByteArray(), mChannelPsk) && channelSetting.getModemConfig() == modemConfig);
        }

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
        mLogger.v(TAG, "  Writing radio config to device, region: " + mRegionCode);
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        RadioConfigProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();

        RadioConfigProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
        RadioConfigProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();

        userPreferencesBuilder.setPositionBroadcastSecs(ForwarderConstants.POSITION_BROADCAST_INTERVAL_S);
        userPreferencesBuilder.setScreenOnSecs(ForwarderConstants.LCD_SCREEN_ON_S);
        userPreferencesBuilder.setWaitBluetoothSecs(ForwarderConstants.WAIT_BLUETOOTH_S);
        userPreferencesBuilder.setPhoneTimeoutSecs(ForwarderConstants.PHONE_TIMEOUT_S);
        userPreferencesBuilder.setRegion(mRegionCode);

        radioConfigBuilder.setPreferences(userPreferencesBuilder);

        radioConfig = radioConfigBuilder.build();

        try {
            mMeshService.setRadioConfig(radioConfig.toByteArray());
        } catch (RemoteException e) {
            mLogger.e(TAG, "writeRadioConfig() - exception while writing config");
            e.printStackTrace();
        }
    }

    private void writeChannelConfig(AppOnlyProtos.ChannelSet channelSet) {
        mLogger.v(TAG, "  Writing channel config to device, name: " + mChannelName + ", mode: " + mChannelMode + ", psk: " + mHashHelper.hashFromBytes(mChannelPsk));
        if (mMeshService == null) {
            mLogger.v(TAG, "  Not connected to MeshService");
            return;
        }

        AppOnlyProtos.ChannelSet.Builder channelSetBuilder = channelSet.toBuilder();

        ChannelProtos.ChannelSettings.ModemConfig modemConfig = ChannelProtos.ChannelSettings.ModemConfig.forNumber(mChannelMode);
        ChannelProtos.ChannelSettings.Builder channelSettingsBuilder = ChannelProtos.ChannelSettings.newBuilder();
        channelSettingsBuilder.setName(mChannelName);
        channelSettingsBuilder.setPsk(ByteString.copyFrom(mChannelPsk));
        channelSettingsBuilder.setModemConfig(modemConfig);

        channelSettingsBuilder.setModemConfig(ChannelProtos.ChannelSettings.ModemConfig.forNumber(mChannelMode));

        int indexToRemove = -1;

        for (int i = 0 ; i < channelSet.getSettingsCount() ; i++) {
            ChannelProtos.ChannelSettings channelSetting = channelSet.getSettings(i);
            if (!mChannelName.equals(channelSetting.getName())) {
                continue;
            }

            indexToRemove = i;
        }

        if (indexToRemove != -1) {
            channelSetBuilder.removeSettings(indexToRemove);
        }

        channelSetBuilder.addSettings(channelSettingsBuilder.build());
        channelSet = channelSetBuilder.build();

        try {
            mMeshService.setChannels(channelSet.toByteArray());
        } catch (RemoteException e) {
            mLogger.e(TAG, "writeChannelConfig() - exception while writing channels");
            e.printStackTrace();
        }
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
