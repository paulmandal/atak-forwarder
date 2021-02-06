package com.paulmandal.atak.forwarder.persistence;

import android.content.SharedPreferences;

import com.paulmandal.atak.forwarder.Config;

import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_CHANNEL_NAME;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_CHANNEL_PSK;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_CHANNEL_SPEED;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_CHAT_HOP_LIMIT;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_DROP_DUPLICATE_MSGS_TTL;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_OTHER_HOP_LIMIT;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_PLI_HOP_LIMIT;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_PLI_MAX_FREQUENCY;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_TRACKER_PLI_INTERVAL;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_TRACKER_ROLE;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_TRACKER_SCREEN_OFF_TIME;
import static com.paulmandal.atak.forwarder.persistence.PreferencesKeys.KEY_TRACKER_TEAM;

public class PreferencesWatcher implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences mSharedPreferences;
    private ConfigState mConfigState;

    public PreferencesWatcher(SharedPreferences sharedPreferences, ConfigState configState) {
        mSharedPreferences = sharedPreferences;
        mConfigState = configState;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_PLI_MAX_FREQUENCY:
                mConfigState.pliMaxFrequencyS = sharedPreferences.getInt(key, mConfigState.defaultPliMaxFrequencyS);
                break;
            case KEY_DROP_DUPLICATE_MSGS_TTL:
                mConfigState.duplicateMessagesTtlM = sharedPreferences.getInt(key, mConfigState.defaultDuplicateMessagesTtlM);
                break;
            case KEY_PLI_HOP_LIMIT:
                mConfigState.pliHopLimit = sharedPreferences.getInt(key, mConfigState.defaultPliHopLimit);
                break;
            case KEY_CHAT_HOP_LIMIT:
                mConfigState.chatHopLimit = sharedPreferences.getInt(key, mConfigState.defaultChatHopLimit);
                break;
            case KEY_OTHER_HOP_LIMIT:
                mConfigState.otherHopLimit = sharedPreferences.getInt(key, mConfigState.defaultOtherHopLimit);
                break;
            case KEY_CHANNEL_NAME:
                mConfigState.channelName = sharedPreferences.getString(key, mConfigState.defaultChannelName);
                break;
            case KEY_CHANNEL_SPEED:
                mConfigState.channelMode = sharedPreferences.getInt(key, mConfigState.defaultChannelMode);
                break;
            case KEY_CHANNEL_PSK:
                mConfigState.channelPsk = new byte[Config.PSK_LENGTH];// TODO: sharedPreferences.getString(key, mConfigState.defaultChannelPskStr);
                break;
            case KEY_TRACKER_PLI_INTERVAL:
                mConfigState.trackerPliIntervalS = sharedPreferences.getInt(key, mConfigState.defaultTrackerPliIntervalS);
                break;
            case KEY_TRACKER_SCREEN_OFF_TIME:
                mConfigState.trackerScreenOffTimeS = sharedPreferences.getInt(key, mConfigState.defaultTrackerScreenOffTimeS);
                break;
            case KEY_TRACKER_TEAM:
                mConfigState.trackerTeamIndex = sharedPreferences.getInt(key, mConfigState.defaultTrackerTeamIndex);
                break;
            case KEY_TRACKER_ROLE:
                mConfigState.trackerRoleIndex = sharedPreferences.getInt(key, mConfigState.defaultTrackerRoleIndex);
                break;
        }
    }
}
