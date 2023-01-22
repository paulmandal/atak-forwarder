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
import android.util.Base64;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.CommsLogger;
import com.atakmap.comms.CommsMapComponent;
import com.geeksville.mesh.ConfigProtos;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.meshtastic.CommandQueueWorker;
import com.paulmandal.atak.forwarder.comm.meshtastic.DeviceConfigObserver;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.DeviceConnectionHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshDeviceConfigurationController;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshDeviceConfiguratorFactory;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshSender;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDevice;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.meshtastic.ConnectionStateHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshServiceController;
import com.paulmandal.atak.forwarder.comm.meshtastic.TrackerEventHandler;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;
import com.paulmandal.atak.forwarder.plugin.ui.viewmodels.LoggingViewModel;
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
        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        Logger logger = new Logger(destroyables, sharedPreferences, uiThreadHandler);


        CotComparer cotComparer = new CotComparer();
        CommandQueue commandQueue = new CommandQueue(uiThreadHandler, cotComparer);


        Gson gson = new Gson();

        String callsign = mapView.getDeviceCallsign();
        String atakUid = mapView.getSelfMarker().getUID();
        QueuedCommandFactory queuedCommandFactory = new QueuedCommandFactory();
        MeshServiceController meshServiceController = new MeshServiceController(destroyables,
                atakContext,
                uiThreadHandler,
                logger);

        MeshtasticDeviceSwitcher meshtasticDeviceSwitcher = new MeshtasticDeviceSwitcher(atakContext, logger);
        HashHelper hashHelper = new HashHelper();
        DeviceConnectionHandler deviceConnectionHandler = new DeviceConnectionHandler(
                atakContext,
                destroyables,
                meshServiceController,
                logger);


        MeshDeviceConfiguratorFactory meshDeviceConfiguratorFactory = new MeshDeviceConfiguratorFactory();
        DeviceConfigObserver deviceConfigObserver = new DeviceConfigObserver(
                destroyables,
                sharedPreferences,
                logger,
                gson
        );
        String commDeviceStr = sharedPreferences.getString(PreferencesKeys.KEY_SET_COMM_DEVICE, PreferencesDefaults.DEFAULT_COMM_DEVICE);
        MeshtasticDevice meshtasticDevice = gson.fromJson(commDeviceStr, MeshtasticDevice.class);
        ConfigProtos.Config.LoRaConfig.RegionCode regionCode = ConfigProtos.Config.LoRaConfig.RegionCode.forNumber(Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_REGION, PreferencesDefaults.DEFAULT_REGION)));
        String channelName = sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_NAME, PreferencesDefaults.DEFAULT_CHANNEL_NAME);
        int channelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
        byte[] channelPsk = Base64.decode(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_PSK, PreferencesDefaults.DEFAULT_CHANNEL_PSK), Base64.DEFAULT);
        boolean isRouter = sharedPreferences.getBoolean(PreferencesKeys.KEY_COMM_DEVICE_IS_ROUTER, PreferencesDefaults.DEFAULT_COMM_DEVICE_IS_ROUTER);
        ConfigProtos.Config.LoRaConfig.ModemPreset modemConfig = ConfigProtos.Config.LoRaConfig.ModemPreset.forNumber(channelMode);
        ConfigProtos.Config.DeviceConfig.Role routingRole = isRouter ? ConfigProtos.Config.DeviceConfig.Role.ROUTER_CLIENT : ConfigProtos.Config.DeviceConfig.Role.CLIENT;
        boolean pluginManagesDevice = sharedPreferences.getBoolean(PreferencesKeys.KEY_PLUGIN_MANAGES_DEVICE, PreferencesDefaults.DEFAULT_PLUGIN_MANAGES_DEVICE);

        MeshDeviceConfigurationController meshDeviceConfigurationController = new MeshDeviceConfigurationController(
                meshServiceController,
                deviceConnectionHandler,
                meshtasticDeviceSwitcher,
                meshDeviceConfiguratorFactory,
                deviceConfigObserver,
                hashHelper,
                logger,
                meshtasticDevice,
                regionCode,
                channelName,
                channelMode,
                channelPsk,
                routingRole,
                pluginManagesDevice,
                callsign
        );


        ConnectionStateHandler connectionStateHandler = new ConnectionStateHandler(
                logger,
                meshDeviceConfigurationController,
                meshServiceController,
                deviceConnectionHandler,
                deviceConfigObserver,
                meshtasticDevice,
                regionCode,
                pluginManagesDevice
        );


        DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler = new DiscoveryBroadcastEventHandler(
                atakContext,
                logger,
                commandQueue,
                queuedCommandFactory,
                destroyables,
                connectionStateHandler,
                meshServiceController,
                atakUid,
                callsign
        );


        TrackerEventHandler trackerEventHandler = new TrackerEventHandler(
                atakContext,
                logger,
                destroyables,
                uiThreadHandler,
                connectionStateHandler
        );



        UserTracker userTracker = new UserTracker(
                atakContext,
                uiThreadHandler,
                logger,
                discoveryBroadcastEventHandler,
                trackerEventHandler
        );


        ScheduledExecutorService meshSenderExecutor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread thread = new Thread(r);
            thread.setName("MeshSender.Watchdog");
            return thread;
        });
        MeshSender meshSender = new MeshSender(atakContext,
                destroyables,
                sharedPreferences,
                uiThreadHandler,
                logger,
                connectionStateHandler,
                meshServiceController,
                userTracker,
                meshSenderExecutor);


        ScheduledExecutorService commandQueueExecutor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread thread = new Thread(r);
            thread.setName("CommandQueueWorker.Worker");
            return thread;
        });
        CommandQueueWorker commandQueueWorker = new CommandQueueWorker(
                destroyables,
                connectionStateHandler,
                commandQueue,
                meshSender,
                commandQueueExecutor
        );


        InboundMeshMessageHandler inboundMeshMessageHandler = new InboundMeshMessageHandler(
                atakContext,
                destroyables,
                connectionStateHandler,
                uiThreadHandler,
                logger);


        CotShrinkerFactory cotShrinkerFactory = new CotShrinkerFactory();
        CotShrinker cotShrinker = cotShrinkerFactory.createCotShrinker();
        InboundMessageHandler inboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler(inboundMeshMessageHandler, cotShrinker, userTracker, logger);


        CommsMapComponent commsMapComponent  = CommsMapComponent.getInstance();
        CotMessageCache cotMessageCache = new CotMessageCache(destroyables, sharedPreferences, cotComparer);
        CommsLogger outboundMessageHandler = new OutboundMessageHandler(
                uiThreadHandler,
                commsMapComponent,
                connectionStateHandler,
                commandQueue,
                queuedCommandFactory,
                cotMessageCache,
                cotShrinker,
                logger
        );


        String pluginVersion = "0.0";
        try {
            PackageInfo pInfo = atakContext.getPackageManager().getPackageInfo(getPackageName(), 0);
            pluginVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TrackerCotGenerator trackerCotGenerator = new TrackerCotGenerator(destroyables, sharedPreferences, userTracker, inboundMessageHandler, logger, pluginVersion);


        StatusViewModel statusViewModel = new StatusViewModel(
                deviceConfigObserver,
                hashHelper,
                channelName,
                channelPsk,
                modemConfig,
                meshtasticDevice,
                pluginManagesDevice,
                userTracker,
                connectionStateHandler,
                discoveryBroadcastEventHandler,
                meshSender,
                inboundMeshMessageHandler,
                trackerEventHandler,
                commandQueue);

        LoggingViewModel loggingViewModel = new LoggingViewModel(destroyables, sharedPreferences, logger);


        ForwarderDropDownReceiver forwarderDropDownReceiver = new ForwarderDropDownReceiver(mapView,
                pluginContext,
                atakContext,
                statusViewModel,
                loggingViewModel);


        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(ForwarderDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(forwarderDropDownReceiver, ddFilter);


        new ForwarderMarkerIconWidget(mapView, destroyables, forwarderDropDownReceiver, connectionStateHandler, meshSender);


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
                                meshDeviceConfigurationController,
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
