package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.ArrayList;
import java.util.List;

public class LoggingViewModel implements Logger.Listener {
    public static class LogMessage {
        public String tag;
        public String message;

        public LogMessage(String tag, String message) {
            this.tag = tag;
            this.message = message;
        }
    }

    private final MutableLiveData<List<LogMessage>> mLogMessages = new MutableLiveData<>();

    public LoggingViewModel(Logger logger) {
        logger.addListener(this);

        mLogMessages.setValue(new ArrayList<>());
    }

    public MutableLiveData<List<LogMessage>> getLogMessages() {
        return mLogMessages;
    }

    @Override
    public void onLogMessage(String tag, String message) {
        List<LogMessage> messages = mLogMessages.getValue();
        messages.add(new LogMessage(tag, message));
        mLogMessages.setValue(messages);
    }
}
