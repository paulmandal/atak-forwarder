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
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;

import java.util.Collections;

public class StatusScreen {
    private Context mAtakContext;

    private final TextView mChannelName;
    private final TextView mPskHash;
    private final TextView mModemConfig;
    private final TextView mConnectionStatusTextView;

    private final TextView mMessageQueueLengthTextView;
    private final TextView mReceivedTextView;
    private final TextView mDelieveredTextView;
    private final TextView mTimedOutTextView;
    private final TextView mErroredTextView;
    private final TextView mTotalTextView;
    private final ListView mGroupMembersListView;
    private final Button mBroadcastDiscoveryButton;
    
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

        mBroadcastDiscoveryButton = vg.findViewById(R.id.button_broadcast_discovery);
    }

    @SuppressLint("DefaultLocale")
    public void bind(LifecycleOwner lifecycleOwner,
                     StatusViewModel statusViewModel,
                     Context pluginContext,
                     Context atakContext) {
        mAtakContext = atakContext;

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
            Collections.sort(userInfoList, (UserInfo lhs, UserInfo rhs) -> {
                boolean lhsTak = !(lhs instanceof TrackerUserInfo);
                boolean rhsTak = !(rhs instanceof TrackerUserInfo);

                if (lhsTak == rhsTak) {
                    return 0;
                }

                if (lhsTak) {
                    return -1;
                }

                return 1;
            });
            GroupMemberDataAdapter groupMemberDataAdapter = new GroupMemberDataAdapter(atakContext, pluginContext, userInfoList);
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
        statusViewModel.getPskHash().observe(lifecycleOwner, mPskHash::setText);
        statusViewModel.getModemConfig().observe(lifecycleOwner, modemConfig -> mModemConfig.setText(modemConfig != null ? String.format("%d", modemConfig) : null));
    }

    private void handleNoServiceConnected() {
        Toast.makeText(mAtakContext, "Meshtastic Service not connected, check that you have the Meshtastic app installed", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_no_service_connected);
    }

    private void handleNoDeviceConfigured() {
        Toast.makeText(mAtakContext, "No Comm Device or Region configured, go Settings -> Tool Preferences -> ATAK Forwarder to set them", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_no_device_configured);
    }

    private void handleDeviceDisconnected() {
        Toast.makeText(mAtakContext, "Comm Device disconnected, check that it is turned on and paired", Toast.LENGTH_LONG).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_disconnected);
    }

    private void handleDeviceConnected() {
        Toast.makeText(mAtakContext, "Comm device connected", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_connected);
    }
}
