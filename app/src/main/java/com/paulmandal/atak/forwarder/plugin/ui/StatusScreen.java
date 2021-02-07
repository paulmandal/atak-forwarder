package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusViewModel;

public class StatusScreen {
    private Context mAtakContext;

    private TextView mChannelName;
    private TextView mPskHash;
    private TextView mModemConfig;
    private TextView mConnectionStatusTextView;

    private TextView mMessageQueueLengthTextView;
    private TextView mReceivedTextView;
    private TextView mDelieveredTextView;
    private TextView mTimedOutTextView;
    private TextView mErroredTextView;
    private TextView mTotalTextView;
    private ListView mGroupMembersListView;
    private Button mConnectToServiceButton;
    private Button mBroadcastDiscoveryButton;
    
    public StatusScreen(ViewGroup vg) {
        mConnectionStatusTextView = vg.findViewById(R.id.textview_connection_status);

        mChannelName = vg.findViewById(R.id.channel_name);
        mPskHash = vg.findViewById(R.id.psk_hash);
        mModemConfig = vg.findViewById(R.id.modem_config);

        mMessageQueueLengthTextView = vg.findViewById(R.id.textview_message_queue_length);
        mReceivedTextView = vg.findViewById(R.id.textview_received_messages);
        mDelieveredTextView = vg.findViewById(R.id.textview_delivered_messages);
        mTimedOutTextView = vg.findViewById(R.id.textview_timed_out_messages);
        mErroredTextView = vg.findViewById(R.id.textview_errored_messages);
        mTotalTextView = vg.findViewById(R.id.textview_total_messages);
        mGroupMembersListView = vg.findViewById(R.id.listview_channel_members);

        mConnectToServiceButton = vg.findViewById(R.id.button_connect_to_service);
        mBroadcastDiscoveryButton = vg.findViewById(R.id.button_broadcast_discovery);
    }

    @SuppressLint("DefaultLocale")
    public void bind(LifecycleOwner lifecycleOwner,
                     StatusViewModel statusViewModel,
                     Context pluginContext,
                     Context atakContext) {
        mAtakContext = atakContext;

        mConnectToServiceButton.setOnClickListener((View v) -> statusViewModel.connect());

        mBroadcastDiscoveryButton.setOnClickListener((View v) -> {
            Toast.makeText(atakContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();

            statusViewModel.broadcastDiscoveryMessage();
        });

        statusViewModel.getMessageQueueSize().observe(lifecycleOwner, messageQueueSize -> mMessageQueueLengthTextView.setText(String.format("%d", messageQueueSize)));
        statusViewModel.getConnectionState().observe(lifecycleOwner, connectionState -> {
            switch (connectionState) {
                case NO_SERVICE_CONNECTION:
                    handleNoServiceConnected();
                    break;
                case NO_DEVICE_CONFIGURED:
                    handleNoDeviceConfigured();
                    break;
                case DEVICE_DISCONNECTED:
                    handleDeviceDisconnected();
                    break;
                case DEVICE_CONNECTED:
                    handleDeviceConnected();
                    break;
            }
        });

        statusViewModel.getUserInfoList().observe(lifecycleOwner, userInfoList -> {
            GroupMemberDataAdapter groupMemberDataAdapter = new GroupMemberDataAdapter(pluginContext, userInfoList);
            mGroupMembersListView.setAdapter(groupMemberDataAdapter);
        });

        statusViewModel.getReceivedMessages().observe(lifecycleOwner, receivedMessages -> mReceivedTextView.setText(String.format("%d", receivedMessages)));
        statusViewModel.getDeliveredMessages().observe(lifecycleOwner, deliveredMessages -> mDelieveredTextView.setText(String.format("%d", deliveredMessages)));
        statusViewModel.getTimedOutMessages().observe(lifecycleOwner, timedOutMessages -> mTimedOutTextView.setText(String.format("%d", timedOutMessages)));
        statusViewModel.getErroredMessages().observe(lifecycleOwner, erroredMessages -> mErroredTextView.setText(String.format("%d", erroredMessages)));
        statusViewModel.getTotalMessage().observe(lifecycleOwner, totalMessages -> mTotalTextView.setText(String.format("%d", totalMessages)));
        statusViewModel.getErrorsInARow().observe(lifecycleOwner, errorsInARow -> {
            if (errorsInARow > 1 && errorsInARow % 5 == 0) {
                Toast.makeText(atakContext, String.format("%d errors in a row -- maybe out of range, verify your channel settings if you have not been getting messages", errorsInARow), Toast.LENGTH_LONG).show();
            }
        });
        statusViewModel.getChannelName().observe(lifecycleOwner, channelName -> mChannelName.setText(channelName != null ? String.format("#%s", channelName) : null));
        statusViewModel.getPskHash().observe(lifecycleOwner, pskHash -> mPskHash.setText(pskHash));
        statusViewModel.getModemConfig().observe(lifecycleOwner, modemConfig -> mModemConfig.setText(modemConfig != null ? String.format("%d", modemConfig.getNumber()) : null));
    }

    private void handleNoServiceConnected() {
        Toast.makeText(mAtakContext, "Meshtastic Service not connected, if the icon remains red click the 'Connect Svc' button in the status tab", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_no_service_connected);
        mConnectToServiceButton.setVisibility(View.VISIBLE);
    }

    private void handleNoDeviceConfigured() {
        Toast.makeText(mAtakContext, "No Comm device configured, select one in the Devices tab and tap Set Comm Device", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_no_device_configured);
        mConnectToServiceButton.setVisibility(View.INVISIBLE);
    }

    private void handleDeviceDisconnected() {
        Toast.makeText(mAtakContext, "Comm Device disconnected, check that it is turned on and paired", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_disconnected);
        mConnectToServiceButton.setVisibility(View.INVISIBLE);
    }

    private void handleDeviceConnected() {
        Toast.makeText(mAtakContext, "Comm device connected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_connected);
        mConnectToServiceButton.setVisibility(View.INVISIBLE);
    }
}