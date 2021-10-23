package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import android.content.SharedPreferences;

import androidx.annotation.CallSuper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.gson.ChannelConfig;
import com.paulmandal.atak.forwarder.helpers.ChannelJsonException;
import com.paulmandal.atak.forwarder.helpers.ChannelJsonHelper;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;

public class ChannelStatusViewModel extends DestroyableSharedPrefsListener {
    private final MutableLiveData<String> mChannelName = new MutableLiveData<>();
    private final MutableLiveData<String> mPskHash = new MutableLiveData<>();
    private final MutableLiveData<Integer> mModemConfig = new MutableLiveData<>();

    private final HashHelper mHashHelper;
    private final ChannelJsonHelper mChannelJsonHelper;

    public ChannelStatusViewModel(List<Destroyable> destroyables,
                                  SharedPreferences sharedPreferences,
                                  HashHelper hashHelper,
                                  ChannelJsonHelper channelJsonHelper) {
        super(destroyables,
                sharedPreferences,
                new String[] {},
                new String[]{
                        PreferencesKeys.KEY_CHANNEL_DATA,
                });

        mHashHelper = hashHelper;
        mChannelJsonHelper = channelJsonHelper;

        complexUpdate(sharedPreferences, "");
    }

    public LiveData<String> getChannelName() {
        return mChannelName;
    }

    public LiveData<Integer> getModemConfig() {
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

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        // Do nothing
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // This is in complexUpdate because simpleUpdate is called before the MutableLiveData are available
        String channelJson = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_DATA, PreferencesDefaults.DEFAULT_CHANNEL_DATA);
        List<ChannelConfig> channelConfigs;
        try {
            channelConfigs = mChannelJsonHelper.listFromJson(channelJson);
        } catch (ChannelJsonException e) {
            return;
        }

        boolean first = true;
        for (ChannelConfig channelConfig : channelConfigs) {
            if (channelConfig.isDefault || first) {
                first = false;
                mChannelName.postValue(channelConfig.name);
                mModemConfig.postValue(channelConfig.modemConfig);
                mPskHash.postValue(mHashHelper.hashFromBytes(channelConfig.psk));
            }
        }
    }
}
