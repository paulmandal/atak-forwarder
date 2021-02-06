package com.paulmandal.atak.forwarder.plugin.ui.settings;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DevicesList {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesList.class.getSimpleName();

    private static final String MARKER_MESHTASTIC = "Meshtastic";

    private Context mAtakContext;

    public DevicesList(Context atakContext) {
        mAtakContext = atakContext;
    }

    @SuppressLint("MissingPermission")
    public List<MeshtasticDevice> getMeshtasticDevices() {
        List<MeshtasticDevice> meshtasticDevices = new ArrayList<>();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bluetoothDevices) {
            if (device.getName().startsWith(MARKER_MESHTASTIC)) {
                meshtasticDevices.add(new MeshtasticDevice(device.getName(), device.getAddress(), MeshtasticDevice.DeviceType.BLUETOOTH));
            }
        }

        UsbManager usbManager = (UsbManager) mAtakContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevicesMap = usbManager.getDeviceList();
        Collection<UsbDevice> usbDevices = usbDevicesMap.values();
        for (UsbDevice device : usbDevices) {
            meshtasticDevices.add(new MeshtasticDevice(device.getProductName(), device.getDeviceName(), MeshtasticDevice.DeviceType.USB));
        }

        Log.d(TAG, "Refreshing devices, size: " + meshtasticDevices.size());
        return meshtasticDevices;
    }
}