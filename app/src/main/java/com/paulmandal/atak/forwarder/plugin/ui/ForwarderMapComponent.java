package com.paulmandal.atak.forwarder.plugin.ui;

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
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticDeviceSwitcher;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.factories.CommHardwareFactory;
import com.paulmandal.atak.forwarder.factories.MessageHandlerFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.nonatak.NonAtakStationCotGenerator;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.DevicesTabViewModel;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels.StatusTabViewModel;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinkerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.atakmap.android.util.ATAKConstants.getPackageName;

public class ForwarderMapComponent extends DropDownMapComponent {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ForwarderMapComponent.class.getSimpleName();

    private Context mPluginContext;

    private List<Destroyable> mDestroyables = new ArrayList<>();

    public void onCreate(final Context pluginContext, Intent intent,
                         final MapView mapView) {
        pluginContext.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(pluginContext, intent, mapView);

        mPluginContext = pluginContext;
        Context atakContext = mapView.getContext();

        // TODO: this is kinda a mess, move to a Factory and clean this up (or use Dagger 2)

        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        CotComparer cotComparer = new CotComparer();
        CommandQueue commandQueue = new CommandQueue(uiThreadHandler, cotComparer);
        QueuedCommandFactory queuedCommandFactory = new QueuedCommandFactory();
        CotShrinkerFactory cotShrinkerFactory = new CotShrinkerFactory();
        CotShrinker cotShrinker = cotShrinkerFactory.createCotShrinker();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(atakContext);

        MeshtasticDeviceSwitcher meshtasticDeviceSwitcher = new MeshtasticDeviceSwitcher(atakContext);
        UserTracker userTracker = new UserTracker(atakContext, uiThreadHandler);
        MeshtasticCommHardware meshtasticCommHardware = CommHardwareFactory.createAndInitMeshtasticCommHardware(atakContext, mapView, uiThreadHandler, meshtasticDeviceSwitcher, userTracker, userTracker, commandQueue, queuedCommandFactory, sharedPreferences, mDestroyables);
        InboundMessageHandler inboundMessageHandler = MessageHandlerFactory.getInboundMessageHandler(meshtasticCommHardware, cotShrinker);
        CotMessageCache cotMessageCache = new CotMessageCache(mDestroyables, sharedPreferences, cotComparer);
        OutboundMessageHandler outboundMessageHandler = MessageHandlerFactory.getOutboundMessageHandler(meshtasticCommHardware, commandQueue, queuedCommandFactory, cotMessageCache, cotShrinker, mDestroyables);

        String pluginVersion = "0.0";
        try {
            PackageInfo pInfo = atakContext.getPackageManager().getPackageInfo(getPackageName(), 0);
            pluginVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        NonAtakStationCotGenerator nonAtakStationCotGenerator = new NonAtakStationCotGenerator(userTracker, inboundMessageHandler, pluginVersion, mapView.getDeviceCallsign());

        HashHelper hashHelper = new HashHelper();
        StatusTabViewModel statusTabViewModel = new StatusTabViewModel(userTracker, meshtasticCommHardware, commandQueue, hashHelper);
        DevicesTabViewModel devicesTabViewModel = new DevicesTabViewModel(atakContext, uiThreadHandler, meshtasticDeviceSwitcher, meshtasticCommHardware, hashHelper);

        DevicesList devicesList = new DevicesList(atakContext);

        ForwarderDropDownReceiver forwarderDropDownReceiver = new ForwarderDropDownReceiver(mapView,
                pluginContext,
                atakContext,
                statusTabViewModel,
                devicesTabViewModel);

        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(ForwarderDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(forwarderDropDownReceiver, ddFilter);

        new ForwarderMarkerIconWidget(mapView, mDestroyables, forwarderDropDownReceiver, meshtasticCommHardware);

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        pluginContext.getString(R.string.preferences_title),
                        pluginContext.getString(R.string.preferences_summary),
                        pluginContext.getString(R.string.key_atak_forwarder_preferences),
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher_maybe),
                        new ForwarderPreferencesFragment(
                                pluginContext,
                                mDestroyables,
                                sharedPreferences,
                                devicesList,
                                meshtasticCommHardware,
                                cotMessageCache,
                                commandQueue
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
