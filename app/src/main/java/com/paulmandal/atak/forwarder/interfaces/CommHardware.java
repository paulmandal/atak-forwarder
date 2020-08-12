package com.paulmandal.atak.forwarder.interfaces;

public interface CommHardware {
    interface Listener {
        void onMessageReceived(byte[] message);
    }

    void init();
    void destroy();
    void sendMessage(byte[] message);
    void addListener(Listener listener);
    void removeListener(Listener listener);
}
