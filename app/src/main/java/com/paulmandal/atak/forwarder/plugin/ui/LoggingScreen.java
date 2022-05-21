package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;

public class LoggingScreen extends ConstraintLayout {
    private final ListView mLoggingListView;

    public LoggingScreen(Context context) {
        this(context, null);
    }

    public LoggingScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoggingScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.logging_layout, this);

        mLoggingListView = findViewById(R.id.listview_logging_messages);
    }

    public void bind(LifecycleOwner lifecycleOwner,
                     Context pluginContext,
                     LoggingViewModel loggingViewModel) {
        loggingViewModel.getLogMessages().observe(lifecycleOwner, logMessages -> {
            LogMessageDataAdapter logMessageAdapter = new LogMessageDataAdapter(pluginContext, logMessages);
            mLoggingListView.setAdapter(logMessageAdapter);
        });
    }
}
