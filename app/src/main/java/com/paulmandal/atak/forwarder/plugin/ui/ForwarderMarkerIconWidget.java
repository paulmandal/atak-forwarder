package com.paulmandal.atak.forwarder.plugin.ui;

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

public class ForwarderMarkerIconWidget extends MarkerIconWidget implements CommHardware.ScanListener, MapWidget.OnClickListener  {
    private final static int ICON_WIDTH = 32;
    private final static int ICON_HEIGHT = 32;

    private GroupManagementDropDownReceiver mGroupManagementDropDownReceiver;
    private MapView mMapView;

    public ForwarderMarkerIconWidget(MapView mapView,
                                     GroupManagementDropDownReceiver groupManagementDropDownReceiver,
                                     CommHardware commHardware) {
        mGroupManagementDropDownReceiver = groupManagementDropDownReceiver;
        mMapView = mapView;

        commHardware.addScanListener(this);

        setName("Forwarder Status");
        addOnClickListener(this);

        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.addWidget(this);

        updateIcon(commHardware.isConnected());
    }

    @Override
    public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
        if (widget == this) {
            if (!mGroupManagementDropDownReceiver.isDropDownOpen()) {
                Intent intent = new Intent();
                intent.setAction(GroupManagementDropDownReceiver.SHOW_PLUGIN);
                AtakBroadcast.getInstance().sendBroadcast(intent);
            } else {
                DropDownManager.getInstance().unHidePane();
            }
        }
    }

    @Override
    public void onScanStarted() {
        updateIcon(false);
    }

    @Override
    public void onScanTimeout() {
        updateIcon(false);
    }

    @Override
    public void onDeviceConnected() {
        updateIcon(true);
    }

    @Override
    public void onDeviceDisconnected() {
        updateIcon(false);
    }

    public void onDestroy() {
        RootLayoutWidget root = (RootLayoutWidget) mMapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.removeWidget(this);
    }

    private void updateIcon(boolean connected) {
        int drawableId = connected ? R.drawable.ic_connected : R.drawable.ic_disconnected;
        String imageUri = "android.resource://com.paulmandal.atak.forwarder/" + drawableId;

        Icon.Builder builder = new Icon.Builder();
        builder.setAnchor(0, 0);
        builder.setColor(Icon.STATE_DEFAULT, Color.WHITE);
        builder.setSize(ICON_WIDTH, ICON_HEIGHT);
        builder.setImageUri(Icon.STATE_DEFAULT, imageUri);

        Icon icon = builder.build();
        setIcon(icon);
    }
}
