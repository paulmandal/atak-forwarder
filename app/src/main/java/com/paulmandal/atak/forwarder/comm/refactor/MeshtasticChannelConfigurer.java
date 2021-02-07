package com.paulmandal.atak.forwarder.comm.refactor;

import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.UserTracker;

public class MeshtasticChannelConfigurer {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticChannelConfigurer.class.getSimpleName();

    private final UserTracker mUserTracker;

    public MeshtasticChannelConfigurer(UserTracker userTracker) {
        mUserTracker = userTracker;
    }

    public void updateChannelSettings(IMeshService meshService, String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        try {
            byte[] radioConfigBytes = meshService.getRadioConfig();

            if (radioConfigBytes == null) {
                Log.e(TAG, "radioConfigBytes was null");
                return;
            }

            mUserTracker.clearData();

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
            MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

            channelSettingsBuilder.setName(channelName);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(psk));
            channelSettingsBuilder.setModemConfig(modemConfig);

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            meshService.setRadioConfig(radioConfig.toByteArray());
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "Exception in updateChannelSettings(): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
