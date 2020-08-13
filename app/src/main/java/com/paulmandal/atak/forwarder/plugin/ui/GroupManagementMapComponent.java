package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

public class GroupManagementMapComponent extends DropDownMapComponent {
    private static final String TAG = "ATAKDBG." + GroupManagementMapComponent.class.getSimpleName();

    private Context mPluginContext;
    private GroupManagementDropDownReceiver mDropDownReceiver;

    private GroupTracker mGroupTracker;
    private CommHardware mCommHardware;

    public GroupManagementMapComponent(GroupTracker groupTracker, CommHardware commHardware) {
        mGroupTracker = groupTracker;
        mCommHardware = commHardware;
    }

    public void onCreate(final Context context, Intent intent,
                         final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        mPluginContext = context;

        mDropDownReceiver = new GroupManagementDropDownReceiver(view, context, mGroupTracker, mCommHardware);

        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(GroupManagementDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(mDropDownReceiver, ddFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }
}
