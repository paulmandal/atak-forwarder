package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class DeviceConnectionHandler extends BroadcastReceiver implements MeshServiceController.Listener, Destroyable {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + DeviceConnectionHandler.class.getSimpleName();

    public enum DeviceConnectionState {
        DISCONNECTED,
        CONNECTED
    }

    public interface Listener {
        void onDeviceConnectionStateChanged(DeviceConnectionState deviceConnectionState);
    }

    private final Context mAtakContext;
    private final MeshServiceController mMeshServiceController;
    private final Logger mLogger;
    private final IntentFilter mIntentFilter;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    private boolean mReceiverRegistered;

    public DeviceConnectionHandler(Context atakContext,
                                   List<Destroyable> destroyables,
                                   MeshServiceController meshServiceController,
                                   Logger logger) {
        mAtakContext = atakContext;
        mMeshServiceController = meshServiceController;
        mLogger = logger;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MeshServiceConstants.ACTION_MESH_CONNECTED);
        mIntentFilter = intentFilter;

        destroyables.add(this);
        meshServiceController.addListener(this);
    }

    @Override
    public void onServiceConnectionStateChanged(MeshServiceController.ServiceConnectionState serviceConnectionState) {
        mLogger.v(TAG, "Service connection state changed to: " + serviceConnectionState);
        if (serviceConnectionState == MeshServiceController.ServiceConnectionState.CONNECTED) {
            if (!mReceiverRegistered) {
                mAtakContext.registerReceiver(this, mIntentFilter);
                mReceiverRegistered = true;
            }

            try {
                notifyListeners(connectionStateFromServiceState(mMeshServiceController.getMeshService().connectionState()));
            } catch (RemoteException e) {
                mLogger.e(TAG, "RemoteException calling connectionState(): " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        notifyListeners(DeviceConnectionState.DISCONNECTED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(MeshServiceConstants.ACTION_MESH_CONNECTED)) {
            String extraConnected = intent.getStringExtra(MeshServiceConstants.EXTRA_CONNECTED);
            boolean connected = extraConnected.equals(MeshServiceConstants.STATE_CONNECTED);
            notifyListeners(connected ? DeviceConnectionState.CONNECTED : DeviceConnectionState.DISCONNECTED);
        }
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mAtakContext.unregisterReceiver(this);
        mReceiverRegistered = false;
    }

    private DeviceConnectionState connectionStateFromServiceState(String connectionString) {
        DeviceConnectionState connectionState = DeviceConnectionState.DISCONNECTED;

        if (connectionString == null) {
            return connectionState;
        }

        if (connectionString.equals(MeshServiceConstants.STATE_CONNECTED)
                || connectionString.equals(MeshServiceConstants.STATE_DEVICE_SLEEP)) {
            connectionState = DeviceConnectionState.CONNECTED;
        }

        return connectionState;
    }

    private void notifyListeners(DeviceConnectionState deviceConnectionState) {
        mLogger.v(TAG, "Notifying listeners of device connection state: " + deviceConnectionState);
        for (Listener listener : mListeners) {
            listener.onDeviceConnectionStateChanged(deviceConnectionState);
        }
    }
}
