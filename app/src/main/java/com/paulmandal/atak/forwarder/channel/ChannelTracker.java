package com.paulmandal.atak.forwarder.channel;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChannelTracker implements MeshtasticCommHardware.ChannelListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTracker.class.getSimpleName();

    public interface ChannelMembersUpdateListener {
        void onChannelMembersUpdated(List<UserInfo> atakUsers, List<NonAtakUserInfo> nonAtakStations);
    }

    public static final String USER_NOT_FOUND = "";

    private Context mAtakContext;
    private Handler mUiThreadHandler;

    private final List<UserInfo> mAtakUsers = new ArrayList<>();
    private final List<NonAtakUserInfo> mNonAtatkStations = new ArrayList<>();

    private List<ChannelMembersUpdateListener> mChannelMembersUpdateListeners = new ArrayList<>();

    public ChannelTracker(Context atakContext,
                          Handler uiThreadHandler) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;
    }

    public List<UserInfo> getAtakUsers() {
        return mAtakUsers;
    }
    public List<NonAtakUserInfo> getNonAtakStations() { return mNonAtatkStations; }

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
        for (UserInfo user : mNonAtatkStations) {
            if (user.meshId.equals(meshId)) {
                nonAtakStationUserInfo = user;
                break;
            }
        }

        if (nonAtakStationUserInfo != null) {
            mNonAtatkStations.remove(nonAtakStationUserInfo);
        }

        // Add user to ATAK users list and notify listeners
        if (!foundInAtakUsers) {
            mAtakUsers.add(new UserInfo(callsign, meshId, atakUid, false, null));

            notifyListeners();
        }

        Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChannelMembersUpdated(List<NonAtakUserInfo> userInfoList) {
        List<NonAtakUserInfo> newUsers = new ArrayList<>();

        for (NonAtakUserInfo possiblyNewUser : userInfoList) {
            boolean found = false;
            for (UserInfo user : mAtakUsers) {
                if (user.meshId.equals(possiblyNewUser.meshId)) {
                    found = true;

                    if (!Objects.equals(user.batteryPercentage, possiblyNewUser.batteryPercentage)) {
                        user.batteryPercentage = possiblyNewUser.batteryPercentage;
                    }

                    break;
                }
            }

            boolean repeatedUserEntry = newUsers.contains(possiblyNewUser);
            boolean alreadyKnowAboutStation = mNonAtatkStations.contains(possiblyNewUser);
            if (!found && !repeatedUserEntry && !alreadyKnowAboutStation) {
                newUsers.add(possiblyNewUser);
            }
        }

        if (newUsers.size() > 0) {
            mNonAtatkStations.addAll(newUsers);

            notifyListeners();
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

    public void clearData() {
        mAtakUsers.clear();
        notifyListeners();
    }

    private void notifyListeners() {
        for (ChannelMembersUpdateListener channelMembersUpdateListener : mChannelMembersUpdateListeners) {
            mUiThreadHandler.post(() -> channelMembersUpdateListener.onChannelMembersUpdated(mAtakUsers, mNonAtatkStations));
        }
    }
}
