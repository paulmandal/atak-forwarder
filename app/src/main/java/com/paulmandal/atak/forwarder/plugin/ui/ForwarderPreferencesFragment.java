package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.preference.PluginPreferenceFragment;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.persistence.PreferencesKeys;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;
import com.paulmandal.atak.forwarder.plugin.ui.settings.MainSettingsButtons;

public class ForwarderPreferencesFragment extends PluginPreferenceFragment {
    private static Context sPluginContext;
    private static DevicesList sDevicesList;

    public ForwarderPreferencesFragment() {
        super(sPluginContext, R.xml.preferences);
    }

    @SuppressWarnings("ValidFragment")
    public ForwarderPreferencesFragment(final Context pluginContext,
                                        final DevicesList devicesList) {
        super(pluginContext, R.xml.preferences);
        this.sPluginContext = pluginContext;
        this.sDevicesList = devicesList;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainSettingsButtons mainSettingsButtons = new MainSettingsButtons(sDevicesList, findPreference(PreferencesKeys.KEY_SET_COMM_DEVICE), findPreference(PreferencesKeys.KEY_REFRESH_COMM_DEVICES));
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", sPluginContext.getString(R.string.preferences_title));
    }
}
