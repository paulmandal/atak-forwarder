package com.paulmandal.atak.forwarder.plugin.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.atakmap.android.preference.PluginPreferenceFragment;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.persistence.PreferencesKeys;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.ui.settings.AdvancedButtons;
import com.paulmandal.atak.forwarder.plugin.ui.settings.ChannelButtons;
import com.paulmandal.atak.forwarder.plugin.ui.settings.DevicesList;
import com.paulmandal.atak.forwarder.plugin.ui.settings.MainButtons;
import com.paulmandal.atak.forwarder.plugin.ui.settings.TrackerButtons;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.util.List;

public class ForwarderPreferencesFragment extends PluginPreferenceFragment {
    private static Context sPluginContext;
    private static List<Destroyable> sDestroyables;
    private static SharedPreferences sSharedPreferences;
    private static DevicesList sDevicesList;
    private static CommHardware sCommHardware;
    private static CotMessageCache sCotMessageCache;
    private static CommandQueue sCommandQueue;

    public ForwarderPreferencesFragment() {
        super(sPluginContext, R.xml.preferences);
    }

    @SuppressWarnings("ValidFragment")
    public ForwarderPreferencesFragment(final Context pluginContext,
                                        final List<Destroyable> destroyables,
                                        final SharedPreferences sharedPreferences,
                                        final DevicesList devicesList,
                                        final CommHardware commHardware,
                                        final CotMessageCache cotMessageCache,
                                        final CommandQueue commandQueue) {
        super(pluginContext, R.xml.preferences);
        this.sPluginContext = pluginContext;
        this.sDestroyables = destroyables;
        this.sSharedPreferences = sharedPreferences;
        this.sDevicesList = devicesList;
        this.sCommHardware = commHardware;
        this.sCotMessageCache = cotMessageCache;
        this.sCommandQueue = commandQueue;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainButtons mainButtons = new MainButtons(sDevicesList,
                findPreference(PreferencesKeys.KEY_SET_COMM_DEVICE),
                findPreference(PreferencesKeys.KEY_REFRESH_COMM_DEVICES));

        HashHelper hashHelper = new HashHelper();
        QrHelper qrHelper = new QrHelper();
        ChannelButtons channelButtons = new ChannelButtons(sDestroyables,
                sSharedPreferences,
                getActivity(),
                sPluginContext,
                sCommHardware,
                hashHelper,
                qrHelper,
                findPreference(PreferencesKeys.KEY_CHANNEL_MODE),
                findPreference(PreferencesKeys.KEY_CHANNEL_PSK),
                findPreference(PreferencesKeys.KEY_SHOW_CHANNEL_QR),
                findPreference(PreferencesKeys.KEY_SCAN_CHANNEL_QR),
                findPreference(PreferencesKeys.KEY_SAVE_CHANNEL_TO_FILE),
                findPreference(PreferencesKeys.KEY_SAVE_CHANNEL_TO_FILE));

        TrackerButtons trackerButtons = new TrackerButtons(sDevicesList,
                findPreference(PreferencesKeys.KEY_TRACKER_WRITE_TO_DEVICE));

        AdvancedButtons advancedButtons = new AdvancedButtons(sCotMessageCache,
                sCommandQueue,
                findPreference(PreferencesKeys.KEY_CLEAR_DUPLICATE_MSG_CACHE),
                findPreference(PreferencesKeys.KEY_CLEAR_OUTBOUND_MSG_QUEUE),
                findPreference(PreferencesKeys.KEY_RESET_TO_DEFAULT),
                findPreference(PreferencesKeys.KEY_RESET_TO_DEFAULT_INCLUDING_CHANNEL));
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", sPluginContext.getString(R.string.preferences_title));
    }
}
