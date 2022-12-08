package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.CallSuper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.geeksville.mesh.ConfigProtos;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;

public class ChannelStatusViewModel extends DestroyableSharedPrefsListener {
    private final MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private final MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private final MutableLiveData<ConfigProtos.Config.LoRaConfig.ModemPreset> mModemConfig = new MutableLiveData<>();
    private final MutableLiveData<MeshtasticDevice> mCommDevice = new MutableLiveData<>();

    private final Gson mGson;
    private final HashHelper mHashHelper;

    public ChannelStatusViewModel(List<Destroyable> destroyables,
                                  SharedPreferences sharedPreferences,
                                  Gson gson,
                                  HashHelper hashHelper) {
        super(destroyables,
                sharedPreferences,
                new String[]{},
                new String[]{
                        PreferencesKeys.KEY_CHANNEL_NAME,
                        PreferencesKeys.KEY_CHANNEL_MODE,
                        PreferencesKeys.KEY_CHANNEL_PSK,
                        PreferencesKeys.KEY_SET_COMM_DEVICE
                });

        mGson = gson;
        mHashHelper = hashHelper;

        complexUpdate(sharedPreferences, "");
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

    @CallSuper
    public void clearData() {
        mChannelName.postValue(null);
        mModemConfig.postValue(null);
        mPskHash.postValue(null);
        mCommDevice.postValue(null);
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        // Do nothing
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        String channelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
        int channelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
        byte[] psk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);
        ConfigProtos.Config.LoRaConfig.ModemPreset modemConfig = ConfigProtos.Config.LoRaConfig.ModemPreset.forNumber(channelMode);
        String commDeviceStr = sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE);

        mChannelName.postValue(channelName);
        mModemConfig.postValue(modemConfig);
        mPskHash.postValue(mHashHelper.hashFromBytes(psk));
        mCommDevice.postValue(mGson.fromJson(commDeviceStr, MeshtasticDevice.class));
    }
}
