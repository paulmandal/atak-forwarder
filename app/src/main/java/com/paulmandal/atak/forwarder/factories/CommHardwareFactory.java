package com.paulmandal.atak.forwarder.factories;

import android.app.Activity;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.group.GroupTracker;

public class CommHardwareFactory {
    public static CommHardware createAndInitCommHardware(Activity activity,
                                                         MapView mapView,
                                                         GoTennaCommHardware.GroupListener groupListener,
                                                         GroupTracker groupTracker,
                                                         MessageQueue messageQueue) {
        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();
        long gId = longHashFromString(atakUid);

        CommHardware commHardware = new GoTennaCommHardware(groupListener, groupTracker, messageQueue);
        commHardware.init(activity, callsign, gId, atakUid);
        return commHardware;
    }

    private static long longHashFromString(String s) {
        long hash = 0;
        char[] val = s.toCharArray();
        for (char c : val) {
            hash = 31 * hash + c;
        }
        return hash;
    }

}
