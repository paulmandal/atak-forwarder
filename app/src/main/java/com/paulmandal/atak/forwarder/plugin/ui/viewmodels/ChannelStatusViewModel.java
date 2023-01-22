package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.CallSuper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.ConfigProtos;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.comm.meshtastic.DeviceConfigObserver;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshDeviceConfigurator;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;

public class ChannelStatusViewModel implements DeviceConfigObserver.Listener {
    private final MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private final MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private final MutableLiveData<ConfigProtos.Config.LoRaConfig.ModemPreset> mModemConfig = new MutableLiveData<>();
    private final MutableLiveData<MeshtasticDevice> mCommDevice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mPluginManagesDevice = new MutableLiveData<>();

    private final HashHelper mHashHelper;

    public ChannelStatusViewModel(DeviceConfigObserver deviceConfigObserver,
                                  HashHelper hashHelper,
                                  String channelName,
                                  byte[] psk,
                                  ConfigProtos.Config.LoRaConfig.ModemPreset modemConfig,
                                  MeshtasticDevice meshtasticDevice,
                                  boolean pluginManagesDevice) {
        mHashHelper = hashHelper;

        mChannelName.postValue(channelName);
        mModemConfig.postValue(modemConfig);
        mPskHash.postValue(mHashHelper.hashFromBytes(psk));
        mCommDevice.postValue(meshtasticDevice);
        mPluginManagesDevice.postValue(pluginManagesDevice);

        deviceConfigObserver.addListener(this);
    }

    public LiveData<String> getChannelName() {
        return mChannelName;
    }

    public LiveData<ConfigProtos.Config.LoRaConfig.ModemPreset> getModemPreset() {
        return mModemConfig;
    }

    public LiveData<String> getPskHash() {
        return mPskHash;
    }

    public LiveData<MeshtasticDevice> getCommDevice() {
        return mCommDevice;
    }

    public LiveData<Boolean> getPluginManagesDevice() { return mPluginManagesDevice; }

    @CallSuper
    public void clearData() {
        mChannelName.postValue(null);
        mModemConfig.postValue(null);
        mPskHash.postValue(null);
        mCommDevice.postValue(null);
    }

    @Override
    public void onSelectedDeviceChanged(MeshtasticDevice meshtasticDevice) {
        mCommDevice.postValue(meshtasticDevice);
    }

    @Override
    public void onDeviceConfigChanged(ConfigProtos.Config.LoRaConfig.RegionCode regionCode, String channelName, int channelMode, byte[] channelPsk, ConfigProtos.Config.DeviceConfig.Role routingRole) {
        mChannelName.postValue(channelName);
        mModemConfig.postValue(ConfigProtos.Config.LoRaConfig.ModemPreset.forNumber(channelMode));
        mPskHash.postValue(mHashHelper.hashFromBytes(channelPsk));
    }

    @Override
    public void onPluginManagesDeviceChanged(boolean pluginManagesDevice) {
        mPluginManagesDevice.postValue(pluginManagesDevice);
    }
}
