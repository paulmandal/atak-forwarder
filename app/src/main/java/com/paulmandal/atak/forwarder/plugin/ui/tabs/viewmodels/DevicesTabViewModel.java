package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.nonatak.NonAtakMeshtasticConfigurator;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DevicesTabViewModel implements NonAtakMeshtasticConfigurator.Listener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTabViewModel.class.getSimpleName();

    private static final String MARKER_MESHTASTIC = "Meshtastic";

    private Context mAtakContext;
    private Handler mUiThreadHandler;

    private MeshtasticDeviceSwitcher mMeshtasticDeviceSwitcher;
    private MeshtasticCommHardware mMeshtasticCommHardware;
    private HashHelper mHashHelper;

    private NonAtakMeshtasticConfigurator mNonAtakMeshtasticConfigurator;

    private MutableLiveData<List<MeshtasticDevice>> mMeshtasticDevices = new MutableLiveData<>();
    private MutableLiveData<String> mCommDeviceAddress = new MutableLiveData<>();

    private MeshtasticDevice mCommDevice;

    private String mChannelName;
    private String mPskHash;
    private byte[] mPsk;
    private MeshProtos.ChannelSettings.ModemConfig mModemConfig;

    private MutableLiveData<Boolean> mNonAtakDeviceWriteInProgress = new MutableLiveData<>();

    public DevicesTabViewModel(Context atakContext,
                               Handler uiThreadHandler,
                               MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                               MeshtasticCommHardware commHardware,
                               HashHelper hashHelper) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mMeshtasticDeviceSwitcher = meshtasticDeviceSwitcher;
        mMeshtasticCommHardware = commHardware;
        mHashHelper = hashHelper;

        MeshtasticDevice commDevice = commHardware.getDevice();
        mCommDeviceAddress.setValue(commDevice != null ? commDevice.address : null);
        mNonAtakDeviceWriteInProgress.setValue(false);
    }

    public LiveData<List<MeshtasticDevice>> getMeshtasticDevices() {
        return mMeshtasticDevices;
    }

    public LiveData<String> getCommDeviceAddress() {
        return mCommDeviceAddress;
    }

    public LiveData<Boolean> getNonAtakDeviceWriteInProgress() {
        return mNonAtakDeviceWriteInProgress;
    }

    @SuppressLint("MissingPermission")
    public void refreshDevices() {
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

        String commDeviceAddress = mCommDeviceAddress.getValue();
        for (MeshtasticDevice device : meshtasticDevices) {
            if (device.address.equals(commDeviceAddress)) {
                mCommDevice = device;
            }
        }

        mMeshtasticDevices.setValue(meshtasticDevices);
    }

    public void setCommDeviceAddress(MeshtasticDevice meshtasticDevice) {
        mCommDeviceAddress.setValue(meshtasticDevice.address);
    }

    public void writeToNonAtak(MeshtasticDevice targetDevice, String deviceCallsign, int teamIndex, int roleIndex, int refreshIntervalS, int screenShutoffDelayS) {
//        if (targetDevice.address.equals(mCommDeviceAddress.getValue())
//                || mChannelName == null
//                || mModemConfig == null) {
//            Log.e(TAG, "Attempt to write to CommDevice address or write without channel settings!");
//            return;
//        }
//
//        mNonAtakDeviceWriteInProgress.setValue(true);
//
//        if (mNonAtakMeshtasticConfigurator != null) {
//            mNonAtakMeshtasticConfigurator.cancel();
//        } else {
//            mMeshtasticCommHardware.suspendResume(true);
//        }
//
//        // Write settings to device
//        mNonAtakMeshtasticConfigurator = new NonAtakMeshtasticConfigurator(mAtakContext, mUiThreadHandler, mMeshtasticDeviceSwitcher, mCommDevice, targetDevice, deviceCallsign, mChannelName, mPsk, mModemConfig, teamIndex, roleIndex, refreshIntervalS, screenShutoffDelayS, this);
//        mNonAtakMeshtasticConfigurator.writeToDevice();
    }

    @Override
    public void onDoneWritingToDevice() {
        mMeshtasticCommHardware.suspendResume(false);
        mNonAtakMeshtasticConfigurator = null;
        mNonAtakDeviceWriteInProgress.setValue(false);
    }
}
