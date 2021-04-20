package com.paulmandal.atak.forwarder.comm;

import android.content.SharedPreferences;

import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.preferences.PreferencesDefaults;
import com.paulmandal.atak.forwarder.preferences.PreferencesKeys;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.forwarder.plugin.DestroyableSharedPrefsListener;

import java.util.ArrayList;
import java.util.List;

public class CotMessageCache extends DestroyableSharedPrefsListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + CotMessageCache.class.getSimpleName();

    private final CotComparer mCotComparer;

    private final List<CachedCotEvent> mCachedEvents = new ArrayList<>();

    private int mDuplicateMessagesTtlMs;
    private int mPliMaxFrequencyMs;

    public CotMessageCache(List<Destroyable> destroyables,
                           SharedPreferences sharedPreferences,
                           CotComparer cotComparer) {
        super(destroyables,
                sharedPreferences,
                new String[]{
                        PreferencesKeys.KEY_PLI_MAX_FREQUENCY,
                        PreferencesKeys.KEY_DROP_DUPLICATE_MSGS_TTL
                },
                new String[]{});
        mCotComparer = cotComparer;
    }

    public boolean checkIfRecentlySent(CotEvent cotEvent) {
        purgeCacheOfStaleEvents();

        boolean isPli = MessageType.fromCotEventType(cotEvent.getType()) == MessageType.PLI;

        synchronized (mCachedEvents) {
            for (CachedCotEvent cachedCotEvent : mCachedEvents) {
                if (isPli && MessageType.fromCotEventType(cachedCotEvent.cotEvent.getType()) == MessageType.PLI) {
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
                int purgeTimeMs = mDuplicateMessagesTtlMs;
                if (MessageType.fromCotEventType(cachedCotEvent.cotEvent.getType()) == MessageType.PLI) {
                    purgeTimeMs = mPliMaxFrequencyMs;
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
        mPliMaxFrequencyMs = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_PLI_MAX_FREQUENCY, PreferencesDefaults.DEFAULT_PLI_MAX_FREQUENCY)) * 1000;
        mDuplicateMessagesTtlMs = Integer.parseInt(sharedPreferences.getString(PreferencesKeys.KEY_DROP_DUPLICATE_MSGS_TTL, PreferencesDefaults.DEFAULT_DROP_DUPLICATE_MSGS_TTL)) * 60 * 1000;
    }

    @Override
    protected void complexUpdate(SharedPreferences sharedPreferences, String key) {
        // Do nothing
    }

    private static class CachedCotEvent {
        public final CotEvent cotEvent;
        public final long lastSentTime;

        public CachedCotEvent(CotEvent cotEvent, long lastSentTime) {
            this.cotEvent = cotEvent;
            this.lastSentTime = lastSentTime;
        }
    }
}
