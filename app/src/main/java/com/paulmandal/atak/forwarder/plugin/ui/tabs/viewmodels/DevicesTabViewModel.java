package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.util.List;
import java.util.Set;

public class DevicesTabViewModel implements MeshtasticCommHardware.ChannelSettingsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTabViewModel.class.getSimpleName();

    private HashHelper mHashHelper;

    private MutableLiveData<List<String>> mMeshDevices = new MutableLiveData<>();
    private MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private MutableLiveData<Byte[]> mPsk = new MutableLiveData<>();
    private MutableLiveData<MeshProtos.ChannelSettings.ModemConfig> mModemConfig = new MutableLiveData<>();

    public DevicesTabViewModel(MeshtasticCommHardware commHardware,
                               HashHelper hashHelper) {

        mHashHelper = hashHelper;

        commHardware.addChannelSettingsListener(this);
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
    public LiveData<List<String>> getMeshDevices() {
        return mMeshDevices;
    }

    public void scanForDevices() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevicesList = btAdapter.getBondedDevices();


        for (BluetoothDevice pairedDevice : pairedDevicesList) {
            Log.e(TAG, "pairedDevice.getName(): " + pairedDevice.getName());
            Log.e(TAG, "pairedDevice.getAddress(): " + pairedDevice.getAddress());
            Log.e(TAG, "type: " + pairedDevice.getType());
        }
    }

    public void connectToDevice() {
        // Connect to a new device
    }

    public void writeToDevice() {
        // Write settings to device
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
