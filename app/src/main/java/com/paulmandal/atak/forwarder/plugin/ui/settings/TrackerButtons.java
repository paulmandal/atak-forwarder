package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.preference.Preference;

public class TrackerButtons {
    public TrackerButtons(DevicesList devicesList, Preference writeToDevice) {
        writeToDevice.setOnPreferenceClickListener((Preference preference) -> {
            // show dialog with picker for device (minus comm device) and text entry for name
            // call Non-atak writer thingy
            return true;
        });
    }
}
