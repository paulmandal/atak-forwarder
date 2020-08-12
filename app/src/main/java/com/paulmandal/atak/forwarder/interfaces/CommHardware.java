package com.paulmandal.atak.forwarder.interfaces;

import android.content.Context;

public interface CommHardware {
    interface Listener {
        void onMessageReceived(byte[] message);
    }

    void init(Context context);
    void destroy();
    void sendMessage(byte[] message);
    void addListener(Listener listener);
    void removeListener(Listener listener);
}
