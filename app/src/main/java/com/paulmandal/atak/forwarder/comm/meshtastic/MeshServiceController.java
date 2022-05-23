package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.IMeshService;
import com.geeksville.mesh.RadioConfigProtos;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.SuspendListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshServiceController extends BroadcastReceiver implements Destroyable, SuspendListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public interface ConnectionStateListener {
        void onConnectionStateChanged(ConnectionState connectionState);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshServiceController.class.getSimpleName();

    private final Context mAtakContext;
    private final Handler mUiThreadHandler;
    private final SharedPreferences mSharedPreferences;
    private final Gson mGson;
    private final Logger mLogger;

    private IMeshService mMeshService;
    private final ServiceConnection mServiceConnection;

    private final IntentFilter mIntentFilter;
    private final Intent mServiceIntent;

    private MeshtasticDevice mMeshDevice;
    private RadioConfigProtos.RegionCode mRegionCode = RadioConfigProtos.RegionCode.Unset;
    private ConnectionState mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;
    private boolean mConnectedToService;
    private boolean mReceiverRegistered;

    private final Set<ConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArraySet<>();

    public MeshServiceController(List<Destroyable> destroyables,
                                 SharedPreferences sharedPreferences,
                                 Context atakContext,
                                 Handler uiThreadHandler,
                                 MeshSuspendController meshSuspendController,
                                 Gson gson,
                                 Logger logger) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mGson = gson;
        mLogger = logger;
        mSharedPreferences = sharedPreferences;

        destroyables.add(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        meshSuspendController.addSuspendListener(this);

        // Initialize comm device state
        onSharedPreferenceChanged(sharedPreferences, PreferencesKeys.KEY_SET_COMM_DEVICE);
        onSharedPreferenceChanged(sharedPreferences, PreferencesKeys.KEY_REGION);

        // Set up service connection
        mServiceIntent = new Intent();
        mServiceIntent.setClassName(MeshServiceConstants.PACKAGE_NAME, MeshServiceConstants.CLASS_NAME);

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                logger.v(TAG, "Service connected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mConnectedToService = true;

                if (mMeshDevice == null || mRegionCode == null || mRegionCode == RadioConfigProtos.RegionCode.Unset) {
                    updateConnectionState(ConnectionState.NO_DEVICE_CONFIGURED);
                } else {
                    updateConnectionState(ConnectionState.DEVICE_DISCONNECTED);
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                logger.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;
                mConnectedToService = false;
                updateConnectionState(ConnectionState.NO_SERVICE_CONNECTION);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(MeshServiceConstants.ACTION_MESH_CONNECTED);
        mIntentFilter = filter;

        bindToService();
        registerReceiver();
    }

    public IMeshService getMeshService() {
        return mMeshService;
    }
    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    public void addConnectionStateListener(ConnectionStateListener listener) {
        mConnectionStateListeners.add(listener);
    }

    public void removeConnectionStateListener(ConnectionStateListener listener) {
        mConnectionStateListeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            mLogger.e(TAG, "onReceive with null action");
            return;
        }

        if (action.equals(MeshServiceConstants.ACTION_MESH_CONNECTED)) {
            String extraConnected = intent.getStringExtra(MeshServiceConstants.EXTRA_CONNECTED);
            boolean connected = extraConnected.equals(MeshServiceConstants.STATE_CONNECTED);
            mLogger.d(TAG, "Mesh connected: " + connected);
            updateConnectionState(connected ? ConnectionState.DEVICE_CONNECTED : ConnectionState.DEVICE_DISCONNECTED);
        }
    }

    @Override
    public void onSuspendedChanged(boolean suspended) {
        if (suspended && mReceiverRegistered) {
            unregisterReceiver();
        } else if (!suspended && !mReceiverRegistered) {
            registerReceiver();
        }
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mLogger.v(TAG, "onDestroy()");
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver();
        unbindAndStopService();
    }

    private void unbindAndStopService() {
        if (!mConnectedToService) {
            return;
        }

        unBindService();
        stopService();
    }

    private void updateConnectionState(ConnectionState connectionState) {
        mLogger.d(TAG, "Updating connection state to: " + connectionState);
        if (mConnectionState != connectionState) {
            mConnectionState = connectionState;
            notifyConnectionStateListeners();
        }
    }

    private void notifyConnectionStateListeners() {
        for (ConnectionStateListener connectionStateListener : mConnectionStateListeners) {
            mLogger.v(TAG, "Notifying connection state listener: " + connectionStateListener.hashCode() + ", state: " + mConnectionState);
            mUiThreadHandler.post(() -> connectionStateListener.onConnectionStateChanged(mConnectionState));
        }
    }


    private void registerReceiver() {
        mReceiverRegistered = true;
        mAtakContext.registerReceiver(this, mIntentFilter);
    }

    private void unregisterReceiver() {
        mReceiverRegistered = false;
        mAtakContext.unregisterReceiver(this);
    }

    private void bindToService() {
        mAtakContext.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        mAtakContext.unbindService(mServiceConnection);
    }

    private void stopService() {
        mAtakContext.stopService(mServiceIntent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PreferencesKeys.KEY_SET_COMM_DEVICE)) {
            mMeshDevice = mGson.fromJson(sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE), MeshtasticDevice.class);
        } else if (key.equals(PreferencesKeys.KEY_REGION)) {
            mRegionCode = RadioConfigProtos.RegionCode.forNumber(Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_REGION, PreferencesDefaults.DEFAULT_REGION)));
        }
    }
}
