package com.paulmandal.atak.forwarder.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.meshtastic.MeshtasticDevice;

public class StateStorage {
    private static final String SHARED_PREFS_NAME = "atak-forwarder";
    
    private static final int DEFAULT_CACHE_PURGE_TIME_MS = Config.DEFAULT_CACHE_PURGE_TIME_MS;
    private static final int DEFAULT_PLI_CACHE_PURGE_TIME_MS = Config.DEFAULT_PLI_CACHE_PURGE_TIME_MS;

    private static final String KEY_PLI_CACHE_PURGE_TIME = "pliCachePurgeTime";
    private static final String KEY_DEFAULT_CACHE_PURGE_TIME = "defaultCachePurgeTime";
    private static final String KEY_COMM_DEVICE = "commDevice";

    private Context mContext;

    public StateStorage(Context context) {
        mContext = context;
    }

    public int getDefaultCachePurgeTimeMs() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPref.getInt(KEY_DEFAULT_CACHE_PURGE_TIME, DEFAULT_CACHE_PURGE_TIME_MS);
    }

    public int getPliCachePurgeTimeMs() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPref.getInt(KEY_PLI_CACHE_PURGE_TIME, DEFAULT_PLI_CACHE_PURGE_TIME_MS);
    }

    @Nullable
    public MeshtasticDevice getCommDevice() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_COMM_DEVICE, null);

        Gson gson = new Gson();
        return gson.fromJson(json, MeshtasticDevice.class);
    }

    public void storeCommDevice(MeshtasticDevice meshtasticDevice) {
        Gson gson = new Gson();
        String json = gson.toJson(meshtasticDevice);

        SharedPreferences sharedPref = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_COMM_DEVICE, json);
        editor.apply();
    }

    public void storeDefaultCachePurgeTime(int shapeCachePurgeTimeMs) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_DEFAULT_CACHE_PURGE_TIME, shapeCachePurgeTimeMs);
        editor.apply();
    }

    public void storePliCachePurgeTime(int pliCachePurgeTimeMs) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_PLI_CACHE_PURGE_TIME, pliCachePurgeTimeMs);
        editor.apply();
    }
}
