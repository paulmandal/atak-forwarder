package com.paulmandal.atak.forwarder.plugin.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.MarkerIconWidget;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.group.GroupTracker;

public class GroupManagementMapComponent extends DropDownMapComponent  {
    private static final String TAG = "ATAKDBG." + GroupManagementMapComponent.class.getSimpleName();

    private Context mPluginContext;
    private GroupManagementDropDownReceiver mDropDownReceiver;
    private MarkerIconWidget mMarkerIconWidget;

    private Activity mActivity;
    private GroupTracker mGroupTracker;
    private CommHardware mCommHardware;
    private CotMessageCache mCotMessageCache;
    private MessageQueue mMessageQueue;

    private ForwarderMarkerIconWidget mForwarderMarkerIconWidget;

    public GroupManagementMapComponent(Activity activity,
                                       GroupTracker groupTracker,
                                       CommHardware commHardware,
                                       CotMessageCache cotMessageCache,
                                       MessageQueue messageQueue) {
        mActivity = activity;
        mGroupTracker = groupTracker;
        mCommHardware = commHardware;
        mCotMessageCache = cotMessageCache;
        mMessageQueue = messageQueue;
    }

    public void onCreate(final Context context, Intent intent,
                         final MapView mapView) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, mapView);
        mPluginContext = context;

        mDropDownReceiver = new GroupManagementDropDownReceiver(mapView, context, mActivity, mGroupTracker, mCommHardware, mCotMessageCache, mMessageQueue);

        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(GroupManagementDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(mDropDownReceiver, ddFilter);

        mForwarderMarkerIconWidget = new ForwarderMarkerIconWidget(mapView, mDropDownReceiver, mCommHardware);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        mForwarderMarkerIconWidget.onDestroy();
    }
}
