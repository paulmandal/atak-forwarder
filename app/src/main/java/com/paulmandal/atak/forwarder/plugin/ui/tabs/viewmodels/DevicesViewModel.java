package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;

import java.util.List;

public class DevicesViewModel {
    private DevicesList mDevicesList;

    private MutableLiveData<List<MeshtasticDevice>> mMeshtasticDevices = new MutableLiveData<>();

    public DevicesViewModel(DevicesList devicesList) {
        mDevicesList = devicesList;
    }

    public LiveData<List<MeshtasticDevice>> getMeshtasticDevices() {
        return mMeshtasticDevices;
    }

    public void refreshDevices() {
        mMeshtasticDevices.setValue(mDevicesList.getMeshtasticDevices());
    }
}
