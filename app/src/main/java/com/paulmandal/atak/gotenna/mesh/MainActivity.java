package com.paulmandal.atak.gotenna.mesh;

import android.app.Activity;
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

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.connection.BluetoothAdapterManager;
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.paulmandal.atak.gotenna.mesh.services.ForwardingService;
import com.paulmandal.atak.gotenna.mesh.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity implements ForwardingService.MessageListener {
    private static final String GOTENNA_SDK_TOKEN = "";

    private static final int ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE = 1003;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 54321;
    private static final int SCAN_TIMEOUT_MILLISECONDS = 30000;

    private TextView mOutput;
    ForwardingService mService;
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
        // Check perms
        if (!PermissionUtil.hasLocationPermission(this)) {
            PermissionUtil.requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE);
//            return;
        }
//        UserDataStore userDateStore = UserDataStore.getInstance();

        try {
            GoTenna.setApplicationToken(getApplicationContext(), GOTENNA_SDK_TOKEN);
        } catch (GTInvalidAppTokenException e) {
            e.printStackTrace();
        }

        checkBluetooth();
    }

    private void checkBluetooth() {
        BluetoothAdapterManager bluetoothAdapterManager = BluetoothAdapterManager.getInstance();
        BluetoothAdapterManager.BluetoothStatus bluetoothStatus = bluetoothAdapterManager.getBluetoothStatus();

        switch (bluetoothStatus) {
            case SUPPORTED_AND_ENABLED:
                bindAndStartService();
                break;
            case SUPPORTED_NOT_ENABLED:
                BluetoothAdapterManager.showRequestBluetoothPermissionDialog(this, ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE);
                break;
            case NOT_SUPPORTED:
                // TODO: handle this case
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            checkBluetooth();
        }
    }

    private void bindAndStartService() {
        // Bind to LocalService
        Intent intent = new Intent(this, ForwardingService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
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
            ForwardingService.ForwardingServiceBinder binder = (ForwardingService.ForwardingServiceBinder) service;
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