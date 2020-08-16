package com.paulmandal.atak.forwarder.comm;

import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;

public class CotMessageCache {
    private StateStorage mStateStorage;
    private CotComparer mCotComparer;

    private List<CachedCotEvent> mCachedEvents;
    private int mCachePurgeTimeMs;

    public CotMessageCache(StateStorage stateStorage, CotComparer cotComparer, int cachePurgeTimeMs) {
        mStateStorage = stateStorage;
        mCotComparer = cotComparer;

        mCachedEvents = new ArrayList<>();
        mCachePurgeTimeMs = cachePurgeTimeMs;
    }

    public boolean checkIfRecentlySent(CotEvent cotEvent) {
        purgeCacheOfStaleEvents();

        for (CachedCotEvent cachedCotEvent : mCachedEvents) {
            if (mCotComparer.areCotEventsEqual(cotEvent, cachedCotEvent.cotEvent)) {
                return true;
            }
        }

        return false;
    }

    public void cacheEvent(CotEvent cotEvent) {
        mCachedEvents.add(new CachedCotEvent(cotEvent, System.currentTimeMillis()));
    }

    public void clearData() {
        mCachedEvents.clear();
    }

    public void setCachePurgeTimeMs(int cachePurgeTimeMs) {
        mCachePurgeTimeMs = cachePurgeTimeMs;
        mStateStorage.storeCachePurgeTime(cachePurgeTimeMs);
    }

    public int getCachePurgeTimeMs() {
        return mCachePurgeTimeMs;
    }

    private void purgeCacheOfStaleEvents() {
        long currentTime = System.currentTimeMillis();

        List<CachedCotEvent> purgeEvents = new ArrayList<>();

        for (CachedCotEvent cachedCotEvent : mCachedEvents) {
            if (currentTime - cachedCotEvent.lastSentTime > mCachePurgeTimeMs) {
                purgeEvents.add(cachedCotEvent);
            }
        }

        for (CachedCotEvent purgeableCotEvent : purgeEvents) {
            mCachedEvents.remove(purgeableCotEvent);
        }
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
