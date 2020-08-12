package com.paulmandal.atak.forwarder.factories;

import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;

public class CommHardwareFactory {
    public static GoTennaCommHardware mCommHardwareInstance;

    public static GoTennaCommHardware getCommHardware() {
        if (mCommHardwareInstance == null) {
            mCommHardwareInstance = new GoTennaCommHardware();
            mCommHardwareInstance.init();
        }

        return mCommHardwareInstance;
    }
}
