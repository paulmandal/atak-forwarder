package com.paulmandal.atak.gotenna.mesh.helpers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.connection.BluetoothAdapterManager;
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.paulmandal.atak.gotenna.mesh.MainActivity;
import com.paulmandal.atak.gotenna.mesh.utils.PermissionUtil;

/**
 * Checks permissions to ensure GoTenna can be used and sets up the GoTenna SDK
 *
 * Note: You must forward Activity.onActivityResult(int,int,Intent) to this class
 */
public class GoTennaHelper {
    public interface Listener {
        void onGoTennaSetupComplete();
    }

    private static final String GOTENNA_SDK_TOKEN = MainActivity.GOTENNA_SDK_TOKEN;

    private static final String TAG = "ATAKDBG." + GoTennaHelper.class.getSimpleName();

    private static final int ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE = 1003;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 54321;

    private Activity mActivity;
    private Listener mListener;

    public GoTennaHelper(Activity activity, Listener listener) {
        mActivity = activity;
        mListener = listener;
    }

    public void checkPermissionsAndSetupSdk() {
        if (!PermissionUtil.hasLocationPermission(mActivity)) {
            PermissionUtil.requestLocationPermission(mActivity, LOCATION_PERMISSION_REQUEST_CODE);
        }

        try {
            GoTenna.setApplicationToken(mActivity, GOTENNA_SDK_TOKEN);
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
                mListener.onGoTennaSetupComplete();
                destroy();
                break;
            case SUPPORTED_NOT_ENABLED:
                BluetoothAdapterManager.showRequestBluetoothPermissionDialog(mActivity, ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE);
                break;
            case NOT_SUPPORTED:
                Log.e(TAG, "Bluetooth not supported");
                destroy();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            checkBluetooth();
        }
    }

    private void destroy() {
        mListener = null;
        mActivity = null;
    }
}
