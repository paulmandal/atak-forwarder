package com.paulmandal.atak.forwarder.plugin;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.CallSuper;

import com.atakmap.android.maps.MapView;

import java.util.Arrays;
import java.util.List;

public abstract class DestroyableSharedPrefsListener implements Destroyable, SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences mSharedPreferences;

    List<String> mSimplePreferencesKeys;
    List<String> mComplexPreferencesKeys;

    public DestroyableSharedPrefsListener(List<Destroyable> destroyables,
                                          SharedPreferences sharedPreferences,
                                          String[] simplePreferencesKeys,
                                          String[] complexPreferencesKeys) {
        mSharedPreferences = sharedPreferences;
        mSimplePreferencesKeys = Arrays.asList(simplePreferencesKeys);
        mComplexPreferencesKeys = Arrays.asList(complexPreferencesKeys);

        destroyables.add(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        updateSettings(sharedPreferences);
        complexUpdate(sharedPreferences, "");
    }

    @CallSuper
    public void onDestroy(Context context, MapView mapView) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mSimplePreferencesKeys.contains(key)) {
            updateSettings(sharedPreferences);
        } else if (mComplexPreferencesKeys.contains(key)) {
            complexUpdate(sharedPreferences, key);
        }
    }

    protected abstract void updateSettings(SharedPreferences sharedPreferences);
    protected abstract void complexUpdate(SharedPreferences sharedPreferences, String key); // TODO: rename
}
