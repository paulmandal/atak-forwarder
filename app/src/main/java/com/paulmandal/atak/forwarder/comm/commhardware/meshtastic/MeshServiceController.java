package com.paulmandal.atak.forwarder.comm.commhardware.meshtastic;

import com.paulmandal.atak.forwarder.plugin.SuspendListener;

import java.util.List;

public class MeshServiceController {
    private List<SuspendListener> mSuspendListenerComponents;

    public void addSuspendableComponent(SuspendListener component) { // TODO: rename?
        mSuspendListenerComponents.add(component);

        // TODO: register for broadcast listeners
    }

    public void removeSuspendableComponent(SuspendListener component) {
        mSuspendListenerComponents.remove(component);

        // TODO: register for broadcast listeners
    }

    public void setSuspended(boolean suspended) {
        // TODO: iterate over suspendable components and register/unregister them
    }
}
