package com.paulmandal.atak.forwarder.factories;

import android.app.Activity;
import android.os.Handler;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.persistence.StateStorage;

public class CommHardwareFactory {
    public static CommHardware createAndInitCommHardware(Activity activity,
                                                         MapView mapView,
                                                         Handler handler,
                                                         MeshtasticCommHardware.UserListener userListener,
                                                         UserTracker channelTracker,
                                                         CommandQueue commandQueue,
                                                         QueuedCommandFactory queuedCommandFactory,
                                                         StateStorage stateStorage) {
        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();

        CommHardware commHardware;
        UserInfo selfInfo = new UserInfo(callsign, null, atakUid, null);
        commHardware = new MeshtasticCommHardware(handler, userListener, channelTracker, commandQueue, queuedCommandFactory, activity, selfInfo, stateStorage, stateStorage.getCommDeviceAddress());
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
