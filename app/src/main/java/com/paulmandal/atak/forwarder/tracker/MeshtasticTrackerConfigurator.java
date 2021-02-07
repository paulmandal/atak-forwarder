package com.paulmandal.atak.forwarder.tracker;

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
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.helpers.HashHelper;

public class MeshtasticTrackerConfigurator {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MeshtasticTrackerConfigurator.class.getSimpleName();

    public interface Listener {
        void onDoneWritingToDevice();
    }

    private static final int DEVICE_CONNECTION_TIMEOUT = Config.DEVICE_CONNECTION_TIMEOUT;
    private static final int RADIO_CONFIG_MISSING_RETRY_TIME_MS = Config.RADIO_CONFIG_MISSING_RETRY_TIME_MS;

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

    private final Context mAtakContext;
    private final Handler mUiThreadHandler;

    private final MeshtasticDevice mCommDevice;
    private final MeshtasticCommHardware mMeshtasticCommHardware;
    private final MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private final MeshtasticDevice mTargetDevice;
    private final String mDeviceCallsign;
    private final String mChannelName;
    private final byte[] mPsk;
    private final MeshProtos.ChannelSettings.ModemConfig mModemConfig;
    private final int mTeamIndex;
    private final int mRoleIndex;
    private final int mPliIntervalS;
    private final int mScreenShutoffDelayS;

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
        mListener.onDoneWritingToDevice();
    };

    public MeshtasticTrackerConfigurator(Context atakContext,
                                         Handler uiThreadHandler,
                                         MeshtasticCommHardware meshtasticCommHardware,
                                         MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                         MeshtasticDevice commDevice,
                                         MeshtasticDevice targetDevice,
                                         String deviceCallsign,
                                         String channelName,
                                         byte[] psk,
                                         MeshProtos.ChannelSettings.ModemConfig modemConfig,
                                         int teamIndex,
                                         int roleIndex,
                                         int pliIntervalS,
                                         int screenShutoffDelayS,
                                         Listener listener) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mCommDevice = commDevice;
        mMeshtasticCommHardware = meshtasticCommHardware;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mTargetDevice = targetDevice;
        mDeviceCallsign = deviceCallsign;
        mChannelName = channelName;
        mPsk = psk;
        mModemConfig = modemConfig;
        mTeamIndex = teamIndex;
        mRoleIndex = roleIndex;
        mPliIntervalS = pliIntervalS;
        mScreenShutoffDelayS = screenShutoffDelayS;
        mListener = listener;
    }

    public void writeToDevice() {
        mMeshtasticCommHardware.suspendResume(true);

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

        mAtakContext.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);

        mIntentFilter = filter;

        mAtakContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public void cancel() {
        mUiThreadHandler.removeCallbacks(mTimeoutRunnable);
        unbind();
    }

    private void onConnected() {
        try {
            Log.d(TAG, "Setting service to use non-ATAK device address: " + mTargetDevice);
            mUiThreadHandler.postDelayed(mTimeoutRunnable, DEVICE_CONNECTION_TIMEOUT);
            mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, mTargetDevice);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e(TAG, "RemoteException writing to non-ATAK device: " + e.getMessage());
        }
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
                Log.e(TAG, "radioConfigBytes was null, retrying in: " + RADIO_CONFIG_MISSING_RETRY_TIME_MS + "ms");
                mUiThreadHandler.postDelayed(() -> maybeWriteToDevice(), RADIO_CONFIG_MISSING_RETRY_TIME_MS);
                return;
            }

            MeshProtos.RadioConfig radioConfig = MeshProtos.RadioConfig.parseFrom(radioConfigBytes);
            MeshProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();
            MeshProtos.ChannelSettings channelSettings = radioConfig.getChannelSettings();

            MeshProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            MeshProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();
            MeshProtos.ChannelSettings.Builder channelSettingsBuilder = channelSettings.toBuilder();

            userPreferencesBuilder.setPositionBroadcastSecs(mPliIntervalS);
            userPreferencesBuilder.setScreenOnSecs(mScreenShutoffDelayS);

            channelSettingsBuilder.setName(mChannelName);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(mPsk));
            channelSettingsBuilder.setModemConfig(mModemConfig);

            Log.d(TAG, "Setting non-ATAK device channel: " + mChannelName + " / " + new HashHelper().hashFromBytes(mPsk) + " / " + mModemConfig.getNumber());

            radioConfigBuilder.setPreferences(userPreferencesBuilder);
            radioConfigBuilder.setChannelSettings(channelSettingsBuilder);

            radioConfig = radioConfigBuilder.build();

            mMeshService.setRadioConfig(radioConfig.toByteArray());

            if (TrackerCotGenerator.ROLES.length > 9) {
                throw new RuntimeException("NonAtakStationCotGenerator.ROLES.length > 9, but our shortName format depends on it only ever being 1 digit long");
            }

            Log.d(TAG, "Setting non-ATAK device owner: " + mDeviceCallsign + ", " + String.format("%d%d", mRoleIndex, mTeamIndex));
            mMeshService.setOwner(null, mDeviceCallsign, String.format("%d%d", mRoleIndex, mTeamIndex));

            Log.d(TAG, "Non-ATAK device NodeInfo: " + mMeshService.getMyNodeInfo());

            mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, mCommDevice);
            mWroteToDevice = true;
        } catch (RemoteException | InvalidProtocolBufferException e) {
            Log.e(TAG, "RemoteException writing to non-ATAK device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void maybeDoneWriting() {
        if (!mWroteToDevice) {
            return;
        }

        Log.d(TAG, "Got reconnect after switching back to comm device, finishing write process.");

        unbind();
        mMeshtasticCommHardware.suspendResume(false);
        mListener.onDoneWritingToDevice();
    }

    private void unbind() {
        mAtakContext.unregisterReceiver(mBroadcastReceiver);
        mAtakContext.unbindService(mServiceConnection);
    }
}
