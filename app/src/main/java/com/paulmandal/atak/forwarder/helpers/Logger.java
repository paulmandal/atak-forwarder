package com.paulmandal.atak.forwarder.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;

public class Logger extends DestroyableSharedPrefsListener {

    private final Context mAtakContext;
    private Toast mToast;
    private boolean mEnableLogging;
    private boolean mEnableLoggingToasts;

    public Logger(List<Destroyable> destroyables, SharedPreferences sharedPreferences, Context atakContext) {
        super(destroyables, sharedPreferences,
                new String[] {
                        PreferencesKeys.KEY_ENABLE_LOGGING,
                        PreferencesKeys.KEY_ENABLE_LOGGING_TOASTS
                },
                new String[] {});

        mAtakContext = atakContext;
    }

    public void v(String tag, String message) {
        if (mEnableLogging) {
            Log.v(tag, message);
        }
        maybeToast(tag, message);
    }

    public void d(String tag, String message) {
        if (mEnableLogging) {
            Log.d(tag, message);
        }
        maybeToast(tag, message);
    }

    public void i(String tag, String message) {
        if (mEnableLogging) {
            Log.i(tag, message);
        }
        maybeToast(tag, message);
    }

    public void e(String tag, String message) {
        if (mEnableLogging) {
            Log.e(tag, message);
        }
        maybeToast(tag, message);
    }

    private void maybeToast(String tag, String message) {
        if (mEnableLoggingToasts) {
            if (mToast != null) {
                mToast.cancel();
            }

            mToast = Toast.makeText(mAtakContext, tag + ": " + message, Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        mEnableLogging = sharedPreferences.getBoolean(PreferencesKeys.KEY_ENABLE_LOGGING, PreferencesDefaults.DEFAULT_ENABLE_LOGGING);
        mEnableLoggingToasts = sharedPreferences.getBoolean(PreferencesKeys.KEY_ENABLE_LOGGING_TOASTS, PreferencesDefaults.DEFAULT_ENABLE_LOGGING_TOASTS);
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // Do nothing
    }
}
