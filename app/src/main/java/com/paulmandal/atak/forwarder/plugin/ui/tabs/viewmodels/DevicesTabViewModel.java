package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.util.ArrayList;
import java.util.List;

public class DevicesTabViewModel implements MeshtasticCommHardware.ChannelSettingsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTabViewModel.class.getSimpleName();

    private static final String MARKER_MESHTASTIC = "Meshtastic";

    private Context mAtakContext;
    private MeshtasticCommHardware mMeshtasticCommHardware;
    private HashHelper mHashHelper;

    private MutableLiveData<List<BluetoothDevice>> mBluetoothDevices = new MutableLiveData<>();
    private MutableLiveData<String> mCommDeviceAddress = new MutableLiveData<>();

    private MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private MutableLiveData<Byte[]> mPsk = new MutableLiveData<>();
    private MutableLiveData<MeshProtos.ChannelSettings.ModemConfig> mModemConfig = new MutableLiveData<>();

    public DevicesTabViewModel(Context atakContext,
                               MeshtasticCommHardware commHardware,
                               HashHelper hashHelper) {
        mAtakContext = atakContext;
        mMeshtasticCommHardware = commHardware;
        mHashHelper = hashHelper;

        commHardware.addChannelSettingsListener(this);
        mCommDeviceAddress.setValue(commHardware.getDeviceAddress());
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
        BluetoothManager bm = (BluetoothManager) mAtakContext.getSystemService(Context.BLUETOOTH_SERVICE);
        List<BluetoothDevice> devices = bm.getConnectedDevices(BluetoothGatt.GATT);

        List<BluetoothDevice> filteredDevices = new ArrayList<>(devices.size());

        for (BluetoothDevice device : devices) {
            if (device.getName().startsWith(MARKER_MESHTASTIC)) {
                filteredDevices.add(device);
            }
        }

        mBluetoothDevices.setValue(devices);
    }

    public void setCommDeviceAddress(String deviceAddress) {
        if (mMeshtasticCommHardware.setDeviceAddress(deviceAddress)) {
            mCommDeviceAddress.setValue(deviceAddress);
        }
    }

    public void writeToNonAtak(String deviceAddress) {
        // Write settings to device
        // TODO: tell MeshtasticCommHardware shit has started
        // TODO: set async finished listener to tell MeshtasticCommhardware we're done
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

}
