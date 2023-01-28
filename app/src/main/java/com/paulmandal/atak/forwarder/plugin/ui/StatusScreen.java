package com.paulmandal.atak.forwarder.plugin.ui;

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
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;

import java.util.Collections;

public class StatusScreen extends ConstraintLayout {
    private Context mAtakContext;

    private final TextView mChannelName;
    private final TextView mPskHash;
    private final TextView mModemConfig;
    private final TextView mConnectionStatusTextView;
    private final TextView mDeviceIdTextView;

    private final TextView mMessageQueueLengthTextView;
    private final TextView mReceivedTextView;
    private final TextView mDelieveredTextView;
    private final TextView mTimedOutTextView;
    private final TextView mErroredTextView;
    private final TextView mTotalTextView;
    private final ListView mGroupMembersListView;
    private final Button mBroadcastDiscoveryButton;
    
    public StatusScreen(Context context) {
        this(context, null);
    }
    
    public StatusScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public StatusScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.status_layout, this);
        
        mConnectionStatusTextView = findViewById(R.id.textview_connection_status);
        mDeviceIdTextView = findViewById(R.id.textview_device_id);

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

        mBroadcastDiscoveryButton = findViewById(R.id.button_broadcast_discovery);
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
                case WRITING_CONFIG:
                    handleDeviceWriting();
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
        statusViewModel.getChannelName().observe(lifecycleOwner, channelName -> mChannelName.setText(channelName != null ? String.format("#%s", channelName) : null));
        statusViewModel.getPskHash().observe(lifecycleOwner, mPskHash::setText);
        statusViewModel.getModemPreset().observe(lifecycleOwner, modemConfig -> mModemConfig.setText(modemConfig != null ? String.format("%d", modemConfig.getNumber()) : null));
        statusViewModel.getCommDevice().observe(lifecycleOwner, commDevice -> mDeviceIdTextView.setText(String.format("(%s)", getShortDeviceId(commDevice))));
        statusViewModel.getPluginManagesDevice().observe(lifecycleOwner, pluginManagesDevice -> {
            int visibility = pluginManagesDevice ? View.VISIBLE : View.INVISIBLE;
            mChannelName.setVisibility(visibility);
            mPskHash.setVisibility(visibility);
            mModemConfig.setVisibility(visibility);
        });
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
        Toast.makeText(mAtakContext, "Comm Device disconnected, check that it is turned on and paired", Toast.LENGTH_SHORT).show();
        mConnectionStatusTextView.setText(R.string.connection_status_device_disconnected);
    }

    private void handleDeviceWriting() {
        mConnectionStatusTextView.setText(R.string.connection_status_device_writing);
    }

    private void handleDeviceConnected() {
        mConnectionStatusTextView.setText(R.string.connection_status_device_connected);
    }

    private String getShortDeviceId(MeshtasticDevice meshtasticDevice) {
        if (meshtasticDevice == null) {
            return "(no dev cfg)";
        }

        if (meshtasticDevice.deviceType == MeshtasticDevice.DeviceType.BLUETOOTH) {
            String addressWithoutDelimiters = meshtasticDevice.address.replace(":", "").toLowerCase();
            return addressWithoutDelimiters.substring(Math.max(0, addressWithoutDelimiters.length() - 4));
        }

        return String.format("...%s", meshtasticDevice.address.substring(Math.max(0, meshtasticDevice.address.length() - 8)));
    }
}
