package com.paulmandal.atak.forwarder.channel;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.TrackerEventHandler;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class UserTracker implements DiscoveryBroadcastEventHandler.DiscoveryBroadcastListener,
        TrackerEventHandler.TrackerListener,
        InboundMessageHandler.InboundPliListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + UserTracker.class.getSimpleName();

    private static final String KEY_STATUS = "status";
    private static final String KEY_BATTERY = "battery";

    public interface ChannelMembersUpdateListener {
        void onChannelMembersUpdated(List<UserInfo> atakUsers, List<TrackerUserInfo> trackers);
    }

    public interface TrackerUpdateListener {
        void trackerUpdated(TrackerUserInfo trackerUserInfo);
    }

    public static final String USER_NOT_FOUND = "";

    private final Context mAtakContext;
    private final Handler mUiThreadHandler;
    private final Logger mLogger;

    private final List<UserInfo> mAtakUsers = new CopyOnWriteArrayList<>();
    private final List<TrackerUserInfo> mTrackers = new CopyOnWriteArrayList<>();

    private final Set<ChannelMembersUpdateListener> mChannelMembersUpdateListeners = new CopyOnWriteArraySet<>();
    private final Set<TrackerUpdateListener> mTrackerUpdateListener = new CopyOnWriteArraySet<>();

    public UserTracker(Context atakContext,
                       Handler uiThreadHandler,
                       Logger logger,
                       DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler,
                       TrackerEventHandler trackerEventHandler) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mLogger = logger;

        discoveryBroadcastEventHandler.setListener(this);
        trackerEventHandler.addListener(this);
    }

    @Override
    public void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid) {
        mLogger.v(TAG, "onUserDiscoveryBroadcastReceived, callsign: " + callsign + ", meshId: " + meshId + ", atakUid: " + atakUid);

        // Check for user
        boolean foundInAtakUsers = false;
        for (UserInfo user : mAtakUsers) {
            if (user.meshId.equals(meshId)) {
                if (user.atakUid == null || !user.atakUid.equals(atakUid)) {
                    user.callsign = callsign;
                    user.atakUid = atakUid;
                }
                foundInAtakUsers = true;
                mLogger.v(TAG, "  found: " + callsign + " in ATAK users list with atakUid: " + atakUid);
                break;
            }
        }

        // Remove user from Trackers list if present
        TrackerUserInfo trackerUserInfo = null;
        for (TrackerUserInfo user : mTrackers) {
            if (user.meshId.equals(meshId)) {
                trackerUserInfo = user;
                mLogger.v(TAG, "  found: " + callsign + " in Tracker user list with meshId: " + meshId);
                break;
            }
        }

        if (trackerUserInfo != null) {
            mLogger.v(TAG, "  removing callsign: " + callsign + " from Tracker users");
            mTrackers.remove(trackerUserInfo);
        }

        // Add user to ATAK users list and notify listeners
        if (!foundInAtakUsers) {
            mLogger.v(TAG, "  Adding new user from discovery broadcast: " + callsign + ", atakUid: " + atakUid);
            mAtakUsers.add(new UserInfo(callsign, meshId, atakUid, null));

            notifyChannelMembersUpdateListeners();
        }

        mUiThreadHandler.post(() -> Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onTrackerUpdated(TrackerUserInfo trackerUserInfo) {
        mLogger.v(TAG, "onTrackerUpdated callsign: " + trackerUserInfo.callsign + ", meshId: " + trackerUserInfo.meshId + ", atakUid: " + trackerUserInfo.atakUid);
        boolean userExistsInAtakUserList = maybeUpdateUserBatteryPercentage(trackerUserInfo);

        if (userExistsInAtakUserList) {
            // Nothing else to do
            mLogger.v(TAG, "  Tracker exists in ATAK user list, exiting");
            return;
        }

        boolean alreadyKnowAboutStation = mTrackers.contains(trackerUserInfo);
        mLogger.v(TAG, "  alreadyKnowAboutStation: " + alreadyKnowAboutStation);
        if (alreadyKnowAboutStation) {
            updateTracker(trackerUserInfo);
        } else {
            mTrackers.add(trackerUserInfo);
        }

        // Notify listeners
        if (trackerUserInfo.gpsValid) {
            notifyTrackerUpdateListeners(trackerUserInfo);
        }
        notifyChannelMembersUpdateListeners();
    }

    @Override
    public void onInboundPli(CotEvent cotEvent) {
        mLogger.v(TAG, "onInboundPli: " + cotEvent);

        CotDetail cotDetail = cotEvent.getDetail();
        if (cotDetail == null) {
            return;
        }

        CotDetail statusDetail = cotDetail.getFirstChildByName(0, KEY_STATUS);
        if (statusDetail == null) {
            return;
        }

        String batteryLevelStr = statusDetail.getAttribute(KEY_BATTERY);
        if (batteryLevelStr == null) {
            return;
        }

        Integer batteryLevel = null;
        try {
            batteryLevel = Integer.parseInt(batteryLevelStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        String atakUid = cotEvent.getUID();
        for (UserInfo userInfo : mAtakUsers) {
            if (userInfo.atakUid != null && userInfo.atakUid.equals(atakUid)) {
                mLogger.v(TAG, "  updating UID: " + atakUid + ", batteryLevel: " + batteryLevel);
                userInfo.batteryPercentage = batteryLevel;
                notifyChannelMembersUpdateListeners();
                break;
            }
        }
    }

    public String getMeshIdForUid(String atakUid) {
        for (UserInfo userInfo : mAtakUsers) {
            if (userInfo.atakUid != null && userInfo.atakUid.equals(atakUid)) {
                return userInfo.meshId;
            }
        }
        return USER_NOT_FOUND;
    }

    public void addUpdateListener(ChannelMembersUpdateListener listener) {
        mChannelMembersUpdateListeners.add(listener);
    }

    public void removeUpdateListener(ChannelMembersUpdateListener listener) {
        mChannelMembersUpdateListeners.remove(listener);
    }

    public void addTrackerUpdateListener(TrackerUpdateListener listener) {
        mTrackerUpdateListener.add(listener);
    }

    public void removeTrackerUpdateListener(TrackerUpdateListener listener) {
        mTrackerUpdateListener.remove(listener);
    }

    public void clearData() {
        mAtakUsers.clear();
        mTrackers.clear();
        notifyChannelMembersUpdateListeners();
    }

    private boolean maybeUpdateUserBatteryPercentage(TrackerUserInfo trackerUserInfo) {
        boolean userExistsInAtakUserList = false;

        for (UserInfo user : mAtakUsers) {
            if (user.meshId.equals(trackerUserInfo.meshId)) {
                userExistsInAtakUserList = true;

                if (trackerUserInfo.batteryPercentage != null && !Objects.equals(user.batteryPercentage, trackerUserInfo.batteryPercentage)) {
                    user.batteryPercentage = trackerUserInfo.batteryPercentage;
                }

                break;
            }
        }

        return userExistsInAtakUserList;
    }

    private void updateTracker(TrackerUserInfo trackerUserInfo) {
        TrackerUserInfo userInfo = mTrackers.get(mTrackers.indexOf(trackerUserInfo));

        mLogger.v(TAG, "updateTracker, updating with data from: " + trackerUserInfo.callsign + ", meshId: " + trackerUserInfo.meshId + ", lat: " + trackerUserInfo.lat + ", lon: " + trackerUserInfo.lon + ", alt: " + trackerUserInfo.altitude + ", batteryPercentage: " + trackerUserInfo.batteryPercentage + ", lastSeenTime: " + trackerUserInfo.lastSeenTime);
        mLogger.v(TAG, "  possibly overwriting: " + userInfo.callsign + ", meshId: " + trackerUserInfo.meshId + ", lat: " + userInfo.lat + ", lon: " + userInfo.lon + ", alt: " + userInfo.altitude + ", batteryPercentage: " + userInfo.batteryPercentage + ", lastSeenTime: " + userInfo.lastSeenTime);

        userInfo.update(trackerUserInfo);
    }

    private void notifyTrackerUpdateListeners(TrackerUserInfo trackerUserInfo) {
        TrackerUserInfo userInfo = mTrackers.get(mTrackers.indexOf(trackerUserInfo));

        for (TrackerUpdateListener listener : mTrackerUpdateListener) {
            mUiThreadHandler.post(() -> listener.trackerUpdated(userInfo));
        }
    }

    private void notifyChannelMembersUpdateListeners() {
        for (ChannelMembersUpdateListener channelMembersUpdateListener : mChannelMembersUpdateListeners) {
            mUiThreadHandler.post(() -> channelMembersUpdateListener.onChannelMembersUpdated(mAtakUsers, mTrackers));
        }
    }
}
