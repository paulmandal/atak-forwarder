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

import java.util.List;

public class DevicesTabViewModel implements MeshtasticCommHardware.ChannelSettingsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + DevicesTabViewModel.class.getSimpleName();

    private static final String MARKER_MESHTASTIC = "Meshtastic";

    private Context mAtakContext;
    private MeshtasticCommHardware mMeshtasticCommHardware;
    private HashHelper mHashHelper;

    private MutableLiveData<List<String>> mMeshDevices = new MutableLiveData<>();
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
//        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothManager bm = (BluetoothManager) mAtakContext.getSystemService(Context.BLUETOOTH_SERVICE);
        List<BluetoothDevice> devices = bm.getConnectedDevices(BluetoothGatt.GATT);
        int status = -1;

        for (BluetoothDevice device : devices) {
            status = bm.getConnectionState(device, BluetoothGatt.GATT);
            Log.e(TAG, "pairedDevice.getName(): " + device.getName());
            Log.e(TAG, "pairedDevice.getAddress(): " + device.getAddress());
            Log.e(TAG, "type: " + device.getType());
            Log.e(TAG, "address: " + device.getAddress());
            Log.e(TAG, "bond state: " + device.getBondState());
            Log.e(TAG, "status: " + status);
            // compare status to:
            //   BluetoothProfile.STATE_CONNECTED
            //   BluetoothProfile.STATE_CONNECTING
            //   BluetoothProfile.STATE_DISCONNECTED
            //   BluetoothProfile.STATE_DISCONNECTING
        }

//        Set<BluetoothDevice> pairedDevicesList = btAdapter.getBondedDevices();
//        for (BluetoothDevice pairedDevice : pairedDevicesList) {
//            String deviceName = pairedDevice.getName();
//            if (deviceName.startsWith(MARKER_MESHTASTIC)) {
//                String address = pairedDevice.getAddress();
//                Log.e(TAG, "pairedDevice.getName(): " + deviceName);
//                Log.e(TAG, "pairedDevice.getAddress(): " + address);
//                Log.e(TAG, "type: " + pairedDevice.getType());
//                Log.e(TAG, "address: " + pairedDevice.getAddress());
//                Log.e(TAG, "bond state: " + pairedDevice.getBondState());
//            }
//        }

        Log.e(TAG, "getDeviceAddress: " + mMeshtasticCommHardware.getDeviceAddress());
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
