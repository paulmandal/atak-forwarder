package com.paulmandal.atak.forwarder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.paulmandal.atak.forwarder.helpers.GoTennaHelper;
import com.paulmandal.atak.forwarder.services.ForwardingService;

public class MainActivity extends AppCompatActivity implements GoTennaHelper.Listener, ForwardingService.MessageListener {
    /**
     * Basic configuration
     */
    public static final String GOTENNA_SDK_TOKEN = ;

    /**
     * IMPORTANT this is used to set the GoTenna frequencies, please adjust to your approx lat/lon
     */
    public static final double LATITUDE = ; //40.619373
    public static final double LONGITUDE = ; //-74.102977

    /**
     * You will need one primary and one secondary device to test this, when building for the secondary device set this to false
     *
     * Basically this sets the GIDs
     */
    private static final boolean PRIMARY_DEVICE = true;

    /**
     * Tweaks to message handling -- GoTenna max message length is 235 bytes with a max transmission rate of 5 msgs per minute (approx, according to their error messages)
     */
    public static final int MAX_MESSAGE_LENGTH = 192;
    public static final int MESSAGE_CHUNK_LENGTH = 192;
    public static final int DELAY_BETWEEN_MESSAGES_MS = 10000;

    /**
     * Test GIDs
     */
    public static final long GOTENNA_LOCAL_GID = PRIMARY_DEVICE ? 123456789 : 987654321;
    public static final long GOTENNA_REMOTE_GID = PRIMARY_DEVICE ? 987654321 : 123456789;

    /**
     * IP and port to listen for outbound messages on, should match what you have configured in ATAK
     * under Settings / Network Connections / Network Connections / Manage Outputs
     */
    public static final int OUTBOUND_MESSAGE_PORT = 31337;

    /**
     * IP and port to retransmit inbound messages to, this should work with the defaults in ATAK
     * Settings / Network Connections / Network Connections / Manage Inputs / 0.0.0.0:4242:udp
     */
    public static final int INBOUND_MESSAGE_DEST_PORT = 4242;

    /**
     * This can be anything > 1024 and < 65535
     */
    public static final int INBOUND_MESSAGE_SRC_PORT = 17233;

    GoTennaHelper mGotennaHelper;

    ForwardingService mService;
    boolean mBound = false;

    private TextView mOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOutput = findViewById(R.id.text_output);

        findViewById(R.id.button_clear).setOnClickListener((View v) -> mOutput.setText(""));

        findViewById(R.id.button_exit).setOnClickListener((View v) -> {
            Intent intent = new Intent(this, ForwardingService.class);
            unbindService(mConnection);
            stopService(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGotennaHelper = new GoTennaHelper(this, this);
        mGotennaHelper.checkPermissionsAndSetupSdk();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGotennaHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onGoTennaSetupComplete() {
        bindAndStartService();
    }

    private void bindAndStartService() {
        // Bind to LocalService
        Intent intent = new Intent(this, ForwardingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        ContextCompat.startForegroundService(this, intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        mBound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ForwardingService.ForwardingServiceBinder binder = (ForwardingService.ForwardingServiceBinder) service;
            mService = binder.getService();

            mService.addListener(MainActivity.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onOutboundMessage(String message) {
        appendLogging("OUT: " + message);
    }

    @Override
    public void onInboundMessage(String message) {
        appendLogging("IN: " + message);
    }

    public void appendLogging(String message) {
        String existing = mOutput.getText().toString();
        final String output = existing + "\n" + message + "\n";
        runOnUiThread(() -> mOutput.setText(output));
    }
}