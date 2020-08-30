package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.group.GroupTracker;

public class GroupManagementMapComponent extends DropDownMapComponent  {
    private static final String TAG = "ATAKDBG." + GroupManagementMapComponent.class.getSimpleName();

    private ForwarderMarkerIconWidget mForwarderMarkerIconWidget;

    private GroupTracker mGroupTracker;
    private CommHardware mCommHardware;
    private CotMessageCache mCotMessageCache;
    private CommandQueue mCommandQueue;


    public GroupManagementMapComponent(GroupTracker groupTracker,
                                       CommHardware commHardware,
                                       CotMessageCache cotMessageCache,
                                       CommandQueue commandQueue) {
        mGroupTracker = groupTracker;
        mCommHardware = commHardware;
        mCotMessageCache = cotMessageCache;
        mCommandQueue = commandQueue;
    }

    public void onCreate(final Context context, Intent intent,
                         final MapView mapView) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, mapView);

        GroupManagementDropDownReceiver groupManagementDropDownReceiver = new GroupManagementDropDownReceiver(mapView, context, mapView.getContext(), mGroupTracker, mCommHardware, mCotMessageCache, mCommandQueue);

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
