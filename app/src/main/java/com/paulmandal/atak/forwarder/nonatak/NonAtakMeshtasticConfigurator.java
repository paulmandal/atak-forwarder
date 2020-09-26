package com.paulmandal.atak.forwarder.nonatak;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

public class NonAtakMeshtasticConfigurator {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + NonAtakMeshtasticConfigurator.class.getSimpleName();

    public interface Listener {
        void onDoneWritingToDevice();
    }

    public static final int WAIT_TIME_AFTER_WRITING_NON_ATAK_DEVICE = Config.WAIT_TIME_AFTER_WRITING_NON_ATAK_DEVICE;
    public static final int DEVICE_CONNECTION_TIMEOUT = Config.DEVICE_CONNECTION_TIMEOUT;

    private static final int POSITION_BROADCAST_INTERVAL_S = Config.POSITION_BROADCAST_INTERVAL_S;
    private static final int LCD_SCREEN_ON_S = Config.LCD_SCREEN_ON_S;

    /**
     * Intents the Meshtastic service can send
     */
    private static final String ACTION_MESH_CONNECTED = "com.geeksville.mesh.MESH_CONNECTED";

    /**
     * Extra data fields from the Meshtastic service
     */
    // a bool true means now connected, false means not
    private static final String EXTRA_CONNECTED = "com.geeksville.mesh.Connected";

    private static final String STATE_CONNECTED = "CONNECTED";

    private final Activity mActivity;
    private final Handler mUiThreadHandler;

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

    private Listener mListener;

    private boolean mBound;

    private boolean mStartedWritingToDevice = false;
    private boolean mWroteToDevice = false;

    private Runnable mTimeoutRunnable = () -> {
        Log.e(TAG, "Timed out writing to non-ATAK device!");
        cancel();
    };

    private Runnable mPostWriteDelayRunnable;

    public NonAtakMeshtasticConfigurator(Activity activity,
                                         Handler uiThreadHandler,
                                         String commDeviceAddress,
                                         String targetDeviceAddress,
                                         String deviceCallsign,
                                         String channelName,
                                         byte[] psk,
                                         MeshProtos.ChannelSettings.ModemConfig modemConfig,
                                         int teamIndex,
                                         int roleIndex,
                                         int pliIntervalS,
                                         Listener listener) {
        mActivity = activity;
        mUiThreadHandler = uiThreadHandler;
        mCommDeviceAddress = commDeviceAddress;
        mTargetDeviceAddress = targetDeviceAddress;
        mDeviceCallsign = deviceCallsign;
        mChannelName = channelName;
        mPsk = psk;
        mModemConfig = modemConfig;
        mTeamIndex = teamIndex;
        mRoleIndex = roleIndex;
        mPliIntervalS = pliIntervalS;
        mListener = listener;
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);

        mIntentFilter = filter;

        mActivity.registerReceiver(mBroadcastReceiver, filter);
    }

    public void cancel() {
        mUiThreadHandler.removeCallbacks(mTimeoutRunnable);
        mUiThreadHandler.removeCallbacks(mPostWriteDelayRunnable);
        unbind();
    }

    private void onConnected() {
        try {
            Log.e(TAG, "Setting non-ATAK address: " + mTargetDeviceAddress);
            mUiThreadHandler.postDelayed(mTimeoutRunnable, DEVICE_CONNECTION_TIMEOUT);
            setDeviceAddress(mTargetDeviceAddress);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e(TAG, "RemoteException writing to non-ATAK device: " + e.getMessage());
        }
    }

    private void setDeviceAddress(String deviceAddress) throws RemoteException {
        mMeshService.setDeviceAddress(String.format("x%s", deviceAddress));
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);

            if (action == null) {
                Log.e(TAG, "onReceive, action was null");
                return;
            }

            switch (action) {
                case ACTION_MESH_CONNECTED:
                    String extraConnected = intent.getStringExtra(EXTRA_CONNECTED);
                    boolean connected = extraConnected.equals(STATE_CONNECTED);

                    if (connected) {
                        mUiThreadHandler.removeCallbacks(mTimeoutRunnable);
                        maybeWriteToDevice();
                        maybeDoneWriting();
                    }
                    // TODO: retry if disconnected? or start a timer and retry if connected doens't happen before delay?
                    Log.d(TAG, "ACTION_MESH_CONNECTED: " + connected + ", extra: " + extraConnected);
                    break;
                default:
                    Log.e(TAG, "Do not know how to handle intent action: " + intent.getAction());
                    break;
            }
        }
    };

    private void maybeWriteToDevice() {
        if (mWroteToDevice || mStartedWritingToDevice) {
            return;
        }

        mStartedWritingToDevice = true;

        try {
            // TODO: move this to a util?
            byte[] radioConfigBytes = mMeshService.getRadioConfig();

            if (radioConfigBytes == null) {
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

            Log.e(TAG, "setting channel: " + mChannelName + " / " + new HashHelper().hashFromBytes(mPsk) + " / " + mModemConfig.getNumber());

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            mMeshService.setRadioConfig(radioConfig.toByteArray());

            Log.e(TAG, "Node Info: " + mMeshService.getMyNodeInfo());
            Log.e(TAG, "setting owner: " + mDeviceCallsign + ", " + String.format("%d%d", mTeamIndex, mRoleIndex));
            mMeshService.setOwner(null, mDeviceCallsign, String.format("%d%d", mTeamIndex, mRoleIndex));

            Log.e(TAG, "post-set node Info: " + mMeshService.getMyNodeInfo());

            mPostWriteDelayRunnable = () -> {
                try {
                    Log.e(TAG, "setting address back to: " + mCommDeviceAddress);
                    setDeviceAddress(mCommDeviceAddress); // TODO: verify this changed back
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mWroteToDevice = true;
                Log.e(TAG, "Done writing to device: " + mTargetDeviceAddress);

            };
            mUiThreadHandler.postDelayed(mPostWriteDelayRunnable, WAIT_TIME_AFTER_WRITING_NON_ATAK_DEVICE);
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "RemoteException writing to non-ATAK device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void maybeDoneWriting() {
        if (!mWroteToDevice) {
            return;
        }

        Log.e(TAG, "got reconnect after writing");

        unbind();
        mListener.onDoneWritingToDevice();
    }

    private void unbind() {
        mActivity.unregisterReceiver(mBroadcastReceiver);
        mActivity.unbindService(mServiceConnection);
    }
}
