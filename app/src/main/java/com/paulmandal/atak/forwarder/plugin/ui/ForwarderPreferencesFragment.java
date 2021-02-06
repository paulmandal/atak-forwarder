package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.preference.PluginPreferenceFragment;
import com.paulmandal.atak.forwarder.R;

public class ForwarderPreferencesFragment extends PluginPreferenceFragment {
    private static Context sPluginContext;

    public ForwarderPreferencesFragment() {
        super(sPluginContext, R.xml.preferences);
    }

    @SuppressWarnings("ValidFragment")
    public ForwarderPreferencesFragment(final Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        this.sPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", sPluginContext.getString(R.string.preferences_title));
    }
}
