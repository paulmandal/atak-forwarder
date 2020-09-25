package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import androidx.annotation.CallSuper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

public class ChannelStatusViewModel implements MeshtasticCommHardware.ChannelSettingsListener {
    private HashHelper mHashHelper;

    private MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private MutableLiveData<MeshProtos.ChannelSettings.ModemConfig> mModemConfig = new MutableLiveData<>();

    public ChannelStatusViewModel(MeshtasticCommHardware commHardware,
                                  HashHelper hashHelper) {

        mHashHelper = hashHelper;

        commHardware.addChannelSettingsListener(this);
    }

    @Override
    @CallSuper
    public void onChannelSettingsUpdated(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        mChannelName.setValue(channelName);
        mModemConfig.setValue(modemConfig);
        mPskHash.setValue(mHashHelper.hashFromBytes(psk));
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
