package com.paulmandal.atak.forwarder.channel;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChannelTracker implements MeshtasticCommHardware.ChannelListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTracker.class.getSimpleName();

    public interface UpdateListener {
        void onUpdated();
    }

    public static final String USER_NOT_FOUND = "";

    private Context mAtakContext;
    private Handler mUiThreadHandler;

    private List<UserInfo> mUserInfoList;

    private List<UpdateListener> mUpdateListeners = new ArrayList<>();

    public ChannelTracker(Context atakContext,
                          Handler uiThreadHandler,
                          @Nullable List<UserInfo> userInfoList) {
        mAtakContext = atakContext;
        mUiThreadHandler = uiThreadHandler;

        if (userInfoList == null) {
            userInfoList = new ArrayList<>();
        }

        mUserInfoList = userInfoList;
    }

    public List<UserInfo> getUsers() {
        return mUserInfoList;
    }

    @Override
    public void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid) {
        // Check for user
        boolean found = false;
        for (UserInfo user : mUserInfoList) {
            if (user.meshId.equals(meshId)) {
                if (user.atakUid == null || !user.atakUid.equals(atakUid)) {
                    user.callsign = callsign;
                    user.atakUid = atakUid;
                }
                found = true;
                break;
            }
        }

        if (!found) {
            mUserInfoList.add(new UserInfo(callsign, meshId, atakUid, false, null));

            notifyListeners();
        }

        Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChannelMembersUpdated(List<UserInfo> userInfoList) {
        List<UserInfo> newUsers = new ArrayList<>();

        for (UserInfo possiblyNewUser : userInfoList) {
            boolean found = false;
            for (UserInfo user : mUserInfoList) {
                if (user.meshId.equals(possiblyNewUser.meshId)) {
                    found = true;

                    if (!Objects.equals(user.batteryPercentage, possiblyNewUser.batteryPercentage)) {
                        user.batteryPercentage = possiblyNewUser.batteryPercentage;
                    }

                    break;
                }
            }

            if (!found && !newUsers.contains(possiblyNewUser)) {
                newUsers.add(possiblyNewUser);
            }
        }

        if (newUsers.size() > 0) {
            mUserInfoList.addAll(newUsers);

            notifyListeners();
        }
    }

    public String getMeshIdForUid(String atakUid) {
        for (UserInfo userInfo : mUserInfoList) {
            if (userInfo.atakUid != null && userInfo.atakUid.equals(atakUid)) {
                return userInfo.meshId;
            }
        }
        return USER_NOT_FOUND;
    }

    public void addUpdateListener(UpdateListener listener) {
        mUpdateListeners.add(listener);
    }

    public void removeUpdateListener(UpdateListener listener) {
        mUpdateListeners.remove(listener);
    }

    public void clearData() {
        mUserInfoList = new ArrayList<>();
        notifyListeners();
    }

    private void notifyListeners() {
        for (UpdateListener updateListener : mUpdateListeners) {
            mUiThreadHandler.post(updateListener::onUpdated);
        }
    }
}
