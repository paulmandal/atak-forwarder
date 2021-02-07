package com.paulmandal.atak.forwarder.plugin.ui.tabs.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.channel.NonAtakUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.commhardware.MeshtasticCommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.plugin.ui.tabs.HashHelper;

import java.util.ArrayList;
import java.util.List;

public class StatusViewModel extends ChannelStatusViewModel implements UserTracker.ChannelMembersUpdateListener,
        CommandQueue.Listener,
        CommHardware.ConnectionStateListener,
        MeshtasticCommHardware.MessageAckNackListener,
        CommHardware.MessageListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + StatusViewModel.class.getSimpleName();

    private CommHardware mCommHardware;

    private MutableLiveData<List<UserInfo>> mUserInfoList = new MutableLiveData<>();
    private MutableLiveData<Integer> mMessageQueueSize = new MutableLiveData<>();
    private MutableLiveData<CommHardware.ConnectionState> mConnectionState = new MutableLiveData<>();
    private MutableLiveData<Integer> mTotalMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mErroredMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mDeliveredMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mTimedOutMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mReceivedMessages = new MutableLiveData<>();
    private MutableLiveData<Integer> mErrorsInARow = new MutableLiveData<>();

    public StatusViewModel(UserTracker userTracker,
                           MeshtasticCommHardware commHardware,
                           CommandQueue commandQueue,
                           HashHelper hashHelper) {
        super(commHardware, hashHelper);

        mCommHardware = commHardware;

        mMessageQueueSize.setValue(0);
        mTotalMessages.setValue(0);
        mErroredMessages.setValue(0);
        mDeliveredMessages.setValue(0);
        mErrorsInARow.setValue(0);
        mTimedOutMessages.setValue(0);
        mReceivedMessages.setValue(0);

        userTracker.addUpdateListener(this);
        commandQueue.setListener(this);
        commHardware.addConnectionStateListener(this);
        commHardware.addMessageAckNackListener(this);
        commHardware.addMessageListener(this);
    }

    @Override
    public void onChannelMembersUpdated(List<UserInfo> atakUsers, List<NonAtakUserInfo> nonAtakStations) {
        List<UserInfo> allStations = new ArrayList<>(atakUsers.size() + nonAtakStations.size());
        allStations.addAll(atakUsers);
        allStations.addAll(nonAtakStations);
        mUserInfoList.setValue(allStations);
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

    @NonNull
    public LiveData<Integer> getTotalMessage() {
        return mTotalMessages;
    }

    @NonNull
    public LiveData<Integer> getTimedOutMessages() {
        return mTimedOutMessages;
    }

    @NonNull
    public LiveData<Integer> getErroredMessages() {
        return mErroredMessages;
    }

    @NonNull
    public LiveData<Integer> getReceivedMessages() {
        return mReceivedMessages;
    }

    @NonNull
    public LiveData<Integer> getDeliveredMessages() {
        return mDeliveredMessages;
    }

    @NonNull
    public LiveData<Integer> getErrorsInARow() {
        return mErrorsInARow;
    }

    public void connect() {
        mCommHardware.connect();
    }

    public void broadcastDiscoveryMessage() {
        mCommHardware.broadcastDiscoveryMessage();
    }

    @Override
    public void onMessageAckNack(int messageId, boolean isAck) {
        mTotalMessages.setValue(mTotalMessages.getValue() + 1);
        if (isAck) {
            mErrorsInARow.setValue(0);
            mDeliveredMessages.setValue(mDeliveredMessages.getValue() + 1);
        } else {
            mErrorsInARow.setValue(mErrorsInARow.getValue() + 1);
            mErroredMessages.setValue(mErroredMessages.getValue() + 1);
        }
    }

    @Override
    public void onMessageTimedOut(int messageId) {
        mTotalMessages.setValue(mTotalMessages.getValue() + 1);
        mTimedOutMessages.setValue(mTimedOutMessages.getValue() + 1);
    }

    @Override
    public void onMessageReceived(int messageId, byte[] message) {
        mReceivedMessages.setValue(mReceivedMessages.getValue() + 1);
    }
}
