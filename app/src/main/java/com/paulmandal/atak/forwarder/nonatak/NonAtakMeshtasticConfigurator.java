package com.paulmandal.atak.forwarder.nonatak;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;

public class NonAtakMeshtasticConfigurator {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + NonAtakMeshtasticConfigurator.class.getSimpleName();

    private static final int POSITION_BROADCAST_INTERVAL_S = Config.POSITION_BROADCAST_INTERVAL_S;
    private static final int LCD_SCREEN_ON_S = Config.LCD_SCREEN_ON_S;

    private final Activity mActivity;

    private final String mCommDeviceAddress;
    private final String mTargetDeviceAddress;
    private final String mDeviceCallsign;
    private final String mChannelName;
    private final byte[] mPsk;
    private final MeshProtos.ChannelSettings.ModemConfig mModemConfig;
    private final int mTeamIndex;
    private final int mRoleIndex;
    private final int mPliIntervalS;

    private IntentFilter mIntentFilter;

    private IMeshService mMeshService;
    private ServiceConnection mServiceConnection;
    private Intent mServiceIntent;

    private boolean mBound;

    public NonAtakMeshtasticConfigurator(Activity activity,
                                         String commDeviceAddress,
                                         String targetDeviceAddress,
                                         String deviceCallsign,
                                         String channelName,
                                         byte[] psk,
                                         MeshProtos.ChannelSettings.ModemConfig modemConfig,
                                         int teamIndex,
                                         int roleIndex,
                                         int pliIntervalS) {
        mActivity = activity;
        mCommDeviceAddress = commDeviceAddress;
        mTargetDeviceAddress = targetDeviceAddress;
        mDeviceCallsign = deviceCallsign;
        mChannelName = channelName;
        mPsk = psk;
        mModemConfig = modemConfig;
        mTeamIndex = teamIndex;
        mRoleIndex = roleIndex;
        mPliIntervalS = pliIntervalS;
    }

    public void writeToDevice() {
        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mBound = true;

                onConnected();
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;

                mBound = false;
            }
        };

        mServiceIntent = new Intent();
        mServiceIntent.setClassName("com.geeksville.mesh", "com.geeksville.mesh.service.MeshService");

        mActivity.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
//
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_MESH_CONNECTED);
//        filter.addAction(ACTION_NODE_CHANGE);
//        filter.addAction(ACTION_RECEIVED_DATA);
//        filter.addAction(ACTION_MESSAGE_STATUS);
//
//        mIntentFilter = filter;
//
//        mActivity.registerReceiver(mBroadcastReceiver, filter);
    }

    private void onConnected() {
        try {
            // TODO: confirmation this worked?
//            setDeviceAddress(mTargetDeviceAddress);

            Thread.sleep(2000); // TODO: better way to do this

            mMeshService.setOwner(null, mDeviceCallsign, String.format("%d%d", mTeamIndex, mRoleIndex));

            // TODO: move this to a util?
            byte[] radioConfigBytes = mMeshService.getRadioConfig();

            if (radioConfigBytes == null) {
                // TODO: retry?
                Log.e(TAG, "radioConfigBytes was null");
                return;
            }

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
            MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

            userPreferencesBuilder.setPositionBroadcastSecs(POSITION_BROADCAST_INTERVAL_S);
            userPreferencesBuilder.setScreenOnSecs(LCD_SCREEN_ON_S);

            channelSettingsBuilder.setName(mChannelName);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(mPsk));
            channelSettingsBuilder.setModemConfig(mModemConfig);

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            mMeshService.setRadioConfig(radioConfig.toByteArray());

            Thread.sleep(2000); // TODO: better way to do this

//            setDeviceAddress(mCommDeviceAddress); // TODO: verify this changed back
            Log.e(TAG, "Done writing to device: " + mTargetDeviceAddress);
        } catch (RemoteException | InterruptedException | InvalidProtocolBufferException e) {
            e.printStackTrace();
            Log.e(TAG, "RemoteException writing to non-ATAK device: " + e.getMessage());
        }
    }

    private void setDeviceAddress(String deviceAddress) throws RemoteException {
        mMeshService.setDeviceAddress(String.format("x%s", deviceAddress));
    }
}
