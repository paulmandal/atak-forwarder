package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
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
    private final Logger mLogger;
    private final String mCallsign;

    @Nullable
    private IMeshService mMeshService;

    @Nullable
    private MeshtasticDevice mMeshDevice;

    private String mChannelName;
    private int mChannelMode;
    private byte[] mChannelPsk;

    private boolean mSetDeviceAddressCalled;

    public MeshDeviceConfigurer(List<Destroyable> destroyables,
                                SharedPreferences sharedPreferences,
                                MeshServiceController meshServiceController,
                                MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
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
        mLogger = logger;
        mCallsign = callsign;

        meshServiceController.addConnectionStateListener(this);

        // TODO: clean up this hacks
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
        new Thread(() -> {
            switch (key) {
                case PreferencesKeys.KEY_SET_COMM_DEVICE:
                    String commDeviceStr = sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE);
                    Gson gson = new Gson();
                    MeshtasticDevice meshtasticDevice = gson.fromJson(commDeviceStr, MeshtasticDevice.class);

                    if (meshtasticDevice == null) {
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
                case PreferencesKeys.KEY_CHANNEL_NAME:
                case PreferencesKeys.KEY_CHANNEL_MODE:
                case PreferencesKeys.KEY_CHANNEL_PSK:
                    String channelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
                    int channelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
                    byte[] psk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);

                    boolean changed = !channelName.equals(mChannelName) || channelMode != mChannelMode || !compareByteArrays(psk, mChannelPsk);

                    if (changed) {
                        mChannelName = channelName;
                        mChannelMode = channelMode;
                        mChannelPsk = psk;

                        checkRadioConfig();
                    }
                    break;
            }
        }).start();
    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {
        mMeshService = mMeshServiceController.getMeshService();
        if (connectionState == ConnectionState.NO_SERVICE_CONNECTION
                || connectionState == ConnectionState.NO_DEVICE_CONFIGURED) {
            return;
        }

        if (!mSetDeviceAddressCalled) {
            complexUpdate(mSharedPreferences, PreferencesKeys.KEY_SET_COMM_DEVICE);
        }

        checkOwner();
        checkRadioConfig();
    }

    private void checkRadioConfig() {
        mLogger.v(TAG, "  Checking radio config");
        byte[] radioConfigBytes = null;
        try {
            radioConfigBytes = mMeshService.getRadioConfig();
        } catch (RemoteException e) {
            mLogger.e(TAG, "  checkRadioConfig() - RemoteException");
            e.printStackTrace();
        }

        if (radioConfigBytes == null) {
            mLogger.e(TAG, "  checkRadioConfig(), radioConfig was null");
            return;
        }

        MeshProtos.RadioConfig radioConfig = null;
        try {
            radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
        } catch (InvalidProtocolBufferException e) {
            mLogger.e(TAG, "  checkRadioConfig() - exception parsing radioConfig protobuf");
            e.printStackTrace();
        }

        MeshProtos.ChannelSettings.ModemConfig modemConfig = MeshProtos.ChannelSettings.ModemConfig.forNumber(mChannelMode);

        MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
        MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

        if (userPreferences.getPositionBroadcastSecs() != ForwarderConstants.POSITION_BROADCAST_INTERVAL_S
                || userPreferences.getScreenOnSecs() != ForwarderConstants.LCD_SCREEN_ON_S
                || userPreferences.getWaitBluetoothSecs() != ForwarderConstants.WAIT_BLUETOOTH_S
                || userPreferences.getPhoneTimeoutSecs() != ForwarderConstants.PHONE_TIMEOUT_S
                || !channelSettings.getName().equals(mChannelName)
                || !compareByteArrays(channelSettings.getPsk().toByteArray(), mChannelPsk)
                || channelSettings.getModemConfig() != modemConfig) {
            writeRadioConfig(radioConfig);
        }
    }

    private void checkOwner() {
        try {
            String meshId = mMeshService.getMyId();
            String shortMeshId = meshId.replaceAll("!", "").substring(meshId.length() - 5);
            mMeshService.setOwner(null, String.format("%s-MX-%s", mCallsign, shortMeshId), mCallsign.substring(0, 1));
        } catch (RemoteException e) {
            mLogger.e(TAG, "checkOwner() -- RemoteException!");
            e.printStackTrace();
        }
    }

    private void writeRadioConfig(MeshProtos.RadioConfig radioConfig) {
        mLogger.v(TAG, "  Writing radio config to device");
        MeshProtos.ChannelSettings.ModemConfig modemConfig = MeshProtos.ChannelSettings.ModemConfig.forNumber(mChannelMode);

        MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
        MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

        MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
        MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
        MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

        userPreferencesBuilder.setPositionBroadcastSecs(ForwarderConstants.POSITION_BROADCAST_INTERVAL_S);
        userPreferencesBuilder.setScreenOnSecs(ForwarderConstants.LCD_SCREEN_ON_S);
        userPreferencesBuilder.setWaitBluetoothSecs(ForwarderConstants.WAIT_BLUETOOTH_S);
        userPreferencesBuilder.setPhoneTimeoutSecs(ForwarderConstants.PHONE_TIMEOUT_S);

        channelSettingsBuilder.setName(mChannelName);
        channelSettingsBuilder.setPsk(ByteString.copyFrom(mChannelPsk));
        channelSettingsBuilder.setModemConfig(modemConfig);

        radioConfigBuilder.setPreferences(userPreferencesBuilder);
        radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

        radioConfig = radioConfigBuilder.build();

        try {
            mMeshService.setRadioConfig(radioConfig.toByteArray());
        } catch (RemoteException e) {
            mLogger.e(TAG, "writeRadioConfig() - exception while writing config");
            e.printStackTrace();
        }
    }

    private boolean compareByteArrays(byte[] lhs, byte[] rhs) {
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
