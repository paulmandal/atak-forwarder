package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;

public class LoggingScreen extends ConstraintLayout {
    private final TextView mLoggingTextView;

    public LoggingScreen(Context context) {
        this(context, null);
    }

    public LoggingScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoggingScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.logging_layout, this);

        mLoggingTextView = findViewById(R.id.logging_textview);
    }

    public void bind(LifecycleOwner lifecycleOwner,
                     LoggingViewModel loggingViewModel) {
        loggingViewModel.getLogMessages().observe(lifecycleOwner, logMessages -> {
            StringBuilder sb = new StringBuilder();
            for (LoggingViewModel.LogMessage logMessage : logMessages) {
                sb.append(String.format("%s: %s\n", logMessage.tag, logMessage.message));
            }
            mLoggingTextView.setText(sb.toString());
        });
    }
}
