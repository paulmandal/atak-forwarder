package com.paulmandal.atak.forwarder.factories;

import android.app.Activity;
import android.os.Handler;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;

public class CommHardwareFactory {
    public static CommHardware createAndInitCommHardware(Activity activity,
                                                         MapView mapView,
                                                         Handler handler,
                                                         GoTennaCommHardware.GroupListener groupListener,
                                                         GroupTracker groupTracker,
                                                         CommandQueue commandQueue,
                                                         QueuedCommandFactory queuedCommandFactory) {
        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();
        long gId = longHashFromString(atakUid);

        CommHardware commHardware = new GoTennaCommHardware(handler, groupListener, groupTracker, commandQueue, queuedCommandFactory);
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
