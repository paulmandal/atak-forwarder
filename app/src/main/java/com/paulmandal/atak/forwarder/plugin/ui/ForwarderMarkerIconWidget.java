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
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshSender;
import com.paulmandal.atak.forwarder.comm.meshtastic.ConnectionStateHandler;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

public class ForwarderMarkerIconWidget extends MarkerIconWidget implements Destroyable,
        ConnectionStateHandler.Listener,
        MapWidget.OnClickListener,
        MeshSender.MessageAckNackListener {
    private final static int ICON_WIDTH = 32;
    private final static int ICON_HEIGHT = 32;

    private static final int PACKET_WINDOW_SIZE = 10;
    private static final int NO_DRAWABLE = -1;

    private final ForwarderDropDownReceiver mForwarderDropDownReceiver;

    private final boolean[] mDeliveredPacketsWindow = new boolean[PACKET_WINDOW_SIZE];
    private int mWindowIndex;

    private ConnectionStateHandler.ConnectionState mConnectionState;

    public ForwarderMarkerIconWidget(MapView mapView,
                                     List<Destroyable> destroyables,
                                     ForwarderDropDownReceiver forwarderDropDownReceiver,
                                     ConnectionStateHandler connectionStateHandler,
                                     MeshSender meshSender) {
        mForwarderDropDownReceiver = forwarderDropDownReceiver;

        destroyables.add(this);

        connectionStateHandler.addListener(this);
        meshSender.addMessageAckNackListener(this);

        setName("Forwarder Status");
        addOnClickListener(this);

        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        LinearLayoutWidget brLayout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
        brLayout.addWidget(this);

        for (int i = 0; i < PACKET_WINDOW_SIZE; i++) {
            mDeliveredPacketsWindow[i] = true;
        }

        mConnectionState = connectionStateHandler.getConnectionState();
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
    public void onConnectionStateChanged(ConnectionStateHandler.ConnectionState connectionState) {
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
                drawableId = R.drawable.ic_no_service_connection;
                break;
            case DEVICE_DISCONNECTED:
                drawableId = R.drawable.ic_disconnected;
                break;
            case NO_DEVICE_CONFIGURED:
                drawableId = R.drawable.ic_no_device_configured;
                break;
            case WRITING_CONFIG:
                drawableId = R.drawable.ic_writing_config;
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

        float percentageOfPacketsDelivered = totalDeliveredPackets / (float) PACKET_WINDOW_SIZE;

        if (percentageOfPacketsDelivered > 0.89F) {
            drawableId = R.drawable.ic_mesh_connection_90plus;
        } else if (percentageOfPacketsDelivered > 0.75F) {
            drawableId = R.drawable.ic_mesh_connection_75plus;
        } else if (percentageOfPacketsDelivered > 0.50F) {
            drawableId = R.drawable.ic_mesh_connection_50plus;
        } else if (percentageOfPacketsDelivered > 0.25F) {
            drawableId = R.drawable.ic_mesh_connection_25plus;
        } else {
            drawableId = R.drawable.ic_mesh_connection_below_25;
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
