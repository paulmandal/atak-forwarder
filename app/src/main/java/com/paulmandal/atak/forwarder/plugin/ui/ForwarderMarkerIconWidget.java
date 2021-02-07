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
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class ForwarderMarkerIconWidget extends MarkerIconWidget implements Destroyable,
        CommHardware.ConnectionStateListener,
        MapWidget.OnClickListener,
        MeshtasticCommHardware.MessageAckNackListener {
    private final static int ICON_WIDTH = 32;
    private final static int ICON_HEIGHT = 32;

    private static final int PACKET_WINDOW_SIZE = 10;
    private static final int NO_DRAWABLE = -1;

    private ForwarderDropDownReceiver mForwarderDropDownReceiver;

    private CommHardware.ConnectionState mConnectionState;
    private boolean[] mDeliveredPacketsWindow = new boolean[PACKET_WINDOW_SIZE];
    private int mWindowIndex;

    public ForwarderMarkerIconWidget(MapView mapView,
                                     List<Destroyable> destroyables,
                                     ForwarderDropDownReceiver forwarderDropDownReceiver,
                                     MeshtasticCommHardware meshtasticCommHardware) {
        mForwarderDropDownReceiver = forwarderDropDownReceiver;

        destroyables.add(this);

        meshtasticCommHardware.addConnectionStateListener(this);
        meshtasticCommHardware.addMessageAckNackListener(this);

        setName("Forwarder Status");
        addOnClickListener(this);

        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.addWidget(this);

        for (int i = 0 ; i < PACKET_WINDOW_SIZE ; i++) {
            mDeliveredPacketsWindow[i] = true;
        }

        mConnectionState = meshtasticCommHardware.getConnectionState();
        updateIcon();
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
        mConnectionState = connectionState;
        updateIcon();
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.removeWidget(this);
    }

    @Override
    public void onMessageAckNack(int messageId, boolean isAck) {
        addPacketToWindow(isAck);
    }

    @Override
    public void onMessageTimedOut(int messageId) {
        addPacketToWindow(false);
    }

    private void addPacketToWindow(boolean isAck) {
        mDeliveredPacketsWindow[mWindowIndex] = isAck;
        mWindowIndex++;
        if (mWindowIndex >= PACKET_WINDOW_SIZE) {
            mWindowIndex = 0;
        }
        updateIcon();
    }

    private void updateIcon() {
        int drawableId = NO_DRAWABLE;

        switch (mConnectionState) {
            case NO_SERVICE_CONNECTION:
            case DEVICE_DISCONNECTED:
                drawableId = R.drawable.ic_status_red;
                break;
            case NO_DEVICE_CONFIGURED:
                drawableId = R.drawable.ic_status_purple;
                break;
        }

        if (drawableId != NO_DRAWABLE) {
            setIcon(drawableId);
            return;
        }

        int totalDeliveredPackets = 0;
        for (boolean isAck : mDeliveredPacketsWindow) {
            if (isAck) {
                totalDeliveredPackets++;
            }
        }

        float percentageOfPacketsDelivered = totalDeliveredPackets / (float)PACKET_WINDOW_SIZE;

        if (percentageOfPacketsDelivered > 0.95F) {
            drawableId = R.drawable.ic_status_green;
        } else if (percentageOfPacketsDelivered > 0.75F) {
            drawableId = R.drawable.ic_status_yellow;
        } else if (percentageOfPacketsDelivered > 0.50F) {
            drawableId = R.drawable.ic_status_orange;
        } else if (percentageOfPacketsDelivered > 0.25F){
            drawableId = R.drawable.ic_status_brown;
        } else {
            drawableId = R.drawable.ic_status_grey;
        }

        setIcon(drawableId);
    }

    private void setIcon(int drawableId) {
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
