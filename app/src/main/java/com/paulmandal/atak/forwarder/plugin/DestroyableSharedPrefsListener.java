package com.paulmandal.atak.forwarder.plugin;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.android.maps.MapView;

import java.util.List;

public abstract class DestroyableSharedPrefsListener implements Destroyable, SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences mSharedPreferences;

    public DestroyableSharedPrefsListener(List<Destroyable> destroyables, SharedPreferences sharedPreferences) {
        destroyables.add(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void onDestroy(Context context, MapView mapView) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}
