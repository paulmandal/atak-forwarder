package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.GroupMemberDataAdapter;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusTabViewModel;

public class StatusTab extends RelativeLayout {
    private Context mAtakContext;

    private TextView mConnectionStatusTextView;

    private TextView mMessageQueueLengthTextView;
    private ListView mGroupMembersListView;
    private Button PairedButton;

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

        mMessageQueueLengthTextView = findViewById(R.id.textview_message_queue_length);
        mGroupMembersListView = findViewById(R.id.listview_channel_members);

        PairedButton = findViewById(R.id.button_paired);
    }

    @SuppressLint("DefaultLocale")
    public void bind(LifecycleOwner lifecycleOwner,
                     StatusTabViewModel statusTabViewModel,
                     Context pluginContext,
                     Context atakContext) {
        mAtakContext = atakContext;

        PairedButton.setOnClickListener((View v) -> statusTabViewModel.connect());

        Button broadcastDiscovery = findViewById(R.id.button_broadcast_discovery);
        broadcastDiscovery.setOnClickListener((View v) -> {
            Toast.makeText(atakContext, "Broadcasting discovery message", Toast.LENGTH_SHORT).show();

            statusTabViewModel.broadcastDiscoveryMessage();
        });

        statusTabViewModel.getMessageQueueSize().observe(lifecycleOwner, messageQueueSize -> mMessageQueueLengthTextView.setText(String.format("%d", messageQueueSize)));
        statusTabViewModel.getConnectionState().observe(lifecycleOwner, connectionState -> {
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
        });

        statusTabViewModel.getUserInfoList().observe(lifecycleOwner, userInfoList -> {
            GroupMemberDataAdapter groupMemberDataAdapter = new GroupMemberDataAdapter(pluginContext, userInfoList);
            mGroupMembersListView.setAdapter(groupMemberDataAdapter);
        });
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
}
