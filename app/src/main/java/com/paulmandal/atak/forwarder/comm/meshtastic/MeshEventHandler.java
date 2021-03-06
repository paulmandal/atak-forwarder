package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.CallSuper;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.SuspendListener;

import java.util.Arrays;
import java.util.List;

public abstract class MeshEventHandler extends BroadcastReceiver implements SuspendListener, Destroyable {
    private final Context mAtakContext;
    protected final Logger mLogger;
    private final IntentFilter mIntentFilter;

    private final List<String> mActionsToHandle;

    private boolean mIsSuspended = false;
    private boolean mReceiverRegistered;

    public MeshEventHandler(Context atakContext,
                            Logger logger,
                            String[] actionsToHandle,
                            List<Destroyable> destroyables,
                            MeshSuspendController meshSuspendController) {
        mAtakContext = atakContext;
        mLogger = logger;
        mActionsToHandle = Arrays.asList(actionsToHandle);

        IntentFilter intentFilter = new IntentFilter();
        for (String action : actionsToHandle) {
            intentFilter.addAction(action);
        }
        mIntentFilter = intentFilter;

        destroyables.add(this);
        meshSuspendController.addSuspendListener(this);
        mAtakContext.registerReceiver(this, intentFilter);
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (mIsSuspended) {
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
    public void onSuspendedChanged(boolean suspended) {
        mIsSuspended = suspended;

        if (suspended && mReceiverRegistered) {
            mReceiverRegistered = false;
            mAtakContext.unregisterReceiver(this);
        } else if (!suspended && !mReceiverRegistered) {
            mReceiverRegistered = true;
            mAtakContext.registerReceiver(this, mIntentFilter);
        }
    }

    @Override
    @CallSuper
    public void onDestroy(Context context, MapView mapView) {
        if (mIsSuspended) {
            return;
        }
        mAtakContext.unregisterReceiver(this);
    }

    protected abstract void handleReceive(Context context, Intent intent);
}
