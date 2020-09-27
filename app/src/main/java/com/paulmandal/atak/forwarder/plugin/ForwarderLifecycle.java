package com.paulmandal.atak.forwarder.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;

import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.paulmandal.atak.forwarder.BuildConfig;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.HackyTests;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.persistence.StateStorage;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverterFactory;
import com.paulmandal.atak.forwarder.comm.protobuf.fallback.FallbackCotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.factories.CommHardwareFactory;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.nonatak.NonAtakStationCotGenerator;
import com.paulmandal.atak.forwarder.plugin.ui.GroupManagementMapComponent;
import com.paulmandal.atak.forwarder.plugin.ui.QrHelper;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.ChannelTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusTabViewModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import transapps.maps.plugin.lifecycle.Lifecycle;

import static com.atakmap.android.util.ATAKConstants.getPackageName;

public class ForwarderLifecycle implements Lifecycle {
    private final static String TAG = Config.DEBUG_TAG_PREFIX + ForwarderLifecycle.class.getSimpleName();

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

        if (BuildConfig.DEBUG) {
            HackyTests hackyTests = new HackyTests();
            hackyTests.runAllTests();
        }

        // TODO: this is kinda a mess, move to a Factory and clean this up (or use Dagger 2)

        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        CotComparer cotComparer = new CotComparer();
        StateStorage stateStorage = new StateStorage(activity);
        CommandQueue commandQueue = new CommandQueue(uiThreadHandler, cotComparer);
        QueuedCommandFactory queuedCommandFactory = new QueuedCommandFactory();
        CotEventProtobufConverter cotEventProtobufConverter = CotEventProtobufConverterFactory.createCotEventProtobufConverter();
        FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter = new FallbackCotEventProtobufConverter();

        ChannelTracker channelTracker = new ChannelTracker(activity, uiThreadHandler);
        mCommHardware = CommHardwareFactory.createAndInitCommHardware(activity, mMapView, uiThreadHandler, channelTracker, channelTracker, commandQueue, queuedCommandFactory, stateStorage);
        InboundMessageHandler inboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler(mCommHardware, cotEventProtobufConverter, fallbackCotEventProtobufConverter);
        // TODO: clean up ugly unchecked cast to MeshstaticCommHardware
        CotMessageCache cotMessageCache = new CotMessageCache(stateStorage, cotComparer, (MeshtasticCommHardware) mCommHardware, stateStorage.getDefaultCachePurgeTimeMs(), stateStorage.getPliCachePurgeTimeMs());
        mOutboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler(mCommHardware, commandQueue, queuedCommandFactory, cotMessageCache, cotEventProtobufConverter, fallbackCotEventProtobufConverter);

        String pluginVersion = "0.0";
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(getPackageName(), 0);
            pluginVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        NonAtakStationCotGenerator nonAtakStationCotGenerator = new NonAtakStationCotGenerator(channelTracker, inboundMessageHandler, pluginVersion, mMapView.getDeviceCallsign());

        Context atakContext = mMapView.getContext();

        HashHelper hashHelper = new HashHelper();
        // TODO: clean up ugly unchecked casts to MeshstaticCommHardware
        MeshtasticCommHardware meshtasticCommHardware = (MeshtasticCommHardware) mCommHardware;
        StatusTabViewModel statusTabViewModel = new StatusTabViewModel(channelTracker, meshtasticCommHardware, commandQueue, hashHelper);
        ChannelTabViewModel channelTabViewModel = new ChannelTabViewModel(mPluginContext, atakContext, meshtasticCommHardware, channelTracker, new QrHelper(), hashHelper);
        DevicesTabViewModel devicesTabViewModel = new DevicesTabViewModel(activity, uiThreadHandler, atakContext, meshtasticCommHardware, hashHelper);

        mOverlays.add(new GroupManagementMapComponent(channelTracker, mCommHardware, cotMessageCache, commandQueue, statusTabViewModel, channelTabViewModel, devicesTabViewModel));

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
