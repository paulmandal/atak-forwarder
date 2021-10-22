package com.paulmandal.atak.forwarder.plugin.ui.viewmodels;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.meshtastic.ConnectionState;
import com.paulmandal.atak.forwarder.comm.meshtastic.DiscoveryBroadcastEventHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshSender;
import com.paulmandal.atak.forwarder.comm.meshtastic.MeshServiceController;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.helpers.ChannelJsonHelper;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.ArrayList;
import java.util.List;

public class StatusViewModel extends ChannelStatusViewModel implements UserTracker.ChannelMembersUpdateListener,
        CommandQueue.Listener,
        MeshServiceController.ConnectionStateListener,
        MeshSender.MessageAckNackListener,
        InboundMeshMessageHandler.MessageListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + StatusViewModel.class.getSimpleName();

    private final DiscoveryBroadcastEventHandler mDiscoveryBroadcastEventHandler;

    private final MutableLiveData<List<UserInfo>> mUserInfoList = new MutableLiveData<>();
    private final MutableLiveData<Integer> mMessageQueueSize = new MutableLiveData<>();
    private final MutableLiveData<ConnectionState> mConnectionState = new MutableLiveData<>();
    private final MutableLiveData<Integer> mTotalMessages = new MutableLiveData<>();
    private final MutableLiveData<Integer> mErroredMessages = new MutableLiveData<>();
    private final MutableLiveData<Integer> mDeliveredMessages = new MutableLiveData<>();
    private final MutableLiveData<Integer> mTimedOutMessages = new MutableLiveData<>();
    private final MutableLiveData<Integer> mReceivedMessages = new MutableLiveData<>();
    private final MutableLiveData<Integer> mErrorsInARow = new MutableLiveData<>();

    public StatusViewModel(List<Destroyable> destroyables,
                           SharedPreferences sharedPreferences,
                           UserTracker userTracker,
                           MeshServiceController meshServiceController,
                           DiscoveryBroadcastEventHandler discoveryBroadcastEventHandler,
                           MeshSender meshSender,
                           InboundMeshMessageHandler inboundMeshMessageHandler,
                           CommandQueue commandQueue,
                           HashHelper hashHelper,
                           ChannelJsonHelper channelJsonHelper) {
        super(destroyables, sharedPreferences, hashHelper, channelJsonHelper);

        mDiscoveryBroadcastEventHandler = discoveryBroadcastEventHandler;

        mMessageQueueSize.setValue(0);
        mTotalMessages.setValue(0);
        mErroredMessages.setValue(0);
        mDeliveredMessages.setValue(0);
        mErrorsInARow.setValue(0);
        mTimedOutMessages.setValue(0);
        mReceivedMessages.setValue(0);

        userTracker.addUpdateListener(this);
        commandQueue.setListener(this);
        meshServiceController.addConnectionStateListener(this);
        meshSender.addMessageAckNackListener(this);
        inboundMeshMessageHandler.addMessageListener(this);
    }

    @Override
    public void onChannelMembersUpdated(List<UserInfo> atakUsers, List<TrackerUserInfo> trackers) {
        List<UserInfo> allStations = new ArrayList<>(atakUsers.size() + trackers.size());
        allStations.addAll(atakUsers);
        allStations.addAll(trackers);
        mUserInfoList.setValue(allStations);
    }

    @Override
    public void onMessageQueueSizeChanged(int size) {
        mMessageQueueSize.setValue(size);
    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {
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
    public LiveData<ConnectionState> getConnectionState() {
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

    public void broadcastDiscoveryMessage() {
        mDiscoveryBroadcastEventHandler.broadcastDiscoveryMessage(true);
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
        mTotalMessages.setValue(mTotalMessages.getValue() + 1);
        mReceivedMessages.setValue(mReceivedMessages.getValue() + 1);
    }
}
