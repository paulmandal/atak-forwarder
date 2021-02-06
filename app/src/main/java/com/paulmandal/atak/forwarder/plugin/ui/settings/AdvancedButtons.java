package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.preference.Preference;

import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;

public class AdvancedButtons {
    public AdvancedButtons(CotMessageCache cotMessageCache,
                           CommandQueue commandQueue,
                           Preference clearMessageCache,
                           Preference clearCommandQueue,
                           Preference resetToDefault,
                           Preference resetToDefaultIncludingChannel) {
        clearMessageCache.setOnPreferenceClickListener((Preference preference) -> {
            cotMessageCache.clearData();
            return true;
        });
        clearCommandQueue.setOnPreferenceClickListener((Preference preference) -> {
            commandQueue.clearData();
            return true;
        });
        resetToDefault.setOnPreferenceClickListener((Preference preference) -> {
            // TODO: implement reset to default
            return true;
        });
        resetToDefaultIncludingChannel.setOnPreferenceClickListener((Preference preference) -> {
            // TODO: implement reseting to default
            return true;
        });
    }
}
