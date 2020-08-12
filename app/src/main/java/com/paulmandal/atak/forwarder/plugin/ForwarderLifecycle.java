
package com.paulmandal.atak.forwarder.plugin;

import android.app.Activity;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.paulmandal.atak.forwarder.factories.CommHardwareFactory;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.GoTennaHelper;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class ForwarderLifecycle implements Lifecycle {
    private final static String TAG = "ATAKDBG." + ForwarderLifecycle.class.getSimpleName();

//    private final Context mContext;
    private Activity mActivity;
//    private MapView mMapView;

    private CommHardware mCommHardware;
    private InboundMessageHandler mInboundMessageHandler;
    private OutboundMessageHandler mOutboundMessageHandler;

    public ForwarderLifecycle() { //Context ctx) {
//        this.mContext = ctx;
//        this.mMapView = null;
//        PluginNativeLoader.init(ctx);
    }

    @Override
    public void onCreate(final Activity activity, final transapps.mapi.MapView transappsMapView) {
        if (transappsMapView == null || !(transappsMapView.getView() instanceof MapView)) {
            Log.e(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        mActivity = activity;
//        mMapView = (MapView) transappsMapView.getView();
    }

    @Override
    public void onDestroy() {
        mOutboundMessageHandler.destroy();
        mCommHardware.destroy();
    }


    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        // TODO: this won't wait for activity result, however we should already have Bluetooth perms via ATAK
        GoTennaHelper.checkPermissionsAndSetupSdk(mActivity);

        mCommHardware = CommHardwareFactory.getCommHardware();
        mInboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler();
        mOutboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler();
    }

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void onFinish() {}

    @Override
    public void onStop() {}

    @Override
    public void onConfigurationChanged(Configuration configuration) {}
}
