package com.paulmandal.atak.forwarder.tracker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.geeksville.mesh.AppOnlyProtos;
import com.geeksville.mesh.ChannelProtos;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.RadioConfigProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshSuspendController;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;

public class MeshtasticTrackerConfigurator {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshtasticTrackerConfigurator.class.getSimpleName();

    public interface Listener {
        void onDoneWritingToDevice();
    }

    private static final int DEVICE_CONNECTION_TIMEOUT = ForwarderConstants.DEVICE_CONNECTION_TIMEOUT;
    private static final int RADIO_CONFIG_MISSING_RETRY_TIME_MS = ForwarderConstants.RADIO_CONFIG_MISSING_RETRY_TIME_MS;
    private static final int DELAY_BEFORE_RESTARTING_MESH_SENDER_AFTER_TRACKER_WRITE = ForwarderConstants.DELAY_BEFORE_RESTARTING_MESH_SENDER_AFTER_TRACKER_WRITE;

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
    private final MeshSuspendController mMeshSuspendController;

    private final MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private final MeshtasticDevice mTargetDevice;
    private final String mDeviceCallsign;
    private final RadioConfigProtos.RegionCode mRegionCode;
    private final String mChannelName;
    private final byte[] mPsk;
    private final ChannelProtos.ChannelSettings.ModemConfig mModemConfig;
    private final int mTeamIndex;
    private final int mRoleIndex;
    private final int mPliIntervalS;
    private final int mScreenShutoffDelayS;
    private final boolean mIsAlwaysPoweredOn;
    private final RadioConfigProtos.Role mDeviceRole;

    private final Listener mListener;
    private final Logger mLogger;
    
    private IMeshService mMeshService;
    private ServiceConnection mServiceConnection;

    private boolean mBound;

    private boolean mStartedWritingToDevice = false;
    private boolean mWroteToDevice = false;

    private final Runnable mTimeoutRunnable;

    public MeshtasticTrackerConfigurator(Context atakContext,
                                         Handler uiThreadHandler,
                                         MeshSuspendController meshSuspendController,
                                         MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                         MeshtasticDevice commDevice,
                                         MeshtasticDevice targetDevice,
                                         String deviceCallsign,
                                         RadioConfigProtos.RegionCode regionCode,
                                         String channelName,
                                         byte[] psk,
                                         ChannelProtos.ChannelSettings.ModemConfig modemConfig,
                                         int teamIndex,
                                         int roleIndex,
                                         int pliIntervalS,
                                         int screenShutoffDelayS,
                                         boolean isAlwaysPoweredOn,
                                         RadioConfigProtos.Role deviceRole,
                                         Listener listener,
                                         Logger logger) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mCommDevice = commDevice;
        mMeshSuspendController = meshSuspendController;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mTargetDevice = targetDevice;
        mDeviceCallsign = deviceCallsign;
        mRegionCode = regionCode;
        mChannelName = channelName;
        mPsk = psk;
        mModemConfig = modemConfig;
        mTeamIndex = teamIndex;
        mRoleIndex = roleIndex;
        mPliIntervalS = pliIntervalS;
        mScreenShutoffDelayS = screenShutoffDelayS;
        mIsAlwaysPoweredOn = isAlwaysPoweredOn;
        mDeviceRole = deviceRole;
        mListener = listener;
        mLogger = logger;

        mTimeoutRunnable = () -> {
            mLogger.e(TAG, "Timed out writing to Tracker device!");
            cancel();
            mMeshSuspendController.setSuspended(false);
            mListener.onDoneWritingToDevice();
        };
    }

    public void writeToDevice() {
        mMeshSuspendController.setSuspended(true);

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mLogger.d(TAG, "onServiceConnected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mBound = true;

                onConnected();
            }

            public void onServiceDisconnected(ComponentName className) {
                mLogger.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;

                mBound = false;
            }
        };

        Intent serviceIntent = new Intent();
        serviceIntent.setClassName("com.geeksville.mesh", "com.geeksville.mesh.service.MeshService");

        mAtakContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MESH_CONNECTED);

        mAtakContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public void cancel() {
        mUiThreadHandler.removeCallbacks(mTimeoutRunnable);
        unbind();
    }

    private void onConnected() {
        try {
            mLogger.d(TAG, "Setting service to use Tracker device address: " + mTargetDevice);
            mUiThreadHandler.postDelayed(mTimeoutRunnable, DEVICE_CONNECTION_TIMEOUT);
            mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, mTargetDevice);
        } catch (RemoteException e) {
            e.printStackTrace();
            mLogger.e(TAG, "RemoteException writing to Tracker device: " + e.getMessage());
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mLogger.d(TAG, "onReceive: " + action);

            if (action == null) {
                mLogger.e(TAG, "onReceive, action was null");
                return;
            }

            if (action.equals(ACTION_MESH_CONNECTED)) {
                String extraConnected = intent.getStringExtra(EXTRA_CONNECTED);
                boolean connected = extraConnected.equals(STATE_CONNECTED);

                if (connected) {
                    mUiThreadHandler.removeCallbacks(mTimeoutRunnable);
                    maybeWriteToDevice();
                    maybeDoneWriting();
                }
                mLogger.d(TAG, "ACTION_MESH_CONNECTED: " + connected + ", extra: " + extraConnected);
            } else {
                mLogger.e(TAG, "Do not know how to handle intent action: " + intent.getAction());
            }
        }
    };

    @SuppressLint("DefaultLocale")
    private void maybeWriteToDevice() {
        if (mWroteToDevice || mStartedWritingToDevice) {
            return;
        }

        mStartedWritingToDevice = true;

        try {
            byte[] radioConfigBytes = mMeshService.getRadioConfig();

            if (radioConfigBytes == null) {
                mLogger.e(TAG, "radioConfigBytes was null, retrying in: " + RADIO_CONFIG_MISSING_RETRY_TIME_MS + "ms");
                mUiThreadHandler.postDelayed(this::maybeWriteToDevice, RADIO_CONFIG_MISSING_RETRY_TIME_MS);
                return;
            }

            byte[] channelSetBytes = null;
            try {
                channelSetBytes = mMeshService.getChannels();
            } catch (RemoteException e) {
                mLogger.e(TAG, "  channelSetBytes - RemoteException!");
                e.printStackTrace();
            }

            if (channelSetBytes == null) {
                mLogger.e(TAG, "  channelSetBytes was null, retrying in: " + RADIO_CONFIG_MISSING_RETRY_TIME_MS + "ms");
                mUiThreadHandler.postDelayed(this::maybeWriteToDevice, RADIO_CONFIG_MISSING_RETRY_TIME_MS);
                return;
            }

            RadioConfigProtos.RadioConfig radioConfig = RadioConfigProtos.RadioConfig.parseFrom(radioConfigBytes);
            RadioConfigProtos.RadioConfig.UserPreferences userPreferences = radioConfig.getPreferences();

            RadioConfigProtos.RadioConfig.Builder radioConfigBuilder = radioConfig.toBuilder();
            RadioConfigProtos.RadioConfig.UserPreferences.Builder userPreferencesBuilder = userPreferences.toBuilder();

            userPreferencesBuilder.setLocationShareDisabled(false);
            userPreferencesBuilder.setGpsDisabled(false);
            userPreferencesBuilder.setPositionBroadcastSecs(mPliIntervalS);
            userPreferencesBuilder.setGpsUpdateInterval(mPliIntervalS);
            userPreferencesBuilder.setScreenOnSecs(mScreenShutoffDelayS);
            userPreferencesBuilder.setIsAlwaysPowered(mIsAlwaysPoweredOn);
            userPreferencesBuilder.setRegion(mRegionCode);
            userPreferencesBuilder.setRole(mDeviceRole);

            mLogger.d(TAG, "Setting Tracker device region: " + mRegionCode + ", deviceRole: " + mDeviceRole);

            radioConfigBuilder.setPreferences(userPreferencesBuilder.build());
            radioConfig = radioConfigBuilder.build();

            mMeshService.setRadioConfig(radioConfig.toByteArray());

            AppOnlyProtos.ChannelSet channelSet = AppOnlyProtos.ChannelSet.parseFrom(channelSetBytes);
            AppOnlyProtos.ChannelSet.Builder channelSetBuilder = channelSet.toBuilder();

            ChannelProtos.ChannelSettings.Builder channelSettingsBuilder = ChannelProtos.ChannelSettings.newBuilder();
            channelSettingsBuilder.setName(mChannelName);
            channelSettingsBuilder.setPsk(ByteString.copyFrom(mPsk));
            channelSettingsBuilder.setModemConfig(mModemConfig);

            for (int i = 0 ; i < channelSet.getSettingsCount() ; i++) {
                ChannelProtos.ChannelSettings channelSetting = channelSet.getSettings(i);
                mLogger.d(TAG, "  deleting channel from Tracker: " + channelSetting.getName());
            }

            mLogger.d(TAG, "Setting Tracker device channel: " + mChannelName + " / " + new HashHelper().hashFromBytes(mPsk) + " / " + mModemConfig.getNumber());
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

            channelSetBuilder.addSettings(0, channelSettingsBuilder.build());
            channelSet = channelSetBuilder.build();

            mMeshService.setChannels(channelSet.toByteArray());

            if (TrackerCotGenerator.ROLES.length > 9) {
                throw new RuntimeException("TrackerCotGenerator.ROLES.length > 9, but our shortName format depends on it only ever being 1 digit long");
            }

            mLogger.d(TAG, "Setting Tracker device owner: " + mDeviceCallsign + ", " + String.format("%d%d", mRoleIndex, mTeamIndex));
            mMeshService.setOwner(null, mDeviceCallsign, String.format("%d%d", mRoleIndex, mTeamIndex));

            mLogger.d(TAG, "Tracker device NodeInfo: " + mMeshService.getMyNodeInfo());

            mUiThreadHandler.postDelayed(() -> {
                try {
                    mLogger.d(TAG, "Setting device address back to Comm Device");
                    mMeshtasticDeviceSwitcher.setDeviceAddress(mMeshService, mCommDevice);
                    mWroteToDevice = true;
                } catch (RemoteException e) {
                    mLogger.e(TAG, "RemoteException writing to Tracker device: " + e.getMessage());
                    e.printStackTrace();
                }
            }, DELAY_BEFORE_RESTARTING_MESH_SENDER_AFTER_TRACKER_WRITE);
        } catch (RemoteException | InvalidProtocolBufferException e) {
            mLogger.e(TAG, "RemoteException writing to Tracker device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void maybeDoneWriting() {
        if (!mWroteToDevice) {
            return;
        }

        mLogger.d(TAG, "Got reconnect after switching back to comm device, finishing write process.");

        unbind();
        mMeshSuspendController.setSuspended(false);
        mListener.onDoneWritingToDevice();
    }

    private void unbind() {
        mAtakContext.unregisterReceiver(mBroadcastReceiver);
        mAtakContext.unbindService(mServiceConnection);
    }
}
