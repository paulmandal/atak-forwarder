package com.paulmandal.atak.forwarder.group;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;

import java.util.ArrayList;
import java.util.List;

import static com.paulmandal.atak.forwarder.plugin.ui.GroupManagementDropDownReceiver.TAG;

public class GroupTracker implements GoTennaCommHardware.GroupListener {
    public interface UpdateListener {
        void onUsersUpdated();
        void onGroupUpdated();
    }

    public static final long USER_NOT_FOUND = -1;

    private Context mPluginContext;

    private List<UserInfo> mUserInfo = new ArrayList<>();
    private GroupInfo mGroupInfo;

    private UpdateListener mUpdateListener;

    public GroupTracker(Context context) {
        mPluginContext = context;
    }

    public List<UserInfo> getUsers() {
        return mUserInfo;
    }

    public GroupInfo getGroup() {
        return mGroupInfo;
    }

    @Override
    public void onUserDiscoveryBroadcast(String callsign, long gId, String atakUid) {
        Log.d(TAG, "onUserDiscoveryBroadcast: " + callsign);
        // Check for user
        boolean found = false;
        for (UserInfo user : mUserInfo) {
            if (user.gId == gId) {
                found = true;
                break;
            }
        }

        if (!found) {
            mUserInfo.add(new UserInfo(callsign, gId, atakUid, false));
        }

        if (mUpdateListener != null) {
            mUpdateListener.onUsersUpdated();
        }

        Toast.makeText(mPluginContext, "User discovery broadcast received for " + callsign, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGroupCreated(long groupId, List<Long> memberGids) {
        Log.d("ATAKDBG", "onGroupCreated: " + groupId + " ids: " + memberGids.toString());
        mGroupInfo = new GroupInfo(groupId, memberGids);

        // Update group membership
        for (long memberGid : memberGids) {
            for (UserInfo userInfo : mUserInfo) {
                if (userInfo.gId == memberGid) {
                    userInfo.isInGroup = true;
                    break;
                }
            }
        }

        if (mUpdateListener != null) {
            mUpdateListener.onGroupUpdated();
        }
    }

    public long getGidForUid(String atakUid) {
        for (UserInfo userInfo : mUserInfo) {
            if (userInfo.atakUid.equals(atakUid)) {
                return userInfo.gId;
            }
        }
        return USER_NOT_FOUND;
    }

    public void setUpdateListener(UpdateListener updateListener) {
        mUpdateListener = updateListener;
    }
}
