package com.paulmandal.atak.forwarder.preferences;

import com.google.gson.Gson;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.ChannelConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * These should match the values in preferences.xml
 */
public class PreferencesDefaults {
    public static final String DEFAULT_REGION = "0";
    public static final String DEFAULT_PLI_MAX_FREQUENCY = "30";
    public static final String DEFAULT_DROP_DUPLICATE_MSGS_TTL = "20";
    public static final String DEFAULT_PLI_HOP_LIMIT = "3";
    public static final String DEFAULT_CHAT_HOP_LIMIT = "3";
    public static final String DEFAULT_OTHER_HOP_LIMIT = "3";
    public static final boolean DEFAULT_COMM_DEVICE_IS_ROUTER = false;
    public static final List<ChannelConfig> DEFAULT_CHANNEL_CONFIGS = new ArrayList<ChannelConfig>() {{ add(new ChannelConfig(ForwarderConstants.DEFAULT_CHANNEL_NAME, ForwarderConstants.DEFAULT_CHANNEL_PSK, ForwarderConstants.DEFAULT_CHANNEL_MODE, true)); }};
    public static final String DEFAULT_CHANNEL_DATA = new Gson().toJson(DEFAULT_CHANNEL_CONFIGS, ArrayList.class);
    public static final String DEFAULT_TRACKER_PLI_INTERVAL = "60";
    public static final String DEFAULT_TRACKER_STALE_AFTER_SECS = "75";
    public static final String DEFAULT_TRACKER_SCREEN_OFF_TIME = "5";
    public static final boolean DEFAULT_TRACKER_IS_ROUTER = true;
    public static final String DEFAULT_TRACKER_TEAM = "0";
    public static final String DEFAULT_TRACKER_ROLE = "0";
    public static final String DEFAULT_COMM_DEVICE = null;
    public static final boolean DEFAULT_ENABLE_LOGGING = true;
    public static final boolean DEFAULT_ENABLE_LOGGING_TOASTS = false;
}
