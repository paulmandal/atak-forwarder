package com.paulmandal.atak.forwarder.persistence;

import android.content.SharedPreferences;

public class PreferencesReader {
    public ConfigState readConfigFromPreferences(SharedPreferences preferences) {
        ConfigState configState = new ConfigState();
        // TODO: Read the whole thing w/ defaults?
        return configState;
    }
}
