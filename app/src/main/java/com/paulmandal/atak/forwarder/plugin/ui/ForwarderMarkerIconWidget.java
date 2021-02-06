package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.MotionEvent;

import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.MarkerIconWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.coremap.maps.assets.Icon;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class ForwarderMarkerIconWidget extends MarkerIconWidget implements Destroyable, CommHardware.ConnectionStateListener, MapWidget.OnClickListener  {
    private final static int ICON_WIDTH = 32;
    private final static int ICON_HEIGHT = 32;

    private ForwarderDropDownReceiver mForwarderDropDownReceiver;

    public ForwarderMarkerIconWidget(MapView mapView,
                                     List<Destroyable> destroyables,
                                     ForwarderDropDownReceiver forwarderDropDownReceiver,
                                     CommHardware commHardware) {
        mForwarderDropDownReceiver = forwarderDropDownReceiver;

        destroyables.add(this);

        commHardware.addConnectionStateListener(this);

        setName("Forwarder Status");
        addOnClickListener(this);

        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.addWidget(this);

        updateIcon(commHardware.getConnectionState());

    }

    @Override
    public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
        if (widget == this) {
            if (!mForwarderDropDownReceiver.isDropDownOpen()) {
                Intent intent = new Intent();
                intent.setAction(ForwarderDropDownReceiver.SHOW_PLUGIN);
                AtakBroadcast.getInstance().sendBroadcast(intent);
            } else {
                DropDownManager.getInstance().unHidePane();
            }
        }
    }

    @Override
    public void onConnectionStateChanged(CommHardware.ConnectionState connectionState) {
        updateIcon(connectionState);
    }

    private void updateIcon(CommHardware.ConnectionState connectionState) {
        int drawableId = R.drawable.ic_no_service_connected;
        switch (connectionState) {
            case NO_SERVICE_CONNECTION:
                drawableId = R.drawable.ic_no_service_connected;
                break;
            case NO_DEVICE_CONFIGURED:
                drawableId = R.drawable.ic_no_device_configured;
                break;
            case DEVICE_DISCONNECTED:
                drawableId = R.drawable.ic_device_disconnected;
                break;
            case DEVICE_CONNECTED:
                drawableId = R.drawable.ic_device_connected;
                break;
        }

        String imageUri = "android.resource://com.paulmandal.atak.forwarder/" + drawableId;

        Icon.Builder builder = new Icon.Builder();
        builder.setAnchor(0, 0);
        builder.setColor(Icon.STATE_DEFAULT, Color.WHITE);
        builder.setSize(ICON_WIDTH, ICON_HEIGHT);
        builder.setImageUri(Icon.STATE_DEFAULT, imageUri);

        Icon icon = builder.build();
        setIcon(icon);
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.removeWidget(this);
    }
}
