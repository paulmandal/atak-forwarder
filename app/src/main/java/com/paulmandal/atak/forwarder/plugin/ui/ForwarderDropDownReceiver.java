package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.TabHost;

import androidx.lifecycle.LifecycleOwner;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;

public class ForwarderDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + ForwarderDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.paulmandal.atak.forwarder.SHOW_PLUGIN";

    private final View mTemplateView;

    private boolean mIsDropDownOpen;

    public ForwarderDropDownReceiver(final MapView mapView,
                                     final Context pluginContext,
                                     final Context atakContext,
                                     final StatusViewModel statusViewModel,
                                     final LoggingViewModel loggingViewModel) {
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

        spec = tabs.newTabSpec("tab_logging");
        spec.setContent(R.id.tab_logging);
        spec.setIndicator("Logging");
        tabs.addTab(spec);

        // Fix tabs being too large
        for (int i = 0; i < tabs.getTabWidget().getChildCount(); i++) {
                tabs.getTabWidget().getChildAt(i).getLayoutParams().height /= 2;
        }

        // Set up the rest of the UI
        LifecycleOwner lifecycleOwner = (LifecycleOwner) atakContext;

        StatusScreen statusScreen = mTemplateView.findViewById(R.id.tab_status);
        statusScreen.bind(lifecycleOwner, statusViewModel, pluginContext, atakContext);

        LoggingScreen loggingScreen = mTemplateView.findViewById(R.id.tab_logging);
        loggingScreen.bind(lifecycleOwner, pluginContext, loggingViewModel);
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
