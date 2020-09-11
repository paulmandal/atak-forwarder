package com.paulmandal.atak.forwarder.factories;

import android.app.Activity;
import android.os.Handler;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.UserInfo;

public class CommHardwareFactory {
    public static CommHardware createAndInitCommHardware(Activity activity,
                                                         MapView mapView,
                                                         Handler handler,
                                                         MeshtasticCommHardware.GroupListener groupListener,
                                                         GroupTracker groupTracker,
                                                         CommandQueue commandQueue,
                                                         QueuedCommandFactory queuedCommandFactory) {
        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();

        CommHardware commHardware;
        if (!Config.GOTENNA_SDK_TOKEN.isEmpty()) {
//            commHardware = new GoTennaCommHardware(handler, groupListener, groupTracker, commandQueue, queuedCommandFactory);
            throw new RuntimeException("GoTenna Mesh is not currently supported.");
        } else {
            UserInfo selfInfo = new UserInfo(callsign, null, atakUid, false);
            commHardware = new MeshtasticCommHardware(handler, groupListener, groupTracker, commandQueue, queuedCommandFactory, activity, selfInfo);
        }
//        commHardware.init(activity, callsign, gId, atakUid);
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
