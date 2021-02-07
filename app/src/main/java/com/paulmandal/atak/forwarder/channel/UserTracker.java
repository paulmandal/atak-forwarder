package com.paulmandal.atak.forwarder.channel;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.refactor.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.helpers.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserTracker implements MeshtasticCommHardware.UserListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + UserTracker.class.getSimpleName();

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

    private final List<ChannelMembersUpdateListener> mChannelMembersUpdateListeners = new CopyOnWriteArrayList<>();
    private final List<TrackerUpdateListener> mTrackerUpdateListener = new CopyOnWriteArrayList<>();

    public UserTracker(Context atakContext,
                       Handler uiThreadHandler,
                       Logger logger) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
        mLogger = logger;
    }

    public List<UserInfo> getAtakUsers() {
        return mAtakUsers;
    }
    public List<TrackerUserInfo> getTrackers() { return mTrackers; }

    @Override
    public void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid) {
        // Check for user
        boolean foundInAtakUsers = false;
        for (UserInfo user : mAtakUsers) {
            if (user.meshId.equals(meshId)) {
                if (user.atakUid == null || !user.atakUid.equals(atakUid)) {
                    user.callsign = callsign;
                    user.atakUid = atakUid;
                }
                foundInAtakUsers = true;
                break;
            }
        }

        // Remove user from Trackers list if present
        UserInfo trackerUserInfo = null;
        for (UserInfo user : mTrackers) {
            if (user.meshId.equals(meshId)) {
                trackerUserInfo = user;
                break;
            }
        }

        if (trackerUserInfo != null) {
            mTrackers.remove(trackerUserInfo);
        }

        // Add user to ATAK users list and notify listeners
        if (!foundInAtakUsers) {
            mLogger.d(TAG, "Adding new user from discovery broadcast: " + callsign + ", atakUid: " + atakUid);
            mAtakUsers.add(new UserInfo(callsign, meshId, atakUid, null));

            notifyChannelMembersUpdateListeners();
        }

        Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUserUpdated(TrackerUserInfo trackerUserInfo) {
        boolean userExistsInAtakUserList = maybeUpdateUserBatteryPercentage(trackerUserInfo);

        if (userExistsInAtakUserList) {
            // Nothing else to do
            return;
        }

        boolean alreadyKnowAboutStation = mTrackers.contains(trackerUserInfo);
        if (alreadyKnowAboutStation) {
            updateTracker(trackerUserInfo);
        } else {
            mTrackers.add(trackerUserInfo);
        }

        // Notify listeners
        notifyTrackerUpdateListeners();
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
