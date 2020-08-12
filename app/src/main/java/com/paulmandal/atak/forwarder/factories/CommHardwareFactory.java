package com.paulmandal.atak.forwarder.factories;

import android.content.Context;

import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;

public class CommHardwareFactory {
    public static GoTennaCommHardware mCommHardwareInstance;

    public static GoTennaCommHardware getCommHardware(Context context) {
        if (mCommHardwareInstance == null) {
            mCommHardwareInstance = new GoTennaCommHardware();
            mCommHardwareInstance.init(context);
        }

        return mCommHardwareInstance;
    }
}
