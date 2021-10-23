package com.paulmandal.atak.forwarder.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.paulmandal.atak.forwarder.gson.ChannelConfig;

import java.util.ArrayList;
import java.util.List;

public class ChannelJsonHelper {

    private Gson mGson;

    public ChannelJsonHelper(Gson gson) {
        mGson = gson;
    }

    public String toJson(List<ChannelConfig> channelConfigs) throws ChannelJsonException {
        try {
            return mGson.toJson(channelConfigs, ArrayList.class);
        } catch (JsonIOException e) {
            e.printStackTrace();
            throw new ChannelJsonException(e);
        }
    }

    public List<ChannelConfig> listFromJson(String json) throws ChannelJsonException {
        try {
            return mGson.fromJson(json, new TypeToken<ArrayList<ChannelConfig>>() {}.getType());
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new ChannelJsonException(e);
        }
    }
}
