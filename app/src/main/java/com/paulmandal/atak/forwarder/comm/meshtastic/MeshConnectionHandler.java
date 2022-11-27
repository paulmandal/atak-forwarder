package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.os.RemoteException;

import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshConnectionHandler implements MeshServiceController.Listener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MeshConnectionHandler.class.getSimpleName();

    public enum DeviceConnectionState {
        DISCONNECTED,
        CONNECTED
    }

    public interface Listener {
        void onDeviceConnectionStateChanged(DeviceConnectionState deviceConnectionState);
    }

    private final MeshServiceController mMeshServiceController;
    private final Logger mLogger;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    public MeshConnectionHandler(MeshServiceController meshServiceController,
                                 Logger logger) {
        mMeshServiceController = meshServiceController;
        mLogger = logger;

        meshServiceController.addListener(this);
    }

    @Override
    public void onServiceConnectionStateChanged(MeshServiceController.ServiceConnectionState serviceConnectionState) {
        if (serviceConnectionState == MeshServiceController.ServiceConnectionState.CONNECTED) {
            try {
                notifyListeners(connectionStateFromServiceState(mMeshServiceController.getMeshService().connectionState()));
            } catch (RemoteException e) {
                mLogger.e(TAG, "RemoteException calling connectionState(): " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private DeviceConnectionState connectionStateFromServiceState(String connectionString) {
        mLogger.d(TAG, "ConnectionStateString: " + connectionString);
        DeviceConnectionState connectionState = DeviceConnectionState.DISCONNECTED;

        if (connectionString.equals(MeshServiceConstants.STATE_CONNECTED)
                || connectionString.equals(MeshServiceConstants.STATE_DEVICE_SLEEP)) {
            connectionState = DeviceConnectionState.CONNECTED;
        }

        return connectionState;
    }

    private void notifyListeners(DeviceConnectionState deviceConnectionState) {
        for (Listener listener : mListeners) {
            listener.onDeviceConnectionStateChanged(deviceConnectionState);
        }
    }
}
