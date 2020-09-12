package com.paulmandal.atak.forwarder.group;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChannelTracker implements MeshtasticCommHardware.ChannelListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + ChannelTracker.class.getSimpleName();

    public interface UpdateListener {
        void onUsersUpdated();
        void onChannelUpdated();
    }

    public static final String USER_NOT_FOUND = "";

    private Context mAtakContext;
    private Handler mHandler;

    private StateStorage mStateStorage;

    private List<UserInfo> mUserInfoList;

    private String mChannelName;
    private byte[] mPsk;

    private List<UpdateListener> mUpdateListeners = new ArrayList<>();

    public ChannelTracker(Context atakContext,
                          Handler uiThreadHandler,
                          StateStorage stateStorage,
                          @Nullable List<UserInfo> userInfoList) {
        mAtakContext = atakContext;
        mHandler = uiThreadHandler;
        mStateStorage = stateStorage;

        if (userInfoList == null) {
            userInfoList = new ArrayList<>();
        }

        mUserInfoList = userInfoList;
    }

    public List<UserInfo> getUsers() {
        return mUserInfoList;
    }
    public String getChannelName() {
        return mChannelName;
    }
    public byte[] getPsk() {
        return mPsk;
    }

    @Override
    public void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid) {
        // Check for user
        boolean found = false;
        for (UserInfo user : mUserInfoList) {
            Log.e(TAG, "checking against uer: " + user.meshId + ", " + user.callsign + " for incomfing meshId: " + meshId);
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
            Log.e(TAG, "adding user in ChannelTracker: " + callsign + ", " + meshId + ", " + atakUid + ", " + false + ", " + null);
            mUserInfoList.add(new UserInfo(callsign, meshId, atakUid, false, null));

            for (UpdateListener updateListener : mUpdateListeners) {
                updateListener.onUsersUpdated();
            }
        }

        storeState();
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
                Log.e(TAG, "adding user in onChannelMembersUpdated: " + possiblyNewUser.callsign + ", " + possiblyNewUser.meshId + ", " + possiblyNewUser.atakUid + ", " + possiblyNewUser.isInGroup + ", " + possiblyNewUser.batteryPercentage);
                newUsers.add(possiblyNewUser);
            }
        }

        if (newUsers.size() > 0) {
            mUserInfoList.addAll(newUsers);

            for (UpdateListener updateListener : mUpdateListeners) {
                updateListener.onChannelUpdated();
            }

            storeState();
        }
    }

    @Override
    public void onChannelSettingsUpdated(String channelName, byte[] psk) {
        mChannelName = channelName;
        mPsk = psk;

        for (UpdateListener updateListener : mUpdateListeners) {
            updateListener.onChannelUpdated();
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
        storeState();
    }

    private void storeState() {
        mStateStorage.storeState(mUserInfoList);
    }
}
