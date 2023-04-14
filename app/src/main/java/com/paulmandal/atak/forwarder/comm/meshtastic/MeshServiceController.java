package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import com.atakmap.android.maps.MapView;
import com.geeksville.mesh.IMeshService;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshServiceController implements Destroyable {
    public enum ServiceConnectionState {
        DISCONNECTED,
        CONNECTED
    }

    public interface Listener {
        void onServiceConnectionStateChanged(ServiceConnectionState serviceConnectionState);
    }

    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshServiceController.class.getSimpleName();

    private final Context mAtakContext;
    private final Handler mUiThreadHandler;
    private final Logger mLogger;

    private IMeshService mMeshService;
    private final ServiceConnection mServiceConnection;

    private final Intent mServiceIntent;

    private ServiceConnectionState mConnectionState = ServiceConnectionState.DISCONNECTED;
    private boolean mConnectedToService;

    private final Set<Listener> mConnectionStateListeners = new CopyOnWriteArraySet<>();

    public MeshServiceController(List<Destroyable> destroyables,
                                 Context atakContext,
                                 Handler uiThreadHandler,
                                 Logger logger) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mLogger = logger;

        destroyables.add(this);

        // Set up service connection
        mServiceIntent = new Intent();
        mServiceIntent.setClassName(MeshServiceConstants.PACKAGE_NAME, MeshServiceConstants.CLASS_NAME);

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                logger.v(TAG, "Service connected");
                mMeshService = IMeshService.Stub.asInterface(service);
                mConnectedToService = true;

                updateConnectionState(ServiceConnectionState.CONNECTED);
            }

            public void onServiceDisconnected(ComponentName className) {
                logger.e(TAG, "Service has unexpectedly disconnected");
                mMeshService = null;
                mConnectedToService = false;
                updateConnectionState(ServiceConnectionState.DISCONNECTED);
            }
        };

        bindToService();
    }

    public IMeshService getMeshService() {
        return mMeshService;
    }
    public ServiceConnectionState getConnectionState() {
        return mConnectionState;
    }

    public void addListener(Listener listener) {
        mConnectionStateListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mConnectionStateListeners.remove(listener);
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mLogger.v(TAG, "onDestroy()");
        unbindAndStopService();
    }

    private void unbindAndStopService() {
        if (!mConnectedToService) {
            return;
        }

        unBindService();
        stopService();
    }

    private void updateConnectionState(ServiceConnectionState connectionState) {
        mLogger.d(TAG, "Updating service connection state to: " + connectionState);
        mConnectionState = connectionState;
        notifyConnectionStateListeners();
    }

    private void notifyConnectionStateListeners() {
        for (Listener listener : mConnectionStateListeners) {
            mUiThreadHandler.post(() -> listener.onServiceConnectionStateChanged(mConnectionState));
        }
    }

    private void bindToService() {
        mLogger.v(TAG, "Binding to service");
        mAtakContext.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        mAtakContext.unbindService(mServiceConnection);
    }

    private void stopService() {
        mAtakContext.stopService(mServiceIntent);
    }
}
