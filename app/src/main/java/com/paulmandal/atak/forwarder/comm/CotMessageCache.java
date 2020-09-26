package com.paulmandal.atak.forwarder.comm;

import com.atakmap.coremap.cot.event.CotEvent;
import com.geeksville.mesh.MeshProtos;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;
import com.paulmandal.atak.forwarder.cotutils.CotMessageTypes;
import com.paulmandal.atak.forwarder.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;

public class CotMessageCache implements MeshtasticCommHardware.ChannelSettingsListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + CotMessageCache.class.getSimpleName();

    private StateStorage mStateStorage;
    private CotComparer mCotComparer;

    private final List<CachedCotEvent> mCachedEvents = new ArrayList<>();
    private int mPliCachePurgeTimeMs;
    private int mDefaultCachePurgeTimeMs;
    private int mDataRateAwarePliCachePurgeTimeMs;

    public CotMessageCache(StateStorage stateStorage,
                           CotComparer cotComparer,
                           MeshtasticCommHardware commHardware,
                           int defaultCachePurgeTimeMs,
                           int pliCachePurgeTimeMs) {
        mStateStorage = stateStorage;
        mCotComparer = cotComparer;

        mDefaultCachePurgeTimeMs = defaultCachePurgeTimeMs;
        mPliCachePurgeTimeMs = pliCachePurgeTimeMs;
        mDataRateAwarePliCachePurgeTimeMs = pliCachePurgeTimeMs;

        commHardware.addChannelSettingsListener(this);
    }

    @Override
    public void onChannelSettingsUpdated(String channelName, byte[] psk, MeshProtos.ChannelSettings.ModemConfig modemConfig) {
        mDataRateAwarePliCachePurgeTimeMs = mPliCachePurgeTimeMs * (modemConfig.getNumber() + 1);
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

    public int getPliCachePurgeTimeMs() {
        return mPliCachePurgeTimeMs;
    }

    private void purgeCacheOfStaleEvents() {
        long currentTime = System.currentTimeMillis();

        List<CachedCotEvent> purgeEvents = new ArrayList<>();

        synchronized (mCachedEvents) {

            for (CachedCotEvent cachedCotEvent : mCachedEvents) {
                int purgeTime = mDefaultCachePurgeTimeMs;
                if (cachedCotEvent.cotEvent.getType().equals(CotMessageTypes.TYPE_PLI)) {
                    purgeTime = mDataRateAwarePliCachePurgeTimeMs;
                }
                if (currentTime - cachedCotEvent.lastSentTime > purgeTime) {
                    purgeEvents.add(cachedCotEvent);
                }
            }

            for (CachedCotEvent purgeableCotEvent : purgeEvents) {
                mCachedEvents.remove(purgeableCotEvent);
            }
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
