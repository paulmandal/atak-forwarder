package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TabHost;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.AdvancedTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.ChannelTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.SettingsTab;

public class GroupManagementDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = Config.DEBUG_TAG_PREFIX + GroupManagementDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.paulmandal.atak.forwarder.SHOW_PLUGIN";

    private SettingsTab mSettingsTab;
    private ChannelTab mChannelTab;
    private AdvancedTab mAdvancedTab;

    private final View mTemplateView;

    private boolean mIsDropDownOpen;

    public GroupManagementDropDownReceiver(final MapView mapView,
                                           final Context pluginContext,
                                           final SettingsTab settingsTab,
                                           final ChannelTab channelTab,
                                           final AdvancedTab advancedTab) {
        super(mapView);
        mSettingsTab = settingsTab;
        mChannelTab = channelTab;
        mAdvancedTab = advancedTab;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        mTemplateView = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);

        // Set up tabs
        TabHost tabs = (TabHost) mTemplateView.findViewById(R.id.tab_host);
        tabs.setup();

        TabHost.TabSpec spec = tabs.newTabSpec("tab_settings");
        spec.setContent(R.id.tab_settings);
        spec.setIndicator("Settings");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tab_channel");
        spec.setContent(R.id.tab_channel);
        spec.setIndicator("Channel");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tab_advanced");
        spec.setContent(R.id.tab_advanced);
        spec.setIndicator("Advanced");
        tabs.addTab(spec);

        // Set up the rest of the UI
        settingsTab.init(mTemplateView);
        channelTab.init(mTemplateView);
        advancedTab.init(mTemplateView);
    }

    public void disposeImpl() {
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {
            showDropDown(mTemplateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean isVisible) {
        mIsDropDownOpen = true;
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
        mIsDropDownOpen = false;
    }


    public boolean isDropDownOpen() {
        return mIsDropDownOpen;
    }

}
