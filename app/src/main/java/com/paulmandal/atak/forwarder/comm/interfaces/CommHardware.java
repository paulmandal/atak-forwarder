package com.paulmandal.atak.forwarder.comm.interfaces;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

public interface CommHardware {
    interface Listener {
        void onMessageReceived(byte[] message);
    }

    void init(@NonNull Context context, @NonNull String callsign, long gId, String atakUid);
    void destroy();
    void broadcastDiscoveryMessage();
    void createGroup(List<Long> memberGids);
    void addToGroup(List<Long> allMemberGids, List<Long> newMemberGids);
    void addListener(Listener listener);
    void removeListener(Listener listener);
}
