package com.paulmandal.atak.forwarder.factories;

import android.app.Activity;
import android.os.Handler;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.persistence.JsonHelper;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;

import java.util.ArrayList;

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

        CommHardware commHardware;
        if (!Config.GOTENNA_SDK_TOKEN.isEmpty()) {
            commHardware = new GoTennaCommHardware(handler, groupListener, groupTracker, commandQueue, queuedCommandFactory);
        } else {
            GroupTracker fakeGroupTracker = new GroupTracker(activity, handler, new StateStorage(activity, new JsonHelper()), null, new GroupInfo(0, new ArrayList<>())); // TODO: clean up hax
            commHardware = new MeshtasticCommHardware(handler, fakeGroupTracker, commandQueue, queuedCommandFactory, activity);
        }
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
