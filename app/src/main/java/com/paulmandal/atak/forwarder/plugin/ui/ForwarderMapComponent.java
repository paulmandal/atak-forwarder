package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.meshtastic.CommandQueueWorker;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshDeviceConfigurer;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshSender;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshServiceController;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshSuspendController;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.meshtastic.TrackerEventHandler;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.ChannelJsonHelper;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.StatusViewModel;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;
import com.paulmandal.atak.forwarder.tracker.TrackerCotGenerator;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinkerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.atakmap.android.util.ATAKConstants.getPackageName;

public class ForwarderMapComponent extends DropDownMapComponent {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + ForwarderMapComponent.class.getSimpleName();

    private Context mPluginContext;

    private final List<Destroyable> mDestroyables = new ArrayList<>();

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onCreate(final Context pluginContext, Intent intent,
                         final MapView mapView) {
        pluginContext.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(pluginContext, intent, mapView);

        mPluginContext = pluginContext;
        Context atakContext = mapView.getContext();

        List<Destroyable> destroyables = mDestroyables;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(atakContext);

        // Internal components
        Logger logger = new Logger(destroyables, sharedPreferences, atakContext);


        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        CotComparer cotComparer = new CotComparer();
        CommandQueue commandQueue = new CommandQueue(uiThreadHandler, cotComparer);


        MeshSuspendController meshSuspendController = new MeshSuspendController();
        MeshServiceController meshServiceController = new MeshServiceController(destroyables,
                sharedPreferences,
                atakContext,
                uiThreadHandler,
                meshSuspendController,
                logger);


        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();
        QueuedCommandFactory queuedCommandFactory = new QueuedCommandFactory();
        DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler = new DiscoveryBroadcastEventHandler(
                atakContext,
                logger,
                commandQueue,
                queuedCommandFactory,
                destroyables,
                meshSuspendController,
                meshServiceController,
                atakUid,
                callsign);


        TrackerEventHandler trackerEventHandler = new TrackerEventHandler(
                atakContext,
                logger,
                destroyables,
                meshSuspendController);


        MeshtasticDeviceSwitcher meshtasticDeviceSwitcher = new MeshtasticDeviceSwitcher(atakContext, logger);
        HashHelper hashHelper = new HashHelper();
        ChannelJsonHelper channelJsonHelper = new ChannelJsonHelper(new Gson());
        MeshDeviceConfigurer meshDeviceConfigurer = new MeshDeviceConfigurer(destroyables,
                sharedPreferences,
                meshServiceController,
                meshtasticDeviceSwitcher,
                hashHelper,
                channelJsonHelper,
                logger,
                callsign);


        UserTracker userTracker = new UserTracker(atakContext, uiThreadHandler, logger, discoveryBroadcastEventHandler, trackerEventHandler);


        ScheduledExecutorService meshSenderExecutor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread thread = new Thread(r);
            thread.setName("MeshSender.Watchdog");
            return thread;
        });
        MeshSender meshSender = new MeshSender(atakContext,
                destroyables,
                sharedPreferences,
                meshSuspendController,
                uiThreadHandler,
                logger,
                meshServiceController,
                userTracker,
                meshSenderExecutor);


        ScheduledExecutorService commandQueueExecutor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread thread = new Thread(r);
            thread.setName("CommandQueueWorker.Worker");
            return thread;
        });
        CommandQueueWorker commandQueueWorker = new CommandQueueWorker(destroyables, meshServiceController, commandQueue, meshSender, commandQueueExecutor);


        InboundMeshMessageHandler inboundMeshMessageHandler = new InboundMeshMessageHandler(
                atakContext,
                destroyables,
                meshSuspendController,
                uiThreadHandler,
                logger);


        CotShrinkerFactory cotShrinkerFactory = new CotShrinkerFactory();
        CotShrinker cotShrinker = cotShrinkerFactory.createCotShrinker();
        InboundMessageHandler inboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler(inboundMeshMessageHandler, cotShrinker, userTracker, logger);


        CotMessageCache cotMessageCache = new CotMessageCache(destroyables, sharedPreferences, cotComparer);
        OutboundMessageHandler outboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler(meshServiceController, commandQueue, queuedCommandFactory, cotMessageCache, cotShrinker, destroyables, logger);


        String pluginVersion = "0.0";
        try {
            PackageInfo pInfo = atakContext.getPackageManager().getPackageInfo(getPackageName(), 0);
            pluginVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TrackerCotGenerator trackerCotGenerator = new TrackerCotGenerator(destroyables, sharedPreferences, userTracker, inboundMessageHandler, logger, pluginVersion);


        StatusViewModel statusViewModel = new StatusViewModel(
                destroyables,
                sharedPreferences,
                userTracker,
                meshServiceController,
                discoveryBroadcastEventHandler,
                meshSender,
                inboundMeshMessageHandler,
                commandQueue,
                hashHelper,
                channelJsonHelper);


        ForwarderDropDownReceiver forwarderDropDownReceiver = new ForwarderDropDownReceiver(mapView,
                pluginContext,
                atakContext,
                statusViewModel);


        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(ForwarderDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(forwarderDropDownReceiver, ddFilter);


        new ForwarderMarkerIconWidget(mapView, destroyables, forwarderDropDownReceiver, meshServiceController, meshSender);


        DevicesList devicesList = new DevicesList(atakContext);
        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        pluginContext.getString(R.string.preferences_title),
                        pluginContext.getString(R.string.preferences_summary),
                        pluginContext.getString(R.string.key_atak_forwarder_preferences),
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher_maybe),
                        new ForwarderPreferencesFragment(
                                pluginContext,
                                destroyables,
                                sharedPreferences,
                                devicesList,
                                meshSuspendController,
                                discoveryBroadcastEventHandler,
                                cotMessageCache,
                                commandQueue,
                                logger
                        )));
    }

    @Override
    protected void onDestroyImpl(Context context, MapView mapView) {
        super.onDestroyImpl(context, mapView);
        for (Destroyable destroyable : mDestroyables) {
            destroyable.onDestroy(context, mapView);
        }
        ToolsPreferenceFragment.unregister(mPluginContext.getString(R.string.key_atak_forwarder_preferences));
    }
}
