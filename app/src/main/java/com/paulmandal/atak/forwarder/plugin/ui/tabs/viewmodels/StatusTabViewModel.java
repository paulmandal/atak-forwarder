package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;

import java.util.List;

public class StatusTabViewModel implements ChannelTracker.UpdateListener,
        CommandQueue.Listener,
        CommHardware.ConnectionStateListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + StatusTabViewModel.class.getSimpleName();

    private CommHardware mCommHardware;

    private MutableLiveData<List<UserInfo>> mUserInfoList = new MutableLiveData<>();
    private MutableLiveData<Integer> mMessageQueueSize = new MutableLiveData<>();
    private MutableLiveData<CommHardware.ConnectionState> mConnectionState = new MutableLiveData<>();

    public StatusTabViewModel(ChannelTracker channelTracker,
                              CommHardware commHardware,
                              CommandQueue commandQueue) {
        mCommHardware = commHardware;

        channelTracker.addUpdateListener(this);
        commandQueue.setListener(this);
        commHardware.addConnectionStateListener(this);
    }

    @Override
    public void onUpdated(List<UserInfo> userInfoList) {
        mUserInfoList.setValue(userInfoList);
    }

    @Override
    public void onMessageQueueSizeChanged(int size) {
        mMessageQueueSize.setValue(size);
    }

    @Override
    public void onConnectionStateChanged(CommHardware.ConnectionState connectionState) {
        mConnectionState.setValue(connectionState);
    }

    @NonNull
    public LiveData<List<UserInfo>> getUserInfoList() {
        return mUserInfoList;
    }

    @NonNull
    public LiveData<Integer> getMessageQueueSize() {
        return mMessageQueueSize;
    }

    @NonNull
    public LiveData<CommHardware.ConnectionState> getConnectionState() {
        return mConnectionState;
    }

    public void connect() {
        mCommHardware.connect();
    }

    public void broadcastDiscoveryMessage() {
        mCommHardware.broadcastDiscoveryMessage();
    }
}
