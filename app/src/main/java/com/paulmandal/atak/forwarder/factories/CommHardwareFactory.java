package com.paulmandal.atak.forwarder.factories;

import android.app.Activity;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

public class CommHardwareFactory {
    public static CommHardware createAndInitCommHardware(Activity activity, MapView mapView, GoTennaCommHardware.GroupListener groupListener, GroupTracker groupTracker) {
        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();
        long gId = longHashFromString(atakUid);

        CommHardware commHardware = new GoTennaCommHardware(groupListener, groupTracker);
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
