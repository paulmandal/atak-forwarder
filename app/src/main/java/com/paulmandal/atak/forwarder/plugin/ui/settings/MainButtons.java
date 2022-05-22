package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.preference.Preference;

import com.atakmap.android.gui.PanListPreference;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;

import java.util.List;

public class MainButtons {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + MainButtons.class.getSimpleName();

    private final Gson mGson;

    public MainButtons(DevicesList devicesList,
                       Preference setCommDevicePreference,
                       Preference refreshDevicesPreference,
                       Preference regionPreference,
                       Gson gson) {
        mGson = gson;

        PanListPreference commDevicePreference = (PanListPreference) setCommDevicePreference;
        updateCommDevices(commDevicePreference, devicesList.getMeshtasticDevices());

        refreshDevicesPreference.setOnPreferenceClickListener((Preference preference) -> {
            updateCommDevices(commDevicePreference, devicesList.getMeshtasticDevices());
            return true;
        });

        PanListPreference listPreferenceRegion = (PanListPreference) regionPreference;
        listPreferenceRegion.setEntries(R.array.regions);
        listPreferenceRegion.setEntryValues(R.array.regions_values);
    }

    private void updateCommDevices(PanListPreference commDevicePreference,
                                   List<MeshtasticDevice> meshtasticDevices) {
        String[] devices = new String[meshtasticDevices.size()];
        String[] values = new String[meshtasticDevices.size()];
        for (int i = 0 ; i < meshtasticDevices.size() ; i++) {
            MeshtasticDevice meshtasticDevice = meshtasticDevices.get(i);
            devices[i] = meshtasticDevice.toString();
            values[i] = mGson.toJson(meshtasticDevice);
        }

        commDevicePreference.setEntries(devices);
        commDevicePreference.setEntryValues(values);
    }
}
