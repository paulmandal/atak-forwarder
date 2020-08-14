package com.paulmandal.atak.forwarder.group;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.comm.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.group.persistence.StateStorage;

import java.util.ArrayList;
import java.util.List;

public class GroupTracker implements GoTennaCommHardware.GroupListener {
    public interface UpdateListener {
        void onUsersUpdated();
        void onGroupUpdated();
    }

    public static final long USER_NOT_FOUND = -1;

    private Context mPluginContext;
    private StateStorage mStateStorage;

    private List<UserInfo> mUserInfoList;
    private GroupInfo mGroupInfo;

    private UpdateListener mUpdateListener;

    public GroupTracker(Context context, StateStorage stateStorage, @Nullable List<UserInfo> userInfoList, @Nullable GroupInfo groupInfo) {
        mPluginContext = context;
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
    public void onUserDiscoveryBroadcastReceived(String callsign, long gId, String atakUid) {
        // Check for user
        boolean found = false;
        for (UserInfo user : mUserInfoList) {
            if (user.gId == gId) {
                found = true;
                break;
            }
        }

        if (!found) {
            mUserInfoList.add(new UserInfo(callsign, gId, atakUid, false));
        }

        if (mUpdateListener != null) {
            mUpdateListener.onUsersUpdated();
        }

        storeState();
        Toast.makeText(mPluginContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGroupCreated(long groupId, List<Long> memberGids) {
        mGroupInfo = new GroupInfo(groupId, memberGids);

        // Update group membership
        for (long memberGid : memberGids) {
            for (UserInfo userInfo : mUserInfoList) {
                if (userInfo.gId == memberGid) {
                    userInfo.isInGroup = true;
                    break;
                }
            }
        }

        if (mUpdateListener != null) {
            mUpdateListener.onGroupUpdated();
        }

        storeState();
    }

    public long getGidForUid(String atakUid) {
        for (UserInfo userInfo : mUserInfoList) {
            if (userInfo.atakUid.equals(atakUid)) {
                return userInfo.gId;
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
