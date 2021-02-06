package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.preference.Preference;

import com.atakmap.android.gui.PanListPreference;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;

import java.util.List;

public class MainSettingsButtons {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MainSettingsButtons.class.getSimpleName();

    public MainSettingsButtons(DevicesList devicesList,
                               Preference setCommDevicePreference,
                               Preference refreshDevicesPreference) {
        PanListPreference commDevicePreference = (PanListPreference) setCommDevicePreference;
        updateCommDevices(commDevicePreference, devicesList.getMeshtasticDevices());

        refreshDevicesPreference.setOnPreferenceClickListener((Preference preference) -> {
            updateCommDevices(commDevicePreference, devicesList.getMeshtasticDevices());
            return true;
        });
    }

    private void updateCommDevices(PanListPreference commDevicePreference,
                                   List<MeshtasticDevice> meshtasticDevices) {
        Gson gson = new Gson();
        String[] devices = new String[meshtasticDevices.size()];
        String[] values = new String[meshtasticDevices.size()];
        for (int i = 0 ; i < meshtasticDevices.size() ; i++) {
            MeshtasticDevice meshtasticDevice = meshtasticDevices.get(i);
            devices[i] = meshtasticDevice.toString();
            values[i] = gson.toJson(meshtasticDevice);
        }

        commDevicePreference.setEntries(devices);
        commDevicePreference.setEntryValues(values);
    }
}
