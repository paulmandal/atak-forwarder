package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.CallSuper;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.Arrays;
import java.util.List;

public abstract class MeshEventHandler extends BroadcastReceiver implements ConnectionStateHandler.Listener, Destroyable {
    private final Context mAtakContext;
    protected final Logger mLogger;
    private final IntentFilter mIntentFilter;

    private final List<String> mActionsToHandle;

    private ConnectionStateHandler.ConnectionState mConnectionState;
    private boolean mReceiverRegistered;

    public MeshEventHandler(Context atakContext,
                            Logger logger,
                            String[] actionsToHandle,
                            List<Destroyable> destroyables,
                            ConnectionStateHandler connectionStateHandler) {
        mAtakContext = atakContext;
        mLogger = logger;
        mActionsToHandle = Arrays.asList(actionsToHandle);

        IntentFilter intentFilter = new IntentFilter();
        for (String action : actionsToHandle) {
            intentFilter.addAction(action);
        }
        mIntentFilter = intentFilter;

        destroyables.add(this);
        connectionStateHandler.addListener(this);
        mAtakContext.registerReceiver(this, intentFilter);
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (mConnectionState != ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED) {
            return;
        }

        String action = intent.getAction();

        if (!mActionsToHandle.contains(action)) {
            return;
        }

        Thread thread = new Thread(() -> handleReceive(context, intent));
        thread.setName(MeshEventHandler.class.getSimpleName() + ".Worker");
        thread.start();
    }

    @Override
    @CallSuper
    public void onConnectionStateChanged(ConnectionStateHandler.ConnectionState connectionState) {
        mConnectionState = connectionState;

        boolean connected = connectionState == ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED;
        if (!connected && mReceiverRegistered) {
            mReceiverRegistered = false;
            mAtakContext.unregisterReceiver(this);
        } else if (connected && !mReceiverRegistered) {
            mReceiverRegistered = true;
            mAtakContext.registerReceiver(this, mIntentFilter);
        }
    }

    @Override
    @CallSuper
    public void onDestroy(Context context, MapView mapView) {
        if (mConnectionState != ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED) {
            return;
        }
        mAtakContext.unregisterReceiver(this);
    }

    protected abstract void handleReceive(Context context, Intent intent);
}
