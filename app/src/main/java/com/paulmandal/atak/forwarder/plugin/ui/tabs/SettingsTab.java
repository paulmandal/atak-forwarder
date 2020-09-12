package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.group.ChannelTracker;
import com.paulmandal.atak.forwarder.plugin.ui.GroupMemberDataAdapter;

import java.util.Locale;

public class SettingsTab implements ChannelTracker.UpdateListener,
        CommandQueue.Listener,
        CommHardware.ConnectionStateListener {
    private final Context mPluginContext;
    private final Context mAtakContext;

    private CotMessageCache mCotMessageCache;
    private CommandQueue mCommandQueue;
    private ChannelTracker mChannelTracker;
    private CommHardware mCommHardware;

    private TextView mConnectionStatusTextView;
    private TextView mChannelName;

    private TextView mMessageQueueLengthTextView;
    private ListView mGroupMembersListView;
    private Button mCreateGroupButton;
    private Button mScanOrUnpair;

    public SettingsTab(Context pluginContext,
                       Context atakContext,
                       ChannelTracker channelTracker,
                       CommHardware commHardware,
                       CotMessageCache cotMessageCache,
                       CommandQueue commandQueue) {
        mPluginContext = pluginContext;
        mAtakContext = atakContext;
        mChannelTracker = channelTracker;
        mCommHardware = commHardware;
        mCotMessageCache = cotMessageCache;
        mCommandQueue = commandQueue;
    }

    public void init(View templateView) {
        mConnectionStatusTextView = (TextView) templateView.findViewById(R.id.textview_connection_status);
        mChannelName = (TextView) templateView.findViewById(R.id.textview_channel_name);

        mMessageQueueLengthTextView = (TextView)templateView.findViewById(R.id.textview_message_queue_length);
        mCreateGroupButton = (Button) templateView.findViewById(R.id.button_create_channel);
        mGroupMembersListView = (ListView) templateView.findViewById(R.id.listview_channel_members);

        Button broadcastDiscovery = (Button) templateView.findViewById(R.id.button_broadcast_discovery);
        mScanOrUnpair = (Button) templateView.findViewById(R.id.button_scan_or_unpair);

        mMessageQueueLengthTextView.setText(String.format(Locale.getDefault(), "%d", mCommandQueue.getQueueSize()));

        broadcastDiscovery.setOnClickListener((View v) -> {
            Toast.makeText(mAtakContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();
            mCommHardware.broadcastDiscoveryMessage();
        });

        mScanOrUnpair.setOnClickListener(mScanClickListener);

        mChannelTracker.setUpdateListener(this);
        mCommandQueue.setListener(this);
        mCommHardware.addConnectionStateListener(this);
    }

    private View.OnClickListener mScanClickListener = (View v) -> mCommHardware.connect();

    private View.OnClickListener mUnpairClickListener = (View v) -> mCommHardware.disconnect();

    @Override
    public void onUsersUpdated() {
        Toast.makeText(mAtakContext, "User list updated", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    @Override
    public void onChannelUpdated() {
        Toast.makeText(mAtakContext, "Group membership updated", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void updateUi() {
        mChannelName.setText(String.format("#%s", mChannelTracker.getChannelName()));
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
            case SCANNING:
                handleScanStarted();
                break;
            case TIMEOUT:
                handleScanTimeout();
                break;
            case CONNECTED:
                handleDeviceConnected();
                break;
            case DISCONNECTED:
                handleDeviceDisconnected();
                break;
        }
    }

    public void handleScanStarted() {
        Toast.makeText(mAtakContext, "Scanning for comm device", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_scanning);
        mScanOrUnpair.setOnClickListener(null);
    }

    public void handleScanTimeout() {
        Toast.makeText(mAtakContext, "Scanning for comm device timed out, ready device and then rescan in settings menu!", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_timeout);
        mScanOrUnpair.setOnClickListener(mScanClickListener);
        mScanOrUnpair.setText(R.string.scan);
    }

    public void handleDeviceConnected() {
        Toast.makeText(mAtakContext, "Comm device connected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_connected);
        mScanOrUnpair.setOnClickListener(mUnpairClickListener);
        mScanOrUnpair.setText(R.string.unpair);
    }

    public void handleDeviceDisconnected() {
        Toast.makeText(mAtakContext, "Comm device disconnected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_disconnected);
        mScanOrUnpair.setOnClickListener(mScanClickListener);
        mScanOrUnpair.setText(R.string.scan);
    }
}
