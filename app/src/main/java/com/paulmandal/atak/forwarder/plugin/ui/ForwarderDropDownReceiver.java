package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;

import androidx.lifecycle.LifecycleOwner;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.ChannelTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.DevicesTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.StatusTab;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.ChannelTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusTabViewModel;

public class ForwarderDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = Config.DEBUG_TAG_PREFIX + ForwarderDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.paulmandal.atak.forwarder.SHOW_PLUGIN";

    private final View mTemplateView;

    private boolean mIsDropDownOpen;

    public ForwarderDropDownReceiver(final MapView mapView,
                                     final Context pluginContext,
                                     final Context atakContext,
                                     final StatusTabViewModel statusTabViewModel,
                                     final ChannelTabViewModel channelTabViewModel,
                                     final DevicesTabViewModel devicesTabViewModel) {
        super(mapView);
        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        mTemplateView = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);

        // Set up tabs
        TabHost tabs = mTemplateView.findViewById(R.id.tab_host);
        tabs.setup();
        TabHost.TabSpec spec = tabs.newTabSpec("tab_status");
        spec.setContent(R.id.tab_status);
        spec.setIndicator("Status");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tab_channel");
        spec.setContent(R.id.tab_channel);
        spec.setIndicator("Channel");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tab_devices");
        spec.setContent(R.id.tab_devices);
        spec.setIndicator("Devices");
        tabs.addTab(spec);

        TabWidget tabWidget = tabs.getTabWidget();
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            View view = tabWidget.getChildAt(i);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = dpToPx(32);
            view.setLayoutParams(layoutParams);
        }

        // Set up the rest of the UI
        LifecycleOwner lifecycleOwner = (LifecycleOwner) atakContext;

        StatusTab statusTab = mTemplateView.findViewById(R.id.tab_status);
        statusTab.bind(lifecycleOwner, statusTabViewModel, pluginContext, atakContext);

        ChannelTab channelTab = mTemplateView.findViewById(R.id.tab_channel);
        channelTab.bind(lifecycleOwner, channelTabViewModel, pluginContext, atakContext);

        DevicesTab devicesTab = mTemplateView.findViewById(R.id.tab_devices);
        devicesTab.bind(lifecycleOwner, devicesTabViewModel, pluginContext, atakContext);
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
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
