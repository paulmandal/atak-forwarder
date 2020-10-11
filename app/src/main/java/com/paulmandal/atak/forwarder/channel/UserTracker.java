package com.paulmandal.atak.forwarder.channel;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserTracker implements MeshtasticCommHardware.UserListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + UserTracker.class.getSimpleName();

    public interface ChannelMembersUpdateListener {
        void onChannelMembersUpdated(List<UserInfo> atakUsers, List<NonAtakUserInfo> nonAtakStations);
    }

    public interface NonAtakStationUpdateListener {
        void onNonAtakStationUpdated(NonAtakUserInfo nonAtakUserInfo);
    }

    public static final String USER_NOT_FOUND = "";

    private Context mAtakContext;
    private Handler mUiThreadHandler;

    private final List<UserInfo> mAtakUsers = new CopyOnWriteArrayList<>();
    private final List<NonAtakUserInfo> mNonAtakStations = new CopyOnWriteArrayList<>();

    private final List<ChannelMembersUpdateListener> mChannelMembersUpdateListeners = new CopyOnWriteArrayList<>();
    private final List<NonAtakStationUpdateListener> mNonAtakStationUpdateListener = new CopyOnWriteArrayList<>();

    public UserTracker(Context atakContext,
                       Handler uiThreadHandler) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
    }

    public List<UserInfo> getAtakUsers() {
        return mAtakUsers;
    }
    public List<NonAtakUserInfo> getNonAtakStations() { return mNonAtakStations; }

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

        // Remove user from non-ATAK stations list if present
        UserInfo nonAtakStationUserInfo = null;
        for (UserInfo user : mNonAtakStations) {
            if (user.meshId.equals(meshId)) {
                nonAtakStationUserInfo = user;
                break;
            }
        }

        if (nonAtakStationUserInfo != null) {
            mNonAtakStations.remove(nonAtakStationUserInfo);
        }

        // Add user to ATAK users list and notify listeners
        if (!foundInAtakUsers) {
            Log.d(TAG, "Adding new user from discovery broadcast: " + callsign + ", atakUid: " + atakUid);
            mAtakUsers.add(new UserInfo(callsign, meshId, atakUid, null));

            notifyChannelMembersUpdateListeners();
        }

        Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChannelUsersUpdated(List<NonAtakUserInfo> userInfoList) {
        List<NonAtakUserInfo> newUsers = new ArrayList<>();

        boolean updatedNonAtakStation = false;

        for (NonAtakUserInfo possiblyNewUser : userInfoList) {
            boolean userExistsInAtakUserList = maybeUpdateUserBatteryPercentage(possiblyNewUser);

            boolean repeatedUserEntry = newUsers.contains(possiblyNewUser);
            boolean alreadyKnowAboutStation = mNonAtakStations.contains(possiblyNewUser);
            if (!userExistsInAtakUserList && !repeatedUserEntry && !alreadyKnowAboutStation) {
                newUsers.add(possiblyNewUser);
            }

            if (alreadyKnowAboutStation) {
                updatedNonAtakStation = true;
                updateNonAtakStation(possiblyNewUser);
            }
        }

        if (newUsers.size() > 0) {
            for (NonAtakUserInfo user : newUsers) {
                Log.d(TAG, "Adding new non-ATAK user from Meshtastic: " + user.callsign);
            }
            mNonAtakStations.addAll(newUsers);
        }

        if (newUsers.size() > 0 || updatedNonAtakStation) {
            notifyChannelMembersUpdateListeners();
        }
    }

    @Override
    public void onUserUpdated(NonAtakUserInfo nonAtakUserInfo) {
        boolean userExistsInAtakUserList = maybeUpdateUserBatteryPercentage(nonAtakUserInfo);

        if (userExistsInAtakUserList) {
            // Nothing else to do
            return;
        }

        boolean alreadyKnowAboutStation = mNonAtakStations.contains(nonAtakUserInfo);
        if (alreadyKnowAboutStation) {
            updateNonAtakStation(nonAtakUserInfo);
        } else {
            mNonAtakStations.add(nonAtakUserInfo);
        }

        // Notify listeners
        notifyNonAtakStationUpdateListeners(nonAtakUserInfo);
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

    public void addNonAtakStationUpdateListener(NonAtakStationUpdateListener listener) {
        mNonAtakStationUpdateListener.add(listener);
    }

    public void removeNonAtakStationUpdateListener(NonAtakStationUpdateListener listener) {
        mNonAtakStationUpdateListener.remove(listener);
    }

    public void clearData() {
        mAtakUsers.clear();
        mNonAtakStations.clear();
        notifyChannelMembersUpdateListeners();
    }

    private boolean maybeUpdateUserBatteryPercentage(NonAtakUserInfo nonAtakUserInfo) {
        boolean userExistsInAtakUserList = false;

        for (UserInfo user : mAtakUsers) {
            if (user.meshId.equals(nonAtakUserInfo.meshId)) {
                userExistsInAtakUserList = true;

                if (!Objects.equals(user.batteryPercentage, nonAtakUserInfo.batteryPercentage)) {
                    user.batteryPercentage = nonAtakUserInfo.batteryPercentage;
                }

                break;
            }
        }

        return userExistsInAtakUserList;
    }

    private void updateNonAtakStation(NonAtakUserInfo nonAtakUserInfo) {
        NonAtakUserInfo userInfo = mNonAtakStations.get(mNonAtakStations.indexOf(nonAtakUserInfo));

        if (!Objects.equals(userInfo.lat, nonAtakUserInfo.lat)) {
            userInfo.lat = nonAtakUserInfo.lat;
        }

        if (!Objects.equals(userInfo.lon, nonAtakUserInfo.lon)) {
            userInfo.lon = nonAtakUserInfo.lon;
        }

        if (!Objects.equals(userInfo.altitude, nonAtakUserInfo.altitude)) {
            userInfo.altitude = nonAtakUserInfo.altitude;
        }

        if (!Objects.equals(userInfo.batteryPercentage, nonAtakUserInfo.batteryPercentage)) {
            userInfo.batteryPercentage = nonAtakUserInfo.batteryPercentage;
        }
    }

    private void notifyNonAtakStationUpdateListeners(NonAtakUserInfo nonAtakUserInfo) {
        for (NonAtakStationUpdateListener listener : mNonAtakStationUpdateListener) {
            mUiThreadHandler.post(() -> listener.onNonAtakStationUpdated(nonAtakUserInfo));
        }
    }
    private void notifyChannelMembersUpdateListeners() {
        for (ChannelMembersUpdateListener channelMembersUpdateListener : mChannelMembersUpdateListeners) {
            mUiThreadHandler.post(() -> channelMembersUpdateListener.onChannelMembersUpdated(mAtakUsers, mNonAtakStations));
        }
    }
}
