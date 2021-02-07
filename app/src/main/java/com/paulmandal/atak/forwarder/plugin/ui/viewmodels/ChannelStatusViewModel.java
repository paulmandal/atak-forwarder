package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import androidx.annotation.CallSuper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.helpers.HashHelper;

public class ChannelStatusViewModel {
    private HashHelper mHashHelper;

    private MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private MutableLiveData<MeshProtos.ChannelSettings.ModemConfig> mModemConfig = new MutableLiveData<>();

    public ChannelStatusViewModel(MeshtasticCommHardware commHardware,
                                  HashHelper hashHelper) {

        mHashHelper = hashHelper;
    }

    public LiveData<String> getChannelName() {
        return mChannelName;
    }

    public LiveData<MeshProtos.ChannelSettings.ModemConfig> getModemConfig() {
        return mModemConfig;
    }

    public LiveData<String> getPskHash() {
        return mPskHash;
    }

    @CallSuper
    public void clearData() {
        mChannelName.postValue(null);
        mModemConfig.postValue(null);
        mPskHash.postValue(null);
    }
}
