package com.paulmandal.atak.forwarder.group;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;

public class GroupTracker implements MeshtasticCommHardware.GroupListener {
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
                found = true;
                break;
            }
        }

        if (!found) {
            mUserInfoList.add(new UserInfo(callsign, meshId, atakUid, false));
        }

        if (mUpdateListener != null) {
            mHandler.post(() -> mUpdateListener.onUsersUpdated());
        }

        storeState();
        Toast.makeText(mAtakContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGroupCreated(String meshId, List<String> memberGids) { // TODO: fix this, we don't create the group this way
//        mGroupInfo = new GroupInfo(meshId, memberGids);

        // Update group membership
        for (String memberGid : memberGids) {
            for (UserInfo userInfo : mUserInfoList) {
                if (userInfo.meshId.equals(memberGid)) {
                    userInfo.isInGroup = true;
                    break;
                }
            }
        }

        if (mUpdateListener != null) {
            mHandler.post(() -> mUpdateListener.onGroupUpdated());
        }

        storeState();
    }

//    public long getGidForUid(String atakUid) {
//        for (UserInfo userInfo : mUserInfoList) {
//            if (userInfo.atakUid.equals(atakUid)) {
//                return userInfo.gId;
//            }
//        }
//        return USER_NOT_FOUND;
//    }

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
