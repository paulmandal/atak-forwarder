package com.paulmandal.atak.forwarder.comm;

import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.cotutils.CotMessageTypes;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;

public class CotMessageCache {
    private StateStorage mStateStorage;
    private CotComparer mCotComparer;

    private List<CachedCotEvent> mCachedEvents;
    private int mPliCachePurgeTimeMs;
    private int mDefaultCachePurgeTimeMs;

    public CotMessageCache(StateStorage stateStorage, CotComparer cotComparer, int defaultCachePurgeTimeMs, int pliCachePurgeTimeMs) {
        mStateStorage = stateStorage;
        mCotComparer = cotComparer;

        mCachedEvents = new ArrayList<>();
        mDefaultCachePurgeTimeMs = defaultCachePurgeTimeMs;
        mPliCachePurgeTimeMs = pliCachePurgeTimeMs;
    }

    public boolean checkIfRecentlySent(CotEvent cotEvent) {
        purgeCacheOfStaleEvents();

        boolean isPli = cotEvent.getType().equals(CotMessageTypes.TYPE_PLI);

        for (CachedCotEvent cachedCotEvent : mCachedEvents) {
            if (isPli && cachedCotEvent.cotEvent.getType().equals(CotMessageTypes.TYPE_PLI)) {
                // Don't compare PLIs
                return true;
            } else if (mCotComparer.areCotEventsEqual(cotEvent, cachedCotEvent.cotEvent)) {
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

    public void setDefaultCachePurgeTimeMs(int defaultCachePurgeTimeMs) {
        mDefaultCachePurgeTimeMs = defaultCachePurgeTimeMs;
        mStateStorage.storeDefaultCachePurgeTime(defaultCachePurgeTimeMs);
    }

    public void setPliCachePurgeTimeMs(int pliCachePurgeTimeMs) {
        mPliCachePurgeTimeMs = pliCachePurgeTimeMs;
        mStateStorage.storePliCachePurgeTime(pliCachePurgeTimeMs);
    }

    public int getDefaultCachePurgeTimeMs() {
        return mDefaultCachePurgeTimeMs;
    }

    private void purgeCacheOfStaleEvents() {
        long currentTime = System.currentTimeMillis();

        List<CachedCotEvent> purgeEvents = new ArrayList<>();

        for (CachedCotEvent cachedCotEvent : mCachedEvents) {
            int purgeTime = mDefaultCachePurgeTimeMs;
            if (cachedCotEvent.cotEvent.getType().equals(CotMessageTypes.TYPE_PLI)) {
                purgeTime = mPliCachePurgeTimeMs;
            }
            if (currentTime - cachedCotEvent.lastSentTime > purgeTime) {
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
