package com.paulmandal.atak.forwarder.comm.meshtastic;

import com.paulmandal.atak.forwarder.plugin.SuspendListener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MeshSuspendController {
    private boolean mIsSuspended = false;
    private final Set<SuspendListener> mListeners = new CopyOnWriteArraySet<>();

    public void addSuspendListener(SuspendListener listener) {
        mListeners.add(listener);
    }

    public void removeSuspendListener(SuspendListener listener) {
        mListeners.remove(listener);
    }

    public void setSuspended(boolean suspended) {
        if (suspended != mIsSuspended) {
            mIsSuspended = suspended;
        }
        for (SuspendListener listener : mListeners) {
            listener.onSuspendedChanged(suspended);
        }
    }
}
