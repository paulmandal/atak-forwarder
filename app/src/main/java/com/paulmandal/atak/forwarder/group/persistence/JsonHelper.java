package com.paulmandal.atak.forwarder.group.persistence;

import com.paulmandal.atak.forwarder.group.UserInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonHelper {

    private static final String KEY_MESH_ID = "meshId";
    private static final String KEY_ATAK_UID = "atakUid";
    private static final String KEY_IS_IN_GROUP = "isInGroup";
    private static final String KEY_CALLSIGN = "callsign";
    private static final String KEY_BATTERY_PERCENTAGE = "batteryPercentage";

    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_MEMBER_GIDS = "memberGids";

    private static final int NO_VALUE = -1;

    public List<UserInfo> parseUserJson(String userJsonStr) {
        try {
            JSONArray jsonArray = new JSONArray(userJsonStr);
            List<UserInfo> userInfoList = new ArrayList<>();
            for (int i = 0 ; i < jsonArray.length() ; i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String meshId = json.getString(KEY_MESH_ID);
                String atakUid = json.getString(KEY_ATAK_UID);
                boolean isInGroup = json.getBoolean(KEY_IS_IN_GROUP);
                String callsign = json.getString(KEY_CALLSIGN);
                Integer batteryPercentage = json.getInt(KEY_BATTERY_PERCENTAGE);
                if (batteryPercentage == NO_VALUE) {
                    batteryPercentage = null;
                }

                UserInfo userInfo = new UserInfo(callsign, meshId, atakUid, isInGroup, batteryPercentage);
                userInfoList.add(userInfo);
            }

            return userInfoList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

//    public GroupInfo parseGroupJson(String groupInfoStr) {
//        try {
//            JSONObject json = new JSONObject(groupInfoStr);
//            long groupId = json.getLong(KEY_GROUP_ID);
//            List<Long> memberGids = new ArrayList<>();
//
//            JSONArray jsonArray = json.getJSONArray(KEY_MEMBER_GIDS);
//            for (int i = 0 ; i < jsonArray.length() ; i++) {
//                memberGids.add(jsonArray.getLong(i));
//            }
//
//            return new GroupInfo(groupId, memberGids);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public String toJson(List<UserInfo> userInfoList) {
        JSONArray jsonArray = new JSONArray();
        try {
            for (UserInfo userInfo : userInfoList) {
                JSONObject userJson = new JSONObject();
                userJson.put(KEY_MESH_ID, userInfo.meshId);
                userJson.put(KEY_ATAK_UID, userInfo.atakUid);
                userJson.put(KEY_IS_IN_GROUP, userInfo.isInGroup);
                userJson.put(KEY_CALLSIGN, userInfo.callsign);
                userJson.put(KEY_BATTERY_PERCENTAGE, userInfo.batteryPercentage != null ? userInfo.batteryPercentage : NO_VALUE);
                jsonArray.put(userJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray.toString();
    }

//    public String toJson(GroupInfo groupInfo) {
//        JSONObject json = new JSONObject();
//        try {
//            json.put(KEY_GROUP_ID, groupInfo.groupId);
//            JSONArray jsonArray = new JSONArray();
//            for (long memberGid : groupInfo.memberGids) {
//                jsonArray.put(memberGid);
//            }
//            json.put(KEY_MEMBER_GIDS, jsonArray);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return json.toString();
//    }
}
