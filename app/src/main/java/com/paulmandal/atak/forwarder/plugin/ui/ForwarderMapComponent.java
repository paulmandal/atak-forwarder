package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.AdvancedTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.ChannelTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusTabViewModel;

public class ForwarderMapComponent extends DropDownMapComponent {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ForwarderMapComponent.class.getSimpleName();

    private ForwarderMarkerIconWidget mForwarderMarkerIconWidget;

    private ChannelTracker mChannelTracker;
    private CommHardware mCommHardware;
    private CotMessageCache mCotMessageCache;
    private CommandQueue mCommandQueue;
    private StatusTabViewModel mStatusTabViewModel;
    private ChannelTabViewModel mChannelTabViewModel;
    private DevicesTabViewModel mDevicesTabViewModel;

    public ForwarderMapComponent(ChannelTracker channelTracker,
                                 CommHardware commHardware,
                                 CotMessageCache cotMessageCache,
                                 CommandQueue commandQueue,
                                 StatusTabViewModel statusTabViewModel,
                                 ChannelTabViewModel channelTabViewModel,
                                 DevicesTabViewModel devicesTabViewModel) {
        mChannelTracker = channelTracker;
        mCommHardware = commHardware;
        mCotMessageCache = cotMessageCache;
        mCommandQueue = commandQueue;
        mStatusTabViewModel= statusTabViewModel;
        mChannelTabViewModel = channelTabViewModel;
        mDevicesTabViewModel = devicesTabViewModel;
    }

    public void onCreate(final Context pluginContext, Intent intent,
                         final MapView mapView) {
        pluginContext.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(pluginContext, intent, mapView);

        Context atakContext = mapView.getContext();

        AdvancedTab advancedTab = new AdvancedTab(atakContext, mCommandQueue, mCotMessageCache);

        ForwarderDropDownReceiver forwarderDropDownReceiver = new ForwarderDropDownReceiver(mapView,
                pluginContext,
                atakContext,
                mStatusTabViewModel,
                mChannelTabViewModel,
                mDevicesTabViewModel,
                advancedTab);

        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(ForwarderDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(forwarderDropDownReceiver, ddFilter);

        mForwarderMarkerIconWidget = new ForwarderMarkerIconWidget(mapView, forwarderDropDownReceiver, mCommHardware);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        mForwarderMarkerIconWidget.onDestroy();
    }
}
