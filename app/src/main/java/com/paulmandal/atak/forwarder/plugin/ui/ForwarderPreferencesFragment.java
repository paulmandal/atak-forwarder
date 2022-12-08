package com.paulmandal.atak.forwarder.plugin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshDeviceConfigurationController;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.helpers.PskHelper;
import com.paulmandal.atak.forwarder.helpers.QrHelper;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.ui.settings.AdvancedButtons;
import com.paulmandal.atak.forwarder.plugin.ui.settings.ChannelButtons;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;
import com.paulmandal.atak.forwarder.plugin.ui.settings.MainButtons;
import com.paulmandal.atak.forwarder.plugin.ui.settings.TrackerButtons;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;

import java.util.List;

@SuppressWarnings("AccessStaticViaInstance")
public class ForwarderPreferencesFragment extends PluginPreferenceFragment implements Destroyable {
    @SuppressLint("StaticFieldLeak")
    private static Context sPluginContext;
    private static List<Destroyable> sDestroyables;
    private static SharedPreferences sSharedPreferences;
    @SuppressLint("StaticFieldLeak")
    private static DevicesList sDevicesList;
    private static MeshDeviceConfigurationController sMeshDeviceConfigurationController;
    @SuppressLint("StaticFieldLeak")
    private static DiscoveryBroadcastEventHandler sDiscoveryBroadcastEventHandler;
    private static CotMessageCache sCotMessageCache;
    private static CommandQueue sCommandQueue;
    @SuppressLint("StaticFieldLeak")
    private static Logger sLogger;

    public ForwarderPreferencesFragment() {
        super(sPluginContext, R.xml.preferences);
    }

    @SuppressWarnings("ValidFragment")
    public ForwarderPreferencesFragment(final Context pluginContext,
                                        final List<Destroyable> destroyables,
                                        final SharedPreferences sharedPreferences,
                                        final DevicesList devicesList,
                                        final MeshDeviceConfigurationController meshDeviceConfigurationController,
                                        final DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler,
                                        final CotMessageCache cotMessageCache,
                                        final CommandQueue commandQueue,
                                        final Logger logger) {
        super(pluginContext, R.xml.preferences);
        this.sPluginContext = pluginContext;
        this.sDestroyables = destroyables;
        this.sSharedPreferences = sharedPreferences;
        this.sDevicesList = devicesList;
        this.sMeshDeviceConfigurationController = meshDeviceConfigurationController;
        this.sDiscoveryBroadcastEventHandler = discoveryBroadcastEventHandler;
        this.sCotMessageCache = cotMessageCache;
        this.sCommandQueue = commandQueue;
        this.sLogger = logger;

        destroyables.add(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Gson gson = new Gson();

        MainButtons mainButtons = new MainButtons(sDevicesList,
                findPreference(PreferencesKeys.KEY_SET_COMM_DEVICE),
                findPreference(PreferencesKeys.KEY_REFRESH_COMM_DEVICES),
                findPreference(PreferencesKeys.KEY_REGION),
                gson);

        Context settingsMenuContext = getActivity();

        HashHelper hashHelper = new HashHelper();
        PskHelper pskHelper = new PskHelper();
        QrHelper qrHelper = new QrHelper();
        ChannelButtons channelButtons = new ChannelButtons(sDestroyables,
                sSharedPreferences,
                settingsMenuContext,
                sPluginContext,
                sDiscoveryBroadcastEventHandler,
                hashHelper,
                pskHelper,
                qrHelper,
                sLogger,
                findPreference(PreferencesKeys.KEY_CHANNEL_NAME),
                findPreference(PreferencesKeys.KEY_CHANNEL_MODE),
                findPreference(PreferencesKeys.KEY_CHANNEL_PSK),
                findPreference(PreferencesKeys.KEY_SHOW_CHANNEL_QR),
                findPreference(PreferencesKeys.KEY_SCAN_CHANNEL_QR),
                findPreference(PreferencesKeys.KEY_SAVE_CHANNEL_TO_FILE),
                findPreference(PreferencesKeys.KEY_SAVE_CHANNEL_TO_FILE));

        Handler uiThreadHandler = new Handler(Looper.getMainLooper());
        TrackerButtons trackerButtons = new TrackerButtons(settingsMenuContext,
                pluginContext,
                sDevicesList,
                sMeshDeviceConfigurationController,
                gson,
                findPreference(PreferencesKeys.KEY_TRACKER_TEAM),
                findPreference(PreferencesKeys.KEY_TRACKER_ROLE),
                findPreference(PreferencesKeys.KEY_TRACKER_WRITE_TO_DEVICE));

        AdvancedButtons advancedButtons = new AdvancedButtons(sCotMessageCache,
                sCommandQueue,
                findPreference(PreferencesKeys.KEY_CLEAR_DUPLICATE_MSG_CACHE),
                findPreference(PreferencesKeys.KEY_CLEAR_OUTBOUND_MSG_QUEUE),
                findPreference(PreferencesKeys.KEY_SET_LOGGING_LEVEL),
                findPreference(PreferencesKeys.KEY_RESET_TO_DEFAULT),
                findPreference(PreferencesKeys.KEY_RESET_TO_DEFAULT_INCLUDING_CHANNEL));
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", sPluginContext.getString(R.string.preferences_title));
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        this.sPluginContext = null;
        this.sDestroyables = null;
        this.sSharedPreferences = null;
        this.sDevicesList = null;
        this.sMeshDeviceConfigurationController = null;
        this.sDiscoveryBroadcastEventHandler = null;
        this.sCotMessageCache = null;
        this.sCommandQueue = null;
        this.sLogger = null;
    }
}
