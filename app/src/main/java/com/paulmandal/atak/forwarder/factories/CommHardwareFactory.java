package com.paulmandal.atak.forwarder.factories;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticChannelConfigurer;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDeviceConfigurer;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class CommHardwareFactory {
    public static MeshtasticCommHardware createAndInitMeshtasticCommHardware(Context atakContext,
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

        MeshtasticCommHardware meshtasticCommHardware;
        UserInfo selfInfo = new UserInfo(callsign, null, atakUid, null);
        MeshtasticDeviceConfigurer meshtasticDeviceConfigurer = new MeshtasticDeviceConfigurer(selfInfo);
        MeshtasticChannelConfigurer meshtasticChannelConfigurer = new MeshtasticChannelConfigurer(userTracker);
        meshtasticCommHardware = new MeshtasticCommHardware(destroyables, sharedPreferences, atakContext, handler, meshtasticDeviceSwitcher, meshtasticDeviceConfigurer, meshtasticChannelConfigurer, userListener, userTracker, commandQueue, queuedCommandFactory, selfInfo);
        return meshtasticCommHardware;
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
