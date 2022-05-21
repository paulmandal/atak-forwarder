package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;

public class LoggingScreen extends ConstraintLayout {
    private final RecyclerView mLoggingRecyclerView;

    public LoggingScreen(Context context) {
        this(context, null);
    }

    public LoggingScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoggingScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.logging_layout, this);

        mLoggingRecyclerView = findViewById(R.id.logging_recyclerview);
    }

    public void bind(LifecycleOwner lifecycleOwner,
                     Context pluginContext,
                     LoggingViewModel loggingViewModel) {
        loggingViewModel.getLogMessages().observe(lifecycleOwner, logMessages -> {
            LogMessageAdapter logMessageAdapter = new LogMessageAdapter(pluginContext, logMessages);
            mLoggingRecyclerView.setAdapter(logMessageAdapter);
        });
    }
}
