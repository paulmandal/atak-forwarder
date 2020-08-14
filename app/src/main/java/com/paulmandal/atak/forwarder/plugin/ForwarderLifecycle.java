
package com.paulmandal.atak.forwarder.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.interfaces.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotProtobufConverter;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.factories.CommHardwareFactory;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.persistence.JsonHelper;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.plugin.ui.GroupManagementMapComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class ForwarderLifecycle implements Lifecycle {
    private final static String TAG = "ATAKDBG." + ForwarderLifecycle.class.getSimpleName();

    private Context mPluginContext;
    private Activity mActivity;
    private MapView mMapView;
    private final Collection<MapComponent> mOverlays;

    private CommHardware mCommHardware;
    private InboundMessageHandler mInboundMessageHandler;
    private OutboundMessageHandler mOutboundMessageHandler;

    private GroupTracker mGroupTracker;

    public ForwarderLifecycle(Context context) {
        mPluginContext = context;
        mOverlays = new LinkedList<>();
    }

    @Override
    public void onCreate(final Activity activity, final transapps.mapi.MapView transappsMapView) {
        if (transappsMapView == null || !(transappsMapView.getView() instanceof MapView)) {
            Log.e(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        mActivity = activity;
        mMapView = (MapView)transappsMapView.getView();

        CotComparer cotComparer = new CotComparer();
        JsonHelper jsonHelper = new JsonHelper();
        StateStorage stateStorage = new StateStorage(mActivity, jsonHelper);
        CotMessageCache cotMessageCache = new CotMessageCache(stateStorage, cotComparer, stateStorage.getCachePurgeTimeMs());
        MessageQueue messageQueue = new MessageQueue(cotComparer);
        CotProtobufConverter cotProtobufConverter = new CotProtobufConverter();

        mGroupTracker = new GroupTracker(mActivity, stateStorage, stateStorage.getUsers(), stateStorage.getGroupInfo());
        mCommHardware = CommHardwareFactory.createAndInitCommHardware(mActivity, mMapView, mGroupTracker, mGroupTracker, messageQueue);
        mInboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler(mCommHardware, cotProtobufConverter);
        mOutboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler(messageQueue, cotMessageCache, cotProtobufConverter);

        mOverlays.add(new GroupManagementMapComponent(activity, mGroupTracker, mCommHardware, cotMessageCache, messageQueue));

        // create components
        Iterator<MapComponent> iter = mOverlays.iterator();
        MapComponent c;
        while (iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(mPluginContext, activity.getIntent(), mMapView);
            } catch (Exception e) {
                Log.w(TAG, "Unhandled exception trying to create overlays MapComponent", e);
                iter.remove();
            }
        }
    }

    @Override
    public void onDestroy() {
        mOutboundMessageHandler.destroy();
        mCommHardware.destroy();

        for (MapComponent c : mOverlays) {
            c.onDestroy(mPluginContext, mMapView);
        }
    }

    @Override
    public void onStart() {
        for (MapComponent c : mOverlays) {
            c.onStart(mPluginContext, mMapView);
        }
    }

    @Override
    public void onPause() {
        for (MapComponent c : mOverlays) {
            c.onPause(mPluginContext, mMapView);
        }
    }

    @Override
    public void onResume() {
        for (MapComponent c : mOverlays) {
            c.onResume(mPluginContext, mMapView);
        }
    }

    @Override
    public void onFinish() {}

    @Override
    public void onStop() {
        for (MapComponent c : mOverlays) {
            c.onStop(mPluginContext, mMapView);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        for (MapComponent c : mOverlays) {
            c.onConfigurationChanged(configuration);
        }
    }
}
