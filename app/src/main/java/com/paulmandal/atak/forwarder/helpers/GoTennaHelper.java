package com.paulmandal.atak.forwarder.helpers;

import android.app.Activity;
import android.util.Log;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.connection.BluetoothAdapterManager;
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.paulmandal.atak.forwarder.Config;

/**
 * Checks permissions to ensure GoTenna can be used and sets up the GoTenna SDK
 *
 * Note: You must forward Activity.onActivityResult(int,int,Intent) to this class
 */
public class GoTennaHelper {
//    public interface Listener {
//        void onGoTennaSetupComplete();
//    }
//
    private static final String GOTENNA_SDK_TOKEN = Config.GOTENNA_SDK_TOKEN;

    private static final String TAG = "ATAKDBG." + GoTennaHelper.class.getSimpleName();

    private static final int ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE = 1003;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 54321;

//    private Activity mActivity;
//    private Listener mListener;

//    public GoTennaHelper(Activity activity, Listener listener) {
//        mActivity = activity;
//        mListener = listener;
//    }

    public static void checkPermissionsAndSetupSdk(Activity activity) {
//        if (!PermissionUtil.hasLocationPermission(activity)) {
//            PermissionUtil.requestLocationPermission(activity, LOCATION_PERMISSION_REQUEST_CODE);
//        }

        try {
            GoTenna.setApplicationToken(activity, GOTENNA_SDK_TOKEN);
        } catch (GTInvalidAppTokenException e) {
            e.printStackTrace();
        }

        checkBluetooth(activity);
    }

    private static void checkBluetooth(Activity activity) {
        BluetoothAdapterManager bluetoothAdapterManager = BluetoothAdapterManager.getInstance();
        BluetoothAdapterManager.BluetoothStatus bluetoothStatus = bluetoothAdapterManager.getBluetoothStatus();

        switch (bluetoothStatus) {
            case SUPPORTED_AND_ENABLED:
                break;
            case SUPPORTED_NOT_ENABLED:
                BluetoothAdapterManager.showRequestBluetoothPermissionDialog(activity, ENABLE_BLUETOOTH_PERMISSION_REQUEST_CODE);
                break;
            case NOT_SUPPORTED:
                Log.e(TAG, "Bluetooth not supported");
                break;
        }
    }
}
