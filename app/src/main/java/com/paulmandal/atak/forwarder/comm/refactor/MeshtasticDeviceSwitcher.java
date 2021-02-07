package com.paulmandal.atak.forwarder.comm.refactor;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.RemoteException;
import android.util.Log;

import com.geeksville.mesh.IMeshService;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MeshtasticDeviceSwitcher {
    private static final String TAG = Constants.DEBUG_TAG_PREFIX + MeshtasticDeviceSwitcher.class.getSimpleName();

    private static final String ACTION_USB_PERMISSION = "com.paulmandal.atak.forwarder.USB_PERMISSION";

    private final Context mAtakContext;

    private final List<String> mDevicesWithUsbPermisssion;

    public MeshtasticDeviceSwitcher(Context atakContext) {
        mAtakContext = atakContext;
        mDevicesWithUsbPermisssion = new ArrayList<>();
    }

    public void setDeviceAddress(IMeshService meshService, MeshtasticDevice meshtasticDevice) throws RemoteException {
        boolean isUsb = meshtasticDevice.deviceType == MeshtasticDevice.DeviceType.USB;
        if (isUsb && (!mDevicesWithUsbPermisssion.contains(meshtasticDevice.address))) {
            BroadcastReceiver usbReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (ACTION_USB_PERMISSION.equals(action)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                            mDevicesWithUsbPermisssion.add(device.getDeviceName());
                            try {
                                setDeviceAddress(meshService, meshtasticDevice);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Permission denied for USB device: " + device);
                        }

                        mAtakContext.unregisterReceiver(this);
                    }
                }
            };

            UsbManager usbManager = (UsbManager) mAtakContext.getSystemService(Context.USB_SERVICE);
            PendingIntent permissionIntent = PendingIntent.getBroadcast(mAtakContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            UsbDevice device = deviceList.get(meshtasticDevice.address);

            if (device == null) {
                Log.e(TAG, "USB device was not connected: " + meshtasticDevice.address);
                return;
            }

            mAtakContext.registerReceiver(usbReceiver, filter);
            usbManager.requestPermission(device, permissionIntent);
            return;
        }

        String deviceAddressBase = isUsb ? "s%s" : "x%s";
        String deviceAddress = String.format(deviceAddressBase, meshtasticDevice.address);
        meshService.setDeviceAddress(deviceAddress);
    }
}
