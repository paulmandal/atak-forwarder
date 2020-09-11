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

public class GroupTracker implements MeshtasticCommHardware.GroupListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + GroupTracker.class.getSimpleName();

    public interface UpdateListener {
        void onUsersUpdated();
        void onGroupUpdated();
    }

    public static final String USER_NOT_FOUND = "";

    private Context mAtakContext;
    private Handler mHandler;

    private StateStorage mStateStorage;

    private List<UserInfo> mUserInfoList;
    private GroupInfo mGroupInfo;

    private UpdateListener mUpdateListener;

    public GroupTracker(Context atakContext,
                        Handler uiThreadHandler,
                        StateStorage stateStorage,
                        @Nullable List<UserInfo> userInfoList,
                        @Nullable GroupInfo groupInfo) {
        mAtakContext = atakContext;
        mHandler = uiThreadHandler;
        mStateStorage = stateStorage;

        if (userInfoList == null) {
            userInfoList = new ArrayList<>();
        }

        mUserInfoList = userInfoList;
        mGroupInfo = groupInfo;
    }

    public List<UserInfo> getUsers() {
        return mUserInfoList;
    }

    public GroupInfo getGroup() {
        return mGroupInfo;
    }

    @Override
    public void onUserDiscoveryBroadcastReceived(String callsign, String meshId, String atakUid) {
        // Check for user
        boolean found = false;
        for (UserInfo user : mUserInfoList) {
            if (user.meshId.equals(meshId)) {
                if (user.atakUid == null || !user.atakUid.equals(atakUid)) {
                    Log.d(TAG, "## onUserDiscoveryBroadcastReceived, adding atakUid for user: " + callsign + ", meshId: " + meshId + ", atakUid: " + atakUid);
                    user.callsign = callsign;
                    user.atakUid = atakUid;
                }
                found = true;
                break;
            }
        }

        if (!found) {
            Log.d(TAG, "## onUserDiscoveryBroadcastReceived, add: " + callsign + ", meshId: " + meshId + ", atakUid: " + atakUid);
            mUserInfoList.add(new UserInfo(callsign, meshId, atakUid, false));

            if (mUpdateListener != null) {
                mHandler.post(() -> mUpdateListener.onUsersUpdated());
            }
        }

        storeState();
        Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGroupMembersUpdated(List<UserInfo> userInfoList) {
        List<UserInfo> newUsers = new ArrayList<>();

        for (UserInfo possiblyNewUser : userInfoList) {
            boolean found = false;
            for (UserInfo user : mUserInfoList) {
                if (user.meshId.equals(possiblyNewUser.meshId)) {
                    found = true;
                    break;
                }
            }

            if (!found && !newUsers.contains(possiblyNewUser)) {
                newUsers.add(possiblyNewUser);
            }
        }

        if (newUsers.size() > 0) {
            mUserInfoList.addAll(newUsers);

            if (mUpdateListener != null) {
                mHandler.post(() -> mUpdateListener.onGroupUpdated());
            }

            storeState();
        }
    }

    public String getMeshIdForUid(String atakUid) {
        for (UserInfo userInfo : mUserInfoList) {
            if (userInfo.atakUid.equals(atakUid)) {
                return userInfo.meshId;
            }
        }
        return USER_NOT_FOUND;
    }

    public void setUpdateListener(UpdateListener updateListener) {
        mUpdateListener = updateListener;
    }

    public void clearData() {
        mUserInfoList = new ArrayList<>();
        mGroupInfo = null;
        storeState();
    }

    private void storeState() {
        mStateStorage.storeState(mUserInfoList, mGroupInfo);
    }
}
