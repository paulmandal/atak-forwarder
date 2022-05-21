package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.paulmandal.atak.forwarder.R;

public class LoggingScreen extends ConstraintLayout {
    public LoggingScreen(Context context) {
        this(context, null);
    }

    public LoggingScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoggingScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.logging_layout, this);

        // TODO: View stuff
        // TODO: ViewModel
    }
}
