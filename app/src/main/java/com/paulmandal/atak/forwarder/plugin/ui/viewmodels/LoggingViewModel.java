package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.ArrayList;
import java.util.List;

public class LoggingViewModel extends DestroyableSharedPrefsListener implements Logger.Listener {
    private static final int MAX_LOG_MESSAGES_OUTPUT = ForwarderConstants.MAX_LOG_MESSAGES_OUTPUT;

    public static class LogMessage {
        public final int level;
        public final String tag;
        public final String message;

        public LogMessage(int level, String tag, String message) {
            this.level = level;
            this.tag = tag;
            this.message = message;
        }
    }

    private final List<LogMessage> mUnfilteredLogMessages = new ArrayList<>();
    private final MutableLiveData<List<LogMessage>> mLogMessages = new MutableLiveData<>(new ArrayList<>());

    private int mLoggingLevel;

    public LoggingViewModel(List<Destroyable> destroyables, SharedPreferences sharedPreferences, Logger logger) {
        super(destroyables, sharedPreferences,
                new String[] {
                        PreferencesKeys.KEY_SET_LOGGING_LEVEL
                },
                new String[] {});

        logger.addListener(this);
    }

    public LiveData<List<LogMessage>> getLogMessages() {
        return mLogMessages;
    }

    @Override
    public void onLogMessage(int level, String tag, String message) {
        if (mUnfilteredLogMessages.size() > MAX_LOG_MESSAGES_OUTPUT) {
            mUnfilteredLogMessages.remove(0);
        }
        LogMessage logMessage = new LogMessage(level, tag, message);
        mUnfilteredLogMessages.add(logMessage);

        if (level >= mLoggingLevel) {
            List<LogMessage> messages = mLogMessages.getValue();
            messages.add(logMessage);
            mLogMessages.postValue(messages);
        }
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        mLoggingLevel = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_SET_LOGGING_LEVEL, PreferencesDefaults.DEFAULT_LOGGING_LEVEL));

        updateFilteredMessages();
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // Do nothing
    }

    private void updateFilteredMessages() {
        if (mUnfilteredLogMessages == null) {
            return;
        }

        List<LogMessage> messages = new ArrayList<>();

        for (LogMessage message : mUnfilteredLogMessages) {
            if (message.level >= mLoggingLevel) {
                messages.add(message);
            }
        }

        mLogMessages.postValue(messages);
    }
}
