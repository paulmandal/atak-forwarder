package com.paulmandal.atak.forwarder.comm.commhardware.meshtastic;

import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.UserInfo;

public class MeshtasticDeviceConfigurer {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticDeviceConfigurer.class.getSimpleName();

    private static final int POSITION_BROADCAST_INTERVAL_S = Config.POSITION_BROADCAST_INTERVAL_S;
    private static final int LCD_SCREEN_ON_S = Config.LCD_SCREEN_ON_S;
    private static final int WAIT_BLUETOOTH_S = Config.WAIT_BLUETOOTH_S;
    private static final int PHONE_TIMEOUT_S = Config.PHONE_TIMEOUT_S;

    private final UserInfo mSelfInfo;

    public MeshtasticDeviceConfigurer(UserInfo selfInfo) {
        mSelfInfo = selfInfo;
    }

    public boolean configureDevice(IMeshService meshService) {
        try {
            String meshId = meshService.getMyId();
            String shortMeshId = meshId.replaceAll("!", "").substring(meshId.length() - 5);
            meshService.setOwner(null, String.format("%s-MX-%s", mSelfInfo.callsign, shortMeshId), mSelfInfo.callsign.substring(0, 1));

            // Set up radio / channel settings
            byte[] radioConfigBytes = meshService.getRadioConfig();

            if (radioConfigBytes == null) {
                Log.e(TAG, "radioConfigBytes was null");
                return false;
            }

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
            MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

            // Begin Updates

            userPreferencesBuilder.setPositionBroadcastSecs(POSITION_BROADCAST_INTERVAL_S);
            userPreferencesBuilder.setScreenOnSecs(LCD_SCREEN_ON_S);
            userPreferencesBuilder.setWaitBluetoothSecs(WAIT_BLUETOOTH_S);
            userPreferencesBuilder.setPhoneTimeoutSecs(PHONE_TIMEOUT_S);

            // End Updates

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            meshService.setRadioConfig(radioConfig.toByteArray());

            return true;
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "Exception in setupRadio(): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
