package com.paulmandal.atak.gotenna.mesh;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.paulmandal.atak.gotenna.mesh.services.UdpListenerService;

public class MainActivity extends AppCompatActivity implements UdpListenerService.MessageListener {
    private TextView mOutput;
    UdpListenerService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOutput = findViewById(R.id.text_output);

        findViewById(R.id.button_clear).setOnClickListener((View v) -> {
            mOutput.setText("");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, UdpListenerService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        mBound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            UdpListenerService.LocalBinder binder = (UdpListenerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.addListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onMessage(String senderIp, String message) {
        Log.d("UDPDBG", "onMessage, ip: " + senderIp + ", msg: " + message);
        String existing = mOutput.getText().toString();
        final String output = existing + "\nUDP unicast from " + senderIp + ", message: " + message + ", len: " + message.length() + "\n";
        runOnUiThread(() -> mOutput.setText(output));
    }
}