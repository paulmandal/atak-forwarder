package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.group.ChannelTracker;
import com.paulmandal.atak.forwarder.plugin.ui.GroupMemberDataAdapter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Locale;

public class SettingsTab implements ChannelTracker.UpdateListener,
        CommandQueue.Listener,
        CommHardware.ConnectionStateListener {
    private final Context mPluginContext;
    private final Context mAtakContext;

    private CommandQueue mCommandQueue;
    private ChannelTracker mChannelTracker;
    private CommHardware mCommHardware;

    private TextView mConnectionStatusTextView;
    private TextView mChannelName;

    private TextView mMessageQueueLengthTextView;
    private ListView mGroupMembersListView;
    private Button PairedButton;

    public SettingsTab(Context pluginContext,
                       Context atakContext,
                       ChannelTracker channelTracker,
                       CommHardware commHardware,
                       CommandQueue commandQueue) {
        mPluginContext = pluginContext;
        mAtakContext = atakContext;
        mChannelTracker = channelTracker;
        mCommHardware = commHardware;
        mCommandQueue = commandQueue;
    }

    public void init(View templateView) {
        mConnectionStatusTextView = templateView.findViewById(R.id.textview_connection_status);
        mChannelName = templateView.findViewById(R.id.textview_channel_name);

        mMessageQueueLengthTextView = templateView.findViewById(R.id.textview_message_queue_length);
        mGroupMembersListView = templateView.findViewById(R.id.listview_channel_members);

        Button broadcastDiscovery = templateView.findViewById(R.id.button_broadcast_discovery);
        PairedButton = templateView.findViewById(R.id.button_paired);

        mMessageQueueLengthTextView.setText(String.format(Locale.getDefault(), "%d", mCommandQueue.getQueueSize()));
        PairedButton.setOnClickListener((View v) -> mCommHardware.connect());

        broadcastDiscovery.setOnClickListener((View v) -> {
            Toast.makeText(mAtakContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();
            mCommHardware.broadcastDiscoveryMessage();
        });


        mChannelTracker.addUpdateListener(this);
        mCommandQueue.setListener(this);
        mCommHardware.addConnectionStateListener(this);
    }

    @Override
    // TODO: maybe break this into individual UI components or wait for MVVM
    public void onUpdated() {
        byte[] psk = mChannelTracker.getPsk();
        mChannelName.setText(String.format("#%s - %s", mChannelTracker.getChannelName(), psk != null ? hashFromBytes(psk) : null));
        setupListView();
    }

    private void setupListView() {
        GroupMemberDataAdapter groupMemberDataAdapter = new GroupMemberDataAdapter(mPluginContext, mChannelTracker.getUsers());
        mGroupMembersListView.setAdapter(groupMemberDataAdapter);
    }

    @Override
    public void onMessageQueueSizeChanged(int size) {
        mMessageQueueLengthTextView.setText(String.format(Locale.getDefault(), "%d", size));
    }

    @Override
    public void onConnectionStateChanged(CommHardware.ConnectionState connectionState) {
        switch (connectionState) {
            case UNPAIRED:
                handleUnpaired();
                break;
            case CONNECTED:
                handleDeviceConnected();
                break;
            case DISCONNECTED:
                handleDeviceDisconnected();
                break;
        }
    }

    public void handleUnpaired() {
        Toast.makeText(mAtakContext, "Comm device is not paired -- pair with it in the Android Settings > Connected Devices menu and then click Paired", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_unpaired);
        PairedButton.setVisibility(View.VISIBLE);
    }

    public void handleDeviceConnected() {
        Toast.makeText(mAtakContext, "Comm device connected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_connected);
        PairedButton.setVisibility(View.GONE);
    }

    public void handleDeviceDisconnected() {
        Toast.makeText(mAtakContext, "Comm device disconnected -- or maybe unpaired -- pair with it in the Android Settings > Connected Devices menu and then click Paired", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_disconnected);
        PairedButton.setVisibility(View.VISIBLE);
    }

    private String hashFromBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            bytes = md.digest(bytes);

            Formatter formatter  = new Formatter();
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }

            String hash = formatter.toString();
            return hash.substring(hash.length() - 8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
