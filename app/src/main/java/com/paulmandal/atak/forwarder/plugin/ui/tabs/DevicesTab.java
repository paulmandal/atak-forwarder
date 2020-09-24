package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;

public class DevicesTab extends RelativeLayout {
    public DevicesTab(Context context) {
        this(context, null);
    }

    public DevicesTab(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DevicesTab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.devices_layout, this);
    }

    public void bind(LifecycleOwner lifecycleOwner,
                     DevicesTabViewModel devicesTabViewModel) {
        Button scanButton = findViewById(R.id.button_scan_for_devices);
        scanButton.setOnClickListener(v -> devicesTabViewModel.scanForDevices());
    }
}
