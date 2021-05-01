package com.paulmandal.atak.forwarder.channel;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.TrackerEventHandler;
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class UserTracker implements DiscoveryBroadcastEventHandler.DiscoveryBroadcastListener,
        TrackerEventHandler.TrackerListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + UserTracker.class.getSimpleName();

    public interface ChannelMembersUpdateListener {
        void onChannelMembersUpdated(List<UserInfo> atakUsers, List<TrackerUserInfo> trackers);
    }

    public interface TrackerUpdateListener {
        void trackersUpdated(List<TrackerUserInfo> trackers);
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
        trackerEventHandler.setListener(this);
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
            mLogger.v(TAG, "Adding new user from discovery broadcast: " + callsign + ", atakUid: " + atakUid);
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
        notifyTrackerUpdateListeners();
        notifyChannelMembersUpdateListeners();
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

                if (!Objects.equals(user.batteryPercentage, trackerUserInfo.batteryPercentage)) {
                    user.batteryPercentage = trackerUserInfo.batteryPercentage;
                }

                break;
            }
        }

        return userExistsInAtakUserList;
    }

    private void updateTracker(TrackerUserInfo trackerUserInfo) {
        TrackerUserInfo userInfo = mTrackers.get(mTrackers.indexOf(trackerUserInfo));

        if (!Objects.equals(userInfo.lat, trackerUserInfo.lat)) {
            userInfo.lat = trackerUserInfo.lat;
        }

        if (!Objects.equals(userInfo.lon, trackerUserInfo.lon)) {
            userInfo.lon = trackerUserInfo.lon;
        }

        if (!Objects.equals(userInfo.altitude, trackerUserInfo.altitude)) {
            userInfo.altitude = trackerUserInfo.altitude;
        }

        if (!Objects.equals(userInfo.batteryPercentage, trackerUserInfo.batteryPercentage)) {
            userInfo.batteryPercentage = trackerUserInfo.batteryPercentage;
        }

        if (!Objects.equals(userInfo.lastSeenTime, trackerUserInfo.lastSeenTime)) {
            userInfo.lastSeenTime = trackerUserInfo.lastSeenTime;
        }
    }

    private void notifyTrackerUpdateListeners() {
        for (TrackerUpdateListener listener : mTrackerUpdateListener) {
            mUiThreadHandler.post(() -> listener.trackersUpdated(mTrackers));
        }
    }

    private void notifyChannelMembersUpdateListeners() {
        for (ChannelMembersUpdateListener channelMembersUpdateListener : mChannelMembersUpdateListeners) {
            mUiThreadHandler.post(() -> channelMembersUpdateListener.onChannelMembersUpdated(mAtakUsers, mTrackers));
        }
    }
}
