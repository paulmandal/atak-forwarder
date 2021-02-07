package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;

public class ForwarderDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {
    public static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + ForwarderDropDownReceiver.class.getSimpleName();
    public static final String SHOW_PLUGIN = "com.paulmandal.atak.forwarder.SHOW_PLUGIN";

    private final View mTemplateView;

    private boolean mIsDropDownOpen;

    public ForwarderDropDownReceiver(final MapView mapView,
                                     final Context pluginContext,
                                     final Context atakContext,
                                     final StatusViewModel statusViewModel) {
        super(mapView);
        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        mTemplateView = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);

        StatusScreen statusScreen = new StatusScreen((ConstraintLayout) mTemplateView);

        LifecycleOwner lifecycleOwner = (LifecycleOwner) atakContext;
        statusScreen.bind(lifecycleOwner, statusViewModel, pluginContext, atakContext);
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
