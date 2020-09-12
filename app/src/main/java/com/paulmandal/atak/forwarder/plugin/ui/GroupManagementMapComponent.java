package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.group.ChannelTracker;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.AdvancedTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.ChannelTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.SettingsTab;

public class GroupManagementMapComponent extends DropDownMapComponent {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + GroupManagementMapComponent.class.getSimpleName();

    private ForwarderMarkerIconWidget mForwarderMarkerIconWidget;

    private ChannelTracker mChannelTracker;
    private CommHardware mCommHardware;
    private CotMessageCache mCotMessageCache;
    private CommandQueue mCommandQueue;


    public GroupManagementMapComponent(ChannelTracker channelTracker,
                                       CommHardware commHardware,
                                       CotMessageCache cotMessageCache,
                                       CommandQueue commandQueue) {
        mChannelTracker = channelTracker;
        mCommHardware = commHardware;
        mCotMessageCache = cotMessageCache;
        mCommandQueue = commandQueue;
    }

    public void onCreate(final Context pluginContext, Intent intent,
                         final MapView mapView) {
        pluginContext.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(pluginContext, intent, mapView);

        Context atakContext = mapView.getContext();

        SettingsTab settingsTab = new SettingsTab(pluginContext, atakContext, mChannelTracker, mCommHardware, mCommandQueue);
        ChannelTab channelTab = new ChannelTab(pluginContext, mCommHardware, mChannelTracker, new QrHelper());
        AdvancedTab advancedTab = new AdvancedTab(atakContext, mCommandQueue, mCotMessageCache);

        GroupManagementDropDownReceiver groupManagementDropDownReceiver = new GroupManagementDropDownReceiver(mapView,
                pluginContext,
                settingsTab,
                channelTab,
                advancedTab);

        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(GroupManagementDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(groupManagementDropDownReceiver, ddFilter);

        mForwarderMarkerIconWidget = new ForwarderMarkerIconWidget(mapView, groupManagementDropDownReceiver, mCommHardware);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        mForwarderMarkerIconWidget.onDestroy();
    }
}
