package com.paulmandal.atak.forwarder.plugin.ui.tabs;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;

import java.util.List;

public class DevicesTab extends RelativeLayout {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTab.class.getSimpleName();

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
                     DevicesTabViewModel devicesTabViewModel,
                     Context pluginContext) {
        Button refreshButton = findViewById(R.id.button_refresh_connected_devices);
        refreshButton.setOnClickListener(v -> devicesTabViewModel.refreshDevices());

        Button setCommDeviceButton = findViewById(R.id.button_set_comm_device);
        setCommDeviceButton.setOnClickListener(v -> {
            // TODO: alert dialog, save to devicesTabViewModel
        });

        Button writeToNonAtakButton = findViewById(R.id.button_write_to_non_atak);
        writeToNonAtakButton.setOnClickListener(v -> {
            // TODO: dialog? use ViewModel to write this bitch out
        });

        ListView devicesListView = findViewById(R.id.listview_bonded_devices);
        TextView commDevice = findViewById(R.id.textview_comm_device);
        TextView targetDevice = findViewById(R.id.textview_target_device);

        devicesTabViewModel.getBluetoothDevices().observe(lifecycleOwner, bluetoothDevices -> {
            Log.e(TAG, "bluetooth devices updated: " + bluetoothDevices.size());
            String commDeviceAddress = devicesTabViewModel.getCommDeviceAddress().getValue();

            if (commDeviceAddress != null) {
                for (BluetoothDevice device : bluetoothDevices) {
                    String deviceAddress = device.getAddress();
                    if (deviceAddress.equals(commDeviceAddress)) {
                        commDevice.setText(String.format("%s - %s", deviceAddress, device.getName()));
                    }
                }
            }

            DevicesDataAdapter devicesDataAdapter = new DevicesDataAdapter(pluginContext, bluetoothDevices, commDeviceAddress);
            devicesListView.setAdapter(devicesDataAdapter);
        });

        devicesTabViewModel.getCommDeviceAddress().observe(lifecycleOwner, commDeviceAddress -> {
            Log.e(TAG, "comm device addr updated: " + commDeviceAddress);
            List<BluetoothDevice> bluetoothDevices = devicesTabViewModel.getBluetoothDevices().getValue();

            if (bluetoothDevices == null) {
                return;
            }

            if (commDeviceAddress != null) {
                for (BluetoothDevice device : bluetoothDevices) {
                    String deviceAddress = device.getAddress();
                    if (deviceAddress.equals(commDeviceAddress)) {
                        commDevice.setText(String.format("%s - %s", deviceAddress, device.getName()));
                    }
                }
            }

            DevicesDataAdapter devicesDataAdapter = new DevicesDataAdapter(pluginContext, bluetoothDevices, commDeviceAddress);
            devicesListView.setAdapter(devicesDataAdapter);
        });
    }
}
