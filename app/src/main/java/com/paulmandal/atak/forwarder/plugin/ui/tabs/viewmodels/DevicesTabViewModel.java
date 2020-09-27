package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.CallSuper;
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

    private String mChannelName;
    private String mPskHash;
    private byte[] mPsk;
    private MeshProtos.ChannelSettings.ModemConfig mModemConfig;

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

    public LiveData<List<BluetoothDevice>> getBluetoothDevices() {
        return mBluetoothDevices;
    }

    public LiveData<String> getCommDeviceAddress() {
        return mCommDeviceAddress;
    }

    public LiveData<Boolean> getNonAtakDeviceWriteInProgress() {
        return mNonAtakDeviceWriteInProgress;
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

        mBluetoothDevices.setValue(filteredDevices);
    }

    public void setCommDeviceAddress(String deviceAddress) {
        if (mMeshtasticCommHardware.setDeviceAddress(deviceAddress)) {
            mCommDeviceAddress.setValue(deviceAddress);
        }
    }

    public void writeToNonAtak(String deviceAddress, String deviceCallsign, int teamIndex, int roleIndex, int refreshIntervalS, int screenShutoffDelayS) {
        if (deviceAddress.equals(mCommDeviceAddress.getValue())
                || mChannelName == null
                || mModemConfig == null) {
            Log.e(TAG, "Attempt to write to CommDevice address or write without channel settings!");
            return;
        }

        mNonAtakDeviceWriteInProgress.setValue(true);

        if (mNonAtakMeshtasticConfigurator != null) {
            mNonAtakMeshtasticConfigurator.cancel();
        } else {
            mMeshtasticCommHardware.suspendResume(true);
        }

        // Write settings to device
        mNonAtakMeshtasticConfigurator = new NonAtakMeshtasticConfigurator(mActivity, mUiThreadHandler, mCommDeviceAddress.getValue(), deviceAddress, deviceCallsign, mChannelName, mPsk, mModemConfig, teamIndex, roleIndex, refreshIntervalS, screenShutoffDelayS, this);
        mNonAtakMeshtasticConfigurator.writeToDevice();
    }

    @Override
    @CallSuper
    public void onChannelSettingsUpdated(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        mChannelName = channelName;
        mModemConfig = modemConfig;
        mPskHash = mHashHelper.hashFromBytes(psk);
        mPsk = psk;
    }

    @Override
    public void onDoneWritingToDevice() {
        mMeshtasticCommHardware.suspendResume(false);
        mNonAtakMeshtasticConfigurator = null;
        mNonAtakDeviceWriteInProgress.setValue(false);
    }
}
