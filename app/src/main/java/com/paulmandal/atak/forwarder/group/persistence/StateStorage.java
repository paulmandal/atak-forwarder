package com.paulmandal.atak.forwarder.group.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.UserInfo;

import java.util.List;

public class StateStorage {
    private static final int DEFAULT_CACHE_PURGE_TIME_MS = Config.DEFAULT_CACHE_PURGE_TIME_MS;
    private static final int DEFAULT_PLI_CACHE_PURGE_TIME_MS = Config.DEFAULT_PLI_CACHE_PURGE_TIME_MS;

    private static final String KEY_PLI_CACHE_PURGE_TIME = "pliCachePurgeTime";
    private static final String KEY_DEFAULT_CACHE_PURGE_TIME = "defaultCachePurgeTime";
    private static final String KEY_USERS = "users";
    private static final String KEY_GROUP = "group";

    private Context mContext;
    private JsonHelper mJsonHelper;

    public StateStorage(Context context, JsonHelper jsonHelper) {
        mContext = context;
        mJsonHelper = jsonHelper;
    }

    @Nullable
    public List<UserInfo> getUsers() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        String usersStr = sharedPref.getString(KEY_USERS, null);

        if (usersStr == null) {
            return null;
        }

        return mJsonHelper.parseUserJson(usersStr);
    }

    @Nullable
    public GroupInfo getGroupInfo() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        String groupStr = sharedPref.getString(KEY_GROUP, null);

        if (groupStr == null) {
            return null;
        }

        return mJsonHelper.parseGroupJson(groupStr);

    }

    public int getDefaultCachePurgeTimeMs() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        return sharedPref.getInt(KEY_DEFAULT_CACHE_PURGE_TIME, DEFAULT_CACHE_PURGE_TIME_MS);
    }

    public int getPliCachePurgeTimeMs() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        return sharedPref.getInt(KEY_PLI_CACHE_PURGE_TIME, DEFAULT_PLI_CACHE_PURGE_TIME_MS);
    }

    public void storeDefaultCachePurgeTime(int shapeCachePurgeTimeMs) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_DEFAULT_CACHE_PURGE_TIME, shapeCachePurgeTimeMs);
        editor.apply();
    }

    public void storePliCachePurgeTime(int pliCachePurgeTimeMs) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_PLI_CACHE_PURGE_TIME, pliCachePurgeTimeMs);
        editor.apply();
    }

    public void storeState(List<UserInfo> userInfoList, GroupInfo groupInfo) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.shared_prefs_filename), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_USERS, mJsonHelper.toJson(userInfoList));
        editor.putString(KEY_GROUP, groupInfo == null ? null : mJsonHelper.toJson(groupInfo));
        editor.apply();
    }
}
