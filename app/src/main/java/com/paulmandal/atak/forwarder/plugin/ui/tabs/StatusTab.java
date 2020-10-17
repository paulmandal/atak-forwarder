package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.GroupMemberDataAdapter;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusTabViewModel;

public class StatusTab extends ConstraintLayout {
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

    public StatusTab(Context context) {
        this(context, null);
    }

    public StatusTab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusTab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.status_layout, this);

        mConnectionStatusTextView = findViewById(R.id.textview_connection_status);

        mChannelName = findViewById(R.id.channel_name);
        mPskHash = findViewById(R.id.psk_hash);
        mModemConfig = findViewById(R.id.modem_config);

        mMessageQueueLengthTextView = findViewById(R.id.textview_message_queue_length);
        mReceivedTextView = findViewById(R.id.textview_received_messages);
        mDelieveredTextView = findViewById(R.id.textview_delivered_messages);
        mTimedOutTextView = findViewById(R.id.textview_timed_out_messages);
        mErroredTextView = findViewById(R.id.textview_errored_messages);
        mTotalTextView = findViewById(R.id.textview_total_messages);
        mGroupMembersListView = findViewById(R.id.listview_channel_members);

        mConnectToServiceButton = findViewById(R.id.button_connect_to_service);
    }

    @SuppressLint("DefaultLocale")
    public void bind(LifecycleOwner lifecycleOwner,
                     StatusTabViewModel statusTabViewModel,
                     Context pluginContext,
                     Context atakContext) {
        mAtakContext = atakContext;

        mConnectToServiceButton.setOnClickListener((View v) -> statusTabViewModel.connect());

        Button broadcastDiscovery = findViewById(R.id.button_broadcast_discovery);
        broadcastDiscovery.setOnClickListener((View v) -> {
            Toast.makeText(atakContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();

            statusTabViewModel.broadcastDiscoveryMessage();
        });

        statusTabViewModel.getMessageQueueSize().observe(lifecycleOwner, messageQueueSize -> mMessageQueueLengthTextView.setText(String.format("%d", messageQueueSize)));
        statusTabViewModel.getConnectionState().observe(lifecycleOwner, connectionState -> {
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

        statusTabViewModel.getUserInfoList().observe(lifecycleOwner, userInfoList -> {
            GroupMemberDataAdapter groupMemberDataAdapter = new GroupMemberDataAdapter(pluginContext, userInfoList);
            mGroupMembersListView.setAdapter(groupMemberDataAdapter);
        });

        statusTabViewModel.getReceivedMessages().observe(lifecycleOwner, receivedMessages -> mReceivedTextView.setText(String.format("%d", receivedMessages)));
        statusTabViewModel.getDeliveredMessages().observe(lifecycleOwner, deliveredMessages -> mDelieveredTextView.setText(String.format("%d", deliveredMessages)));
        statusTabViewModel.getTimedOutMessages().observe(lifecycleOwner, timedOutMessages -> mTimedOutTextView.setText(String.format("%d", timedOutMessages)));
        statusTabViewModel.getErroredMessages().observe(lifecycleOwner, erroredMessages -> mErroredTextView.setText(String.format("%d", erroredMessages)));
        statusTabViewModel.getTotalMessage().observe(lifecycleOwner, totalMessages -> mTotalTextView.setText(String.format("%d", totalMessages)));
        statusTabViewModel.getErrorsInARow().observe(lifecycleOwner, errorsInARow -> {
            if (errorsInARow > 1 && errorsInARow % 5 == 0) {
                Toast.makeText(atakContext, String.format("%d errors in a row -- maybe out of range, verify your channel settings if you have not been getting messages", errorsInARow), Toast.LENGTH_LONG).show();
            }
        });
        statusTabViewModel.getChannelName().observe(lifecycleOwner, channelName -> mChannelName.setText(channelName != null ? String.format("#%s", channelName) : null));
        statusTabViewModel.getPskHash().observe(lifecycleOwner, pskHash -> mPskHash.setText(pskHash));
        statusTabViewModel.getModemConfig().observe(lifecycleOwner, modemConfig -> mModemConfig.setText(modemConfig != null ? String.format("%d", modemConfig.getNumber()) : null));
    }

    private void handleNoServiceConnected() {
        Toast.makeText(mAtakContext, "Meshtastic Service not connected, if the icon remains red click the 'Connect Svc' button in the status tab", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_no_service_connected);
        mConnectToServiceButton.setVisibility(VISIBLE);
    }

    private void handleNoDeviceConfigured() {
        Toast.makeText(mAtakContext, "No Comm device configured, select one in the Devices tab and tap Set Comm Device", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_no_device_configured);
        mConnectToServiceButton.setVisibility(INVISIBLE);
    }

    private void handleDeviceDisconnected() {
        Toast.makeText(mAtakContext, "Comm Device disconnected, check that it is turned on and paired", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_disconnected);
        mConnectToServiceButton.setVisibility(INVISIBLE);
    }

    private void handleDeviceConnected() {
        Toast.makeText(mAtakContext, "Comm device connected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_connected);
        mConnectToServiceButton.setVisibility(INVISIBLE);
    }
}
