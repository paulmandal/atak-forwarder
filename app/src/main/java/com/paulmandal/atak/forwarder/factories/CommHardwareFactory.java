package com.paulmandal.atak.forwarder.factories;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class CommHardwareFactory {
    public static CommHardware createAndInitCommHardware(Context atakContext,
                                                         MapView mapView,
                                                         Handler handler,
                                                         MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                                         MeshtasticCommHardware.UserListener userListener,
                                                         UserTracker userTracker,
                                                         CommandQueue commandQueue,
                                                         QueuedCommandFactory queuedCommandFactory,
                                                         SharedPreferences sharedPreferences,
                                                         List<Destroyable> destroyables) {
        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();

        CommHardware commHardware;
        UserInfo selfInfo = new UserInfo(callsign, null, atakUid, null);
        commHardware = new MeshtasticCommHardware(destroyables, sharedPreferences, atakContext, handler, meshtasticDeviceSwitcher, userListener, userTracker, commandQueue, queuedCommandFactory, selfInfo);
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
