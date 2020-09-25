package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.nonatak.NonAtakMeshtasticConfigurator;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DevicesTabViewModel implements MeshtasticCommHardware.ChannelSettingsListener,
                                            NonAtakMeshtasticConfigurator.Listener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTabViewModel.class.getSimpleName();

    private static final String MARKER_MESHTASTIC = "Meshtastic";

    private Activity mActivity;
    private Handler mUiThreadHandler;
    private Context mAtakContext;
    private MeshtasticCommHardware mMeshtasticCommHardware;
    private HashHelper mHashHelper;

    private NonAtakMeshtasticConfigurator mNonAtakMeshtasticConfigurator;

    private MutableLiveData<List<BluetoothDevice>> mBluetoothDevices = new MutableLiveData<>();
    private MutableLiveData<String> mCommDeviceAddress = new MutableLiveData<>();

    private MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private MutableLiveData<Byte[]> mPsk = new MutableLiveData<>();
    private MutableLiveData<MeshProtos.ChannelSettings.ModemConfig> mModemConfig = new MutableLiveData<>();

    private MutableLiveData<Boolean> mNonAtakDeviceWriteInProgress = new MutableLiveData<>();

    public DevicesTabViewModel(Activity activity,
                               Handler uiThreadHandler,
                               Context atakContext,
                               MeshtasticCommHardware commHardware,
                               HashHelper hashHelper) {
        mActivity = activity;
        mUiThreadHandler = uiThreadHandler;
        mAtakContext = atakContext;
        mMeshtasticCommHardware = commHardware;
        mHashHelper = hashHelper;

        commHardware.addChannelSettingsListener(this);
        mCommDeviceAddress.setValue(commHardware.getDeviceAddress());
        mNonAtakDeviceWriteInProgress.setValue(false);
    }

    @Nullable
    public LiveData<String> getChannelName() {
        return mChannelName;
    }

    @Nullable
    public LiveData<MeshProtos.ChannelSettings.ModemConfig> getModemConfig() {
        return mModemConfig;
    }

    @Nullable
    public LiveData<String> getPskHash() {
        return mPskHash;
    }

    @Nullable
    public LiveData<Byte[]> getPsk() { return mPsk; }

    @Nullable
    public LiveData<List<BluetoothDevice>> getBluetoothDevices() {
        return mBluetoothDevices;
    }

    @Nullable
    public LiveData<String> getCommDeviceAddress() {
        return mCommDeviceAddress;
    }

    public void refreshDevices() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> filteredDevices = new ArrayList<>(devices.size());
        for (BluetoothDevice device : devices) {
            Log.e(TAG, "bonded device: " + device.getName());
            if (device.getName().startsWith(MARKER_MESHTASTIC)) {
                filteredDevices.add(device);
            }
        }

//        BluetoothManager bm = (BluetoothManager) mAtakContext.getSystemService(Context.BLUETOOTH_SERVICE);
//        List<BluetoothDevice> devices = bm.getConnectedDevices(BluetoothGatt.GATT);
//
//        List<BluetoothDevice> filteredDevices = new ArrayList<>(devices.size());
//
//        for (BluetoothDevice device : devices) {
//            Log.e(TAG, "device: " + device.getName() + ", " + device.getAddress());
//            if (device.getName().startsWith(MARKER_MESHTASTIC)) {
//                filteredDevices.add(device);
//            }
//        }

        mBluetoothDevices.setValue(filteredDevices);
    }

    public void setCommDeviceAddress(String deviceAddress) {
        if (mMeshtasticCommHardware.setDeviceAddress(deviceAddress)) {
            mCommDeviceAddress.setValue(deviceAddress);
        }
    }

    public void writeToNonAtak(String deviceAddress, String deviceCallsign, int teamIndex, int roleIndex, int refreshIntervalS) {
        if (deviceAddress.equals(mCommDeviceAddress)) {
            Log.e(TAG, "Attempt to write to CommDevice address!");
            return;
        }

        // TODO: remove
        Log.e(TAG, "Writing to device: " + deviceAddress + " callsign: " + deviceCallsign + " teamIndex: " + teamIndex + " roleIn: " + roleIndex + " refresh(s): " + refreshIntervalS);

        if (mNonAtakMeshtasticConfigurator != null) {
            mNonAtakMeshtasticConfigurator.cancel();
        } else {
            mMeshtasticCommHardware.suspendResume(true);
        }

        // Write settings to device
        Byte[] pskBytes = mPsk.getValue();
        byte[] pskBytesPrimitive = new byte[pskBytes.length];
        for (int i = 0 ; i < pskBytes.length ; i++) {
            pskBytesPrimitive[i] = pskBytes[i];
        }
        mNonAtakMeshtasticConfigurator = new NonAtakMeshtasticConfigurator(mActivity, mUiThreadHandler, mCommDeviceAddress.getValue(), deviceAddress, deviceCallsign, mChannelName.getValue(), pskBytesPrimitive, mModemConfig.getValue(), teamIndex, roleIndex, refreshIntervalS, this);
        mNonAtakMeshtasticConfigurator.writeToDevice();
    }

    @Override
    @CallSuper
    public void onChannelSettingsUpdated(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        mChannelName.setValue(channelName);
        mModemConfig.setValue(modemConfig);
        mPskHash.setValue(mHashHelper.hashFromBytes(psk));

        Byte[] pskBytes = new Byte[psk.length];
        for (int i = 0 ; i < psk.length ; i++) {
            pskBytes[i] = psk[i];
        }
        mPsk.setValue(pskBytes);
    }

    @Override
    public void onDoneWritingToDevice() {
        mMeshtasticCommHardware.suspendResume(false);
        mNonAtakMeshtasticConfigurator = null;
    }
}
