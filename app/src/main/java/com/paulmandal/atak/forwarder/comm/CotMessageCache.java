package com.paulmandal.atak.forwarder.comm;

import android.content.SharedPreferences;

import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.cotutils.CotMessageTypes;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;

import java.util.ArrayList;
import java.util.List;

public class CotMessageCache extends DestroyableSharedPrefsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + CotMessageCache.class.getSimpleName();

    private CotComparer mCotComparer;

    private final List<CachedCotEvent> mCachedEvents = new ArrayList<>();

    private int mPliMaxFrequencyS;
    private int mDuplicateMessagesTtlM;
    private int mChannelMode;
    private int mModeAwarePliRate;

    public CotMessageCache(List<Destroyable> destroyables,
                           SharedPreferences sharedPreferences,
                           CotComparer cotComparer) {
        super(destroyables,
                sharedPreferences,
                new String[]{
                        PreferencesKeys.KEY_PLI_MAX_FREQUENCY,
                        PreferencesKeys.KEY_DROP_DUPLICATE_MSGS_TTL,
                        PreferencesKeys.KEY_CHANNEL_MODE
                },
                new String[]{});
        mCotComparer = cotComparer;
    }

    public boolean checkIfRecentlySent(CotEvent cotEvent) {
        purgeCacheOfStaleEvents();

        boolean isPli = cotEvent.getType().equals(CotMessageTypes.TYPE_PLI);

        synchronized (mCachedEvents) {
            for (CachedCotEvent cachedCotEvent : mCachedEvents) {
                if (isPli && cachedCotEvent.cotEvent.getType().equals(CotMessageTypes.TYPE_PLI)) {
                    // Don't compare PLIs
                    return true;
                } else if (mCotComparer.areCotEventsEqual(cotEvent, cachedCotEvent.cotEvent)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void cacheEvent(CotEvent cotEvent) {
        synchronized (mCachedEvents) {
            mCachedEvents.add(new CachedCotEvent(cotEvent, System.currentTimeMillis()));
        }
    }

    public void clearData() {
        synchronized (mCachedEvents) {
            mCachedEvents.clear();
        }
    }

    private void purgeCacheOfStaleEvents() {
        long currentTime = System.currentTimeMillis();

        List<CachedCotEvent> purgeEvents = new ArrayList<>();

        synchronized (mCachedEvents) {

            for (CachedCotEvent cachedCotEvent : mCachedEvents) {
                int purgeTimeMs = mDuplicateMessagesTtlM * 60 * 1000;
                if (cachedCotEvent.cotEvent.getType().equals(CotMessageTypes.TYPE_PLI)) {
                    purgeTimeMs = mModeAwarePliRate;
                }
                if (currentTime - cachedCotEvent.lastSentTime > purgeTimeMs) {
                    purgeEvents.add(cachedCotEvent);
                }
            }

            for (CachedCotEvent purgeableCotEvent : purgeEvents) {
                mCachedEvents.remove(purgeableCotEvent);
            }
        }
    }

    @Override
    protected void updateSettings(SharedPreferences sharedPreferences) {
        mPliMaxFrequencyS = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_PLI_MAX_FREQUENCY, PreferencesDefaults.DEFAULT_PLI_MAX_FREQUENCY));
        mDuplicateMessagesTtlM = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_DROP_DUPLICATE_MSGS_TTL, PreferencesDefaults.DEFAULT_DROP_DUPLICATE_MSGS_TTL));
        mChannelMode = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_CHANNEL_MODE, PreferencesDefaults.DEFAULT_CHANNEL_MODE));
        mModeAwarePliRate = (mPliMaxFrequencyS * 1000) * (mChannelMode + 1);
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // Do nothing
    }

    private static class CachedCotEvent {
        public CotEvent cotEvent;
        public long lastSentTime;

        public CachedCotEvent(CotEvent cotEvent, long lastSentTime) {
            this.cotEvent = cotEvent;
            this.lastSentTime = lastSentTime;
        }
    }
}
