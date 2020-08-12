
package com.paulmandal.atak.forwarder.plugin;

import android.app.Activity;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.paulmandal.atak.forwarder.factories.CommHardwareFactory;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class ForwarderLifecycle implements Lifecycle {
    private final static String TAG = "ATAKDBG." + ForwarderLifecycle.class.getSimpleName();

    private Activity mActivity;

    private CommHardware mCommHardware;
    private InboundMessageHandler mInboundMessageHandler;
    private OutboundMessageHandler mOutboundMessageHandler;

    @Override
    public void onCreate(final Activity activity, final transapps.mapi.MapView transappsMapView) {
        if (transappsMapView == null || !(transappsMapView.getView() instanceof MapView)) {
            Log.e(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        mActivity = activity;
    }

    @Override
    public void onDestroy() {
        mOutboundMessageHandler.destroy();
        mCommHardware.destroy();
    }


    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        mCommHardware = CommHardwareFactory.getCommHardware(mActivity);
        mInboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler(mActivity);
        mOutboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler(mActivity);
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
