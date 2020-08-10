package com.paulmandal.atak.forwarder.interfaces;

public interface CommHardware {
    interface Listener {
        void onMessageReceived(String message);
    }

    void init();
    void sendMessage(String message);
    void addListener(Listener listener);
    void removeListener(Listener listener);
}
