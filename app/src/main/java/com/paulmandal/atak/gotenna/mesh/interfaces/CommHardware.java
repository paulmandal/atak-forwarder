package com.paulmandal.atak.gotenna.mesh.interfaces;

public interface CommHardware {
    interface Listener {
        void onMessageReceived(String message);
    }

    void init();
    void sendMessage(String message);
    void addListener(Listener listener);
    void removeListener(Listener listener);
}
