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
import android.util.Log;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.IMeshService;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.Constants;
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

    private static final String TAG = Constants.DEBUG_TAG_PREFIX + MeshServiceController.class.getSimpleName();

    private final Context mAtakContext;
    private final Handler mUiThreadHandler;
    private final SharedPreferences mSharedPreferences;
    private final Logger mLogger;

    private IMeshService mMeshService;
    private ServiceConnection mServiceConnection;

    private final IntentFilter mIntentFilter;
    private final Intent mServiceIntent;

    private MeshtasticDevice mMeshDevice;
    private ConnectionState mConnectionState;
    private boolean mConnectedToService;

    private final Set<ConnectionStateListener> mConnectionStateListeners = new CopyOnWriteArraySet<>();
    private String mMeshId;

    public MeshServiceController(List<Destroyable> destroyables,
                                 SharedPreferences sharedPreferences,
                                 Context atakContext,
                                 Handler uiThreadHandler,
                                 MeshSuspendController meshSuspendController,
                                 Logger logger) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mLogger = logger;
        mSharedPreferences = sharedPreferences;

        destroyables.add(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        meshSuspendController.addSuspendListener(this);

        // Initialize comm device state
        onSharedPreferenceChanged(sharedPreferences, PreferencesKeys.KEY_SET_COMM_DEVICE);

        // Set up service connection
        mServiceIntent = new Intent();
        mServiceIntent.setClassName(MeshServiceConstants.PACKAGE_NAME, MeshServiceConstants.CLASS_NAME);

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                logger.v(TAG, "Service connected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mConnectedToService = true;
                mConnectionState = ConnectionState.DEVICE_DISCONNECTED;

                updateConnectionState();
            }

            public void onServiceDisconnected(ComponentName className) {
                logger.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;
                mConnectedToService = false;
                mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;

                notifyConnectionStateListeners();
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
    public String getMeshId() {
        return mMeshId;
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
            Log.e(TAG, "onReceive with null action");
            return;
        }

        if (action.equals(MeshServiceConstants.ACTION_MESH_CONNECTED)) {
            String extraConnected = intent.getStringExtra(MeshServiceConstants.EXTRA_CONNECTED);
            boolean connected = extraConnected.equals(MeshServiceConstants.STATE_CONNECTED);
            Log.d(TAG, "  Mesh connected: " + connected);
            updateConnectionState();
        }
    }

    @Override
    public void onSuspendedChanged(boolean suspended) {
        if (suspended) {
            unregisterReceiver();
        } else {
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

    private void updateConnectionState() {
        ConnectionState connectionState;
        if (mMeshDevice == null) {
            connectionState = ConnectionState.NO_DEVICE_CONFIGURED;
        } else {
            boolean connected = false;
            try {
                connected = mMeshService.connectionState().equals(MeshServiceConstants.STATE_CONNECTED);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            connectionState = connected ? ConnectionState.DEVICE_CONNECTED : ConnectionState.DEVICE_DISCONNECTED;
        }

        if (mConnectionState != connectionState) {
            mConnectionState = connectionState;
            notifyConnectionStateListeners();
        }
    }

    private void notifyConnectionStateListeners() {
        for (ConnectionStateListener connectionStateListener : mConnectionStateListeners) {
            mUiThreadHandler.post(() -> connectionStateListener.onConnectionStateChanged(mConnectionState));
        }
    }


    private void registerReceiver() {
        mAtakContext.registerReceiver(this, mIntentFilter);
    }

    private void unregisterReceiver() {
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
            Gson gson = new Gson();
            mMeshDevice = gson.fromJson(sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE), MeshtasticDevice.class);
            updateConnectionState();
        }
    }
}
