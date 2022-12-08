package com.paulmandal.atak.forwarder.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.DebugUtils;
import android.util.Log;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Logger extends DestroyableSharedPrefsListener {
    public static final int LOG_LEVEL_VERBOSE = 2;
    public static final int LOG_LEVEL_DEBUG = 3;
    public static final int LOG_LEVEL_INFO = 4;
    public static final int LOG_LEVEL_WARN = 5;
    public static final int LOG_LEVEL_ERROR = 6;

    public interface Listener {
        void onLogMessage(int level, String tag, String message);
    }

    private final Handler mUiThreadHandler;

    private boolean mEnableLogging;

    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    public Logger(List<Destroyable> destroyables, SharedPreferences sharedPreferences, Handler uiThreadHandler) {
        super(destroyables, sharedPreferences,
                new String[] {
                        PreferencesKeys.KEY_ENABLE_LOGGING
                },
                new String[] {});
        mUiThreadHandler = uiThreadHandler;
    }

    public void v(String tag, String message) {
        if (mEnableLogging) {
            Log.v(tag, message);
        }
        notifyListeners(LOG_LEVEL_VERBOSE, tag, message);
    }

    public void d(String tag, String message) {
        if (mEnableLogging) {
            Log.d(tag, message);
        }
        notifyListeners(LOG_LEVEL_DEBUG, tag, message);
    }

    public void i(String tag, String message) {
        if (mEnableLogging) {
            Log.i(tag, message);
        }
        notifyListeners(LOG_LEVEL_INFO, tag, message);
    }

    public void e(String tag, String message) {
        if (mEnableLogging) {
            Log.e(tag, message);
        }
        notifyListeners(LOG_LEVEL_ERROR, tag, message);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(int level, String tag, String message) {
        for(Listener listener : mListeners) {
            mUiThreadHandler.post(() -> listener.onLogMessage(level, tag, message));
        }
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        mEnableLogging = sharedPreferences.getBoolean(PreferencesKeys.KEY_ENABLE_LOGGING, PreferencesDefaults.DEFAULT_ENABLE_LOGGING);
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // Do nothing
    }
}
