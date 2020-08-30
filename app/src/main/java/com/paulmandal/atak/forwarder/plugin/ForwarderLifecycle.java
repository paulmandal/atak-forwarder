
package com.paulmandal.atak.forwarder.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.paulmandal.atak.forwarder.HackyTests;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverterFactory;
import com.paulmandal.atak.forwarder.comm.protobuf.fallback.FallbackCotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.factories.CommHardwareFactory;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.group.persistence.JsonHelper;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.plugin.ui.GroupManagementMapComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

public class ForwarderLifecycle implements Lifecycle {
    private final static String TAG = "ATAKDBG." + ForwarderLifecycle.class.getSimpleName();

    private Context mPluginContext;
    private MapView mMapView;
    private final Collection<MapComponent> mOverlays;

    private CommHardware mCommHardware;
    private OutboundMessageHandler mOutboundMessageHandler;

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
        mMapView = (MapView)transappsMapView.getView();

        HackyTests hackyTests = new HackyTests();
        hackyTests.runAllTests();

        // TODO: this is kinda a mess, move to a Factory and clean this up


        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        CotComparer cotComparer = new CotComparer();
        JsonHelper jsonHelper = new JsonHelper();
        StateStorage stateStorage = new StateStorage(activity, jsonHelper);
        CotMessageCache cotMessageCache = new CotMessageCache(stateStorage, cotComparer, stateStorage.getDefaultCachePurgeTimeMs(), stateStorage.getPliCachePurgeTimeMs());
        CommandQueue commandQueue = new CommandQueue(uiThreadHandler, cotComparer);
        QueuedCommandFactory queuedCommandFactory = new QueuedCommandFactory();
        CotEventProtobufConverter cotEventProtobufConverter = CotEventProtobufConverterFactory.createCotEventProtobufConverter();
        FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter = new FallbackCotEventProtobufConverter();

        GroupTracker groupTracker = new GroupTracker(activity, uiThreadHandler, stateStorage, stateStorage.getUsers(), stateStorage.getGroupInfo());
        mCommHardware = CommHardwareFactory.createAndInitCommHardware(activity, mMapView, uiThreadHandler, groupTracker, groupTracker, commandQueue, queuedCommandFactory);
        MessageHandlerFactory.getInboundMessageHandler(mCommHardware, cotEventProtobufConverter, fallbackCotEventProtobufConverter);
        mOutboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler(mCommHardware, commandQueue, queuedCommandFactory, cotMessageCache, cotEventProtobufConverter, fallbackCotEventProtobufConverter);

        mOverlays.add(new GroupManagementMapComponent(groupTracker, mCommHardware, cotMessageCache, commandQueue));

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
