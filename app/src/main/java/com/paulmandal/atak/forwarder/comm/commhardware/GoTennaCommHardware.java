package com.paulmandal.atak.forwarder.comm.commhardware;


import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.connection.GTConnectionError;
import com.gotenna.sdk.connection.GTConnectionManager;
import com.gotenna.sdk.connection.GTConnectionState;
import com.gotenna.sdk.data.BatteryStateListener;
import com.gotenna.sdk.data.GTCommandCenter;
import com.gotenna.sdk.data.GTDeviceType;
import com.gotenna.sdk.data.GTError;
import com.gotenna.sdk.data.GTErrorListener;
import com.gotenna.sdk.data.GTResponse;
import com.gotenna.sdk.data.GTSendCommandResponseListener;
import com.gotenna.sdk.data.GTSendMessageResponse;
import com.gotenna.sdk.data.Place;
import com.gotenna.sdk.data.groups.GroupMemberInfo;
import com.gotenna.sdk.data.messages.GTBaseMessageData;
import com.gotenna.sdk.data.messages.GTGroupCreationMessageData;
import com.gotenna.sdk.data.messages.GTMessageData;
import com.gotenna.sdk.data.messages.GTTextOnlyMessageData;
import com.gotenna.sdk.exceptions.GTDataMissingException;
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.gotenna.sdk.georegion.PlaceFinderTask;
import com.gotenna.sdk.responses.SystemInfoResponseData;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.AddToGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.CreateGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GoTennaCommHardware extends MessageLengthLimitedCommHardware implements GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener, BatteryStateListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + GoTennaCommHardware.class.getSimpleName();

    public interface GroupListener {
        void onUserDiscoveryBroadcastReceived(String callsign, long gId, String atakUid);
        void onGroupCreated(long groupId, List<Long> memberGids);
    }

    private static final long BROADCAST_MESSAGE = -1;

    private static final double FALLBACK_LATITUDE = Config.FALLBACK_LATITUDE;
    private static final double FALLBACK_LONGITUDE = Config.FALLBACK_LONGITUDE;

    private static final int SCAN_TIMEOUT_MS = Config.SCAN_TIMEOUT_MS;

    private static final int QUOTA_REFRESH_TIME_MS = Config.QUOTA_REFRESH_TIME_MS;
    private static final int MESSAGES_PER_MINUTE = Config.MESSAGES_PER_MINUTE;

    private static final int DELAY_BETWEEN_MSGS_MS = 5000; // TODO: move this to Config

    private Handler mHandler;
    private GroupListener mGroupListener;
    private GroupTracker mGroupTracker;

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;
    int mUsedMessageQuota = 0;

    private Integer mBatteryChargePercentage;
    private boolean mIsBatteryCharging = false;

    private boolean mScanning = false;

    public GoTennaCommHardware(Handler handler,
                               GroupListener groupListener,
                               GroupTracker groupTracker,
                               CommandQueue commandQueue,
                               QueuedCommandFactory queuedCommandFactory) {
        super(handler, commandQueue, queuedCommandFactory, groupTracker);
        mHandler = handler;
        mGroupListener = groupListener;
        mGroupTracker = groupTracker;
    }

    @Override
    public void init(@NonNull Context context, @NonNull String callsign, long gId, String atakUid) {
        super.init(context, callsign, gId, atakUid);
        try {
            GoTenna.setApplicationToken(context, Config.GOTENNA_SDK_TOKEN);
        } catch (GTInvalidAppTokenException e) {
            e.printStackTrace();
        }
        mGtConnectionManager = GTConnectionManager.getInstance();
        mGtCommandCenter = GTCommandCenter.getInstance();

        mGtCommandCenter.setMessageListener(this);
        mGtCommandCenter.addBatteryStateListener(this);
        mGtConnectionManager.addGtConnectionListener(this);

        scanForGotenna(GTDeviceType.MESH);
    }

    @Override
    protected void handleScanForCommDevice() {
        if (!isConnected() && !mScanning) {
            scanForGotenna(GTDeviceType.MESH);
        }
    }

    @Override
    protected void handleDisconnectFromCommDevice() {
        mGtConnectionManager.disconnect();
        mGtConnectionManager.clearConnectedGotennaAddress();
    }

    @Override
    protected void handleBroadcastDiscoveryMessage(BroadcastDiscoveryCommand broadcastDiscoveryCommand) {
        if (!sendMessageSegment(broadcastDiscoveryCommand.discoveryMessage, BROADCAST_MESSAGE)) {
            // Send this message back to the queue
            queueCommand(broadcastDiscoveryCommand);
        }
    }

    long mGroupId_DoNotUse;

    @Override
    protected void handleCreateGroup(CreateGroupCommand createGroupCommand) {
        createGroupCommand.memberGids.add(getSelfInfo().gId);

        Log.d(TAG, "  sending group creation request, member gids: " + createGroupCommand.memberGids);
        try {
            // TODO: error handling
            mGroupId_DoNotUse = mGtCommandCenter.createGroupWithGIDs(createGroupCommand.memberGids,
                    (GTResponse gtResponse, long l) -> {
                        Log.d(TAG, "    created group: " + gtResponse);
                        mGroupListener.onGroupCreated(mGroupId_DoNotUse, createGroupCommand.memberGids);
                    },
                    (GTError gtError, long l) -> Log.d(TAG, "    error creating group: " + gtError));

            sleepForDelay(DELAY_BETWEEN_MSGS_MS);
            maybeSleepUntilQuotaRefresh();
        } catch (GTDataMissingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleAddToGroup(AddToGroupCommand addToGroupCommand) {
        addToGroupCommand.allMemberGids.add(getSelfInfo().gId);
        long groupId = mGroupTracker.getGroup().groupId;
        for (long newMemberGid : addToGroupCommand.newMemberGids) {
            try {
                // TODO: error handling
                mGtCommandCenter.sendIndividualGroupInvite(groupId, addToGroupCommand.allMemberGids, newMemberGid,
                        (GTResponse gtResponse, long l) -> Log.d(TAG, "  Success adding user to group: " + gtResponse),
                        (GTError gtError, long l) -> Log.d(TAG, "  Error inviting user to group: " + gtError));

                sleepForDelay(DELAY_BETWEEN_MSGS_MS);
                maybeSleepUntilQuotaRefresh();
            } catch (GTDataMissingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void handleGetBatteryStatus() {
        mGtCommandCenter.sendGetSystemInfo((SystemInfoResponseData systemInfoResponseData) -> {
                    mBatteryChargePercentage = systemInfoResponseData.getBatteryLevelAsPercentage();
                    notifyGotBatteryInfo(mBatteryChargePercentage);
                },
                (GTError gtError) -> Log.d(TAG, "Error getting system info: " + gtError));
    }

    @Override
    public boolean isBatteryCharging() {
        return mIsBatteryCharging;
    }

    @Override
    public Integer getBatteryChargePercentage() {
        return mBatteryChargePercentage;
    }

    @Override
    public void destroy() {
        super.destroy();
        mGtCommandCenter.setMessageListener(null);
        mGtConnectionManager.disconnect();
    }

    /**
     * GoTenna Connection Handling
     */
    @Override
    public void onConnectionStateUpdated(@NonNull GTConnectionState connectionState) {
        Log.d(TAG, "onConnectionStateUpdated: " + connectionState);
        switch (connectionState) {
            case CONNECTED:
                onGoTennaConnected();
                break;
            case CONNECTING:
                mHandler.removeCallbacks(mScanTimeoutRunnable);
                break;
            case SCANNING:
            case DISCONNECTED:
                setConnected(false);
                notifyConnectionStateListeners(ConnectionState.DISCONNECTED);
                break;
            default:
                setConnected(false);
                break;
        }
    }

    @Override
    public void onConnectionError(@NonNull GTConnectionState connectionState, @NonNull GTConnectionError error) {
        Log.e(TAG, "Error connecting to GoTenna: " + error.getDetailString());
    }

    /**
     * GoTenna Message Handling
     */
    @Override
    public void onIncomingMessage(GTMessageData messageData) {
        byte[] messagePayload = messageData.getDataToProcess();
        String messageString = new String(messagePayload);

        Log.d(TAG, "onIncomingMessage(GTMessageData messageData), msg: " + messageString);

        if (messageString.startsWith(BCAST_MARKER)) {
            handleBroadcast(messageString);
        } else {
            handleMessageChunk(messageData.getSenderGID(), messagePayload);
        }
    }

    @Override
    public void onIncomingMessage(GTBaseMessageData gtBaseMessageData) {
        Log.e(TAG, "onIncomingMessage(GTBaseMessageData gtBaseMessageData) actually fired! Keep this code");
        Log.e(TAG, "gtMessageData type: " + gtBaseMessageData.getClass().getSimpleName());
        if (gtBaseMessageData instanceof GTTextOnlyMessageData) {
            // TODO: remove this? we don't use it
            GTTextOnlyMessageData gtTextOnlyMessageData = (GTTextOnlyMessageData) gtBaseMessageData;
            String messageChunk = gtTextOnlyMessageData.getText();
            handleMessageChunk(gtTextOnlyMessageData.getSenderGID(), messageChunk.getBytes());
        } else if (gtBaseMessageData instanceof GTGroupCreationMessageData) {
            GTGroupCreationMessageData gtGroupCreationMessageData = (GTGroupCreationMessageData) gtBaseMessageData;
            Log.d(TAG, " group creation invite: " + gtGroupCreationMessageData.getGroupGID());
            List<GroupMemberInfo> groupMemberInfoList = gtGroupCreationMessageData.getGroupMembersInfo();
            List<Long> groupMemberIds = new ArrayList<>(groupMemberInfoList.size());
            for (GroupMemberInfo groupMemberInfo : groupMemberInfoList) {
                groupMemberIds.add(groupMemberInfo.getGID());
            }
            mGroupListener.onGroupCreated(gtGroupCreationMessageData.getGroupGID(), groupMemberIds);
        }
    }

    @Override
    public void onBatteryStateChanged(boolean isBatteryCharging) {
        mIsBatteryCharging = isBatteryCharging;
        notifyBatteryChargeStateChanged(isBatteryCharging);
    }

    private void handleBroadcast(String message) {
        String messageWithoutMarker = message.replace(BCAST_MARKER + ",", "");
        String[] messageSplit = messageWithoutMarker.split(",");
        long gId = Long.parseLong(messageSplit[0]);
        String atakUid = messageSplit[1];
        String callsign = messageSplit[2];
        boolean initialDiscoveryMessage = messageSplit[3].equals("1");

        if (initialDiscoveryMessage) {
            broadcastDiscoveryMessage(false);
        }
        mGroupListener.onUserDiscoveryBroadcastReceived(callsign, gId, atakUid);
    }


    /**
     * GoTenna Connection Stuff
     */
    private Runnable mScanTimeoutRunnable = () -> {
        mGtConnectionManager.disconnect();
        mScanning = false;
        notifyConnectionStateListeners(ConnectionState.TIMEOUT);
    };

    private void scanForGotenna(GTDeviceType deviceType) {
        mScanning = true;
        notifyConnectionStateListeners(ConnectionState.SCANNING);
        try {
            mGtConnectionManager.scanAndConnect(deviceType);
            mHandler.postDelayed(mScanTimeoutRunnable, SCAN_TIMEOUT_MS);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    private void onGoTennaConnected() {
        mScanning = false;
        mHandler.removeCallbacks(mScanTimeoutRunnable);
        notifyConnectionStateListeners(ConnectionState.CONNECTED);
        GTDeviceType deviceType = mGtConnectionManager.getDeviceType();

        switch (deviceType) {
            case MESH:
                findAndSetMeshLocation();
                break;
        }
    }

    private void findAndSetMeshLocation() {
        Location location = new Location("Custom");
        location.setLatitude(FALLBACK_LATITUDE);
        location.setLongitude(FALLBACK_LONGITUDE);

        new PlaceFinderTask(location, (@NonNull Place place) -> {
            if (place == Place.UNKNOWN) {
                // Default to North America if we can't find the actual location
                place = Place.NORTH_AMERICA;
            }

            mGtCommandCenter.sendSetGeoRegion(place,
                    (GTResponse response) -> {
                        if (response.getResponseCode() == GTResponse.GTCommandResponseCode.POSITIVE) {
                            Log.d(TAG, "Setting GoTenna GID: " + getSelfInfo().gId);
                            mGtCommandCenter.setGoTennaGID(getSelfInfo().gId, getSelfInfo().callsign,
                                    (GTError gtError) -> {
                                        Log.d(TAG, " Error setting GoTenna ID");
                                        setConnected(false);
                                    });
                            setConnected(true);
                            broadcastDiscoveryMessage(true); // TODO: maybe don't send this every time we reconnect to the device?
                            if (mBatteryChargePercentage == null) {
                                requestBatteryStatus();
                            }
                        } else {
                            Log.d(TAG, "Error setting GID");
                        }
                    },
                    (GTError error) -> Log.d(TAG, "Error setting frequencies: " + error.toString()));
        }).execute();
    }

    /**
     * Pending Message / Retry Stuff
     */
    // TODO: refactor this into its own class
    private CountDownLatch mPendingMessageCountdownLatch;
    private String mMessageSuccessPrefix;
    private String mMessageErrorPrefix;

    @Nullable
    private GTResponse mLastSentMessageResponse;

    @Nullable
    private GTError mLastSentMessageError;

    private GTSendCommandResponseListener mGtSendCommandResponseListener = (GTSendMessageResponse gtSendMessageResponse) -> {
        Log.d(TAG, mMessageSuccessPrefix + gtSendMessageResponse);
        handleMessageResponse(gtSendMessageResponse);
    };

    private GTErrorListener mGtErrorListener =  (GTError gtError) -> {
        Log.d(TAG, mMessageErrorPrefix + gtError);
        handleMessageResponse(gtError);
    };

    private void prepareToSendMessage() {
        mPendingMessageCountdownLatch = new CountDownLatch(1);
        mLastSentMessageResponse = null;
        mLastSentMessageError = null;
    }

    private void handleMessageResponse(GTResponse gtSendMessageResponse) {
        handleMessageResponse(gtSendMessageResponse, null);
    }

    private void handleMessageResponse(GTError gtError) {
        handleMessageResponse(null, gtError);
    }

    private void handleMessageResponse(@Nullable GTResponse gtSendMessageResponse, @Nullable GTError gtError) {
        mLastSentMessageResponse = gtSendMessageResponse;
        mLastSentMessageError = gtError;
        mPendingMessageCountdownLatch.countDown();
    }

    protected boolean sendMessageSegment(byte[] message, long targetId) {
        boolean messageSentSuccessfully = true;

        prepareToSendMessage();

        if (targetId == BROADCAST_MESSAGE) {
            mMessageErrorPrefix =  "    Error sending initial broadcast: ";
            mMessageSuccessPrefix = "    Sent callsign/gid: ";
            mGtCommandCenter.sendBroadcastMessage(message, mGtSendCommandResponseListener, mGtErrorListener);
        } else {
            mMessageSuccessPrefix = "      sendMessage response: ";
            mMessageErrorPrefix = "      sendMessage error: ";
            if (message == null) {
                Log.e(TAG, " null message in sendMessageSegment!");
                new Exception().printStackTrace();
            }
            mGtCommandCenter.sendMessage(message, targetId, mGtSendCommandResponseListener, mGtErrorListener, true);
        }

        awaitPendingMessageCountDownLatch();

        if (mLastSentMessageResponse == null || mLastSentMessageResponse.getResponseCode() != GTResponse.GTCommandResponseCode.POSITIVE) {
            // TODO: check what the response is more fully, in some cases we do not want to retry
            Log.d(TAG, "sendMessageSegment failure, resending");
            Log.d(TAG, "    lastSentMessage responseCode: " + (mLastSentMessageResponse == null ? "null" : mLastSentMessageResponse.getResponseCode()));
            Log.d(TAG, "    lastSentMessage error: " + (mLastSentMessageError == null ? "null" : mLastSentMessageError));

            if (mLastSentMessageError != null && mLastSentMessageError.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED) {
                mUsedMessageQuota = MESSAGES_PER_MINUTE - 1;
            }

            if (mLastSentMessageResponse != null && mLastSentMessageResponse.getResponseCode() == GTResponse.GTCommandResponseCode.NEGATIVE) {
                // TODO: find out what can cause this to happen so we know what to do to remedy it
                Log.d(TAG, "Got ResponseCode NEGATIVE");
                sleepForDelay(DELAY_BETWEEN_MSGS_MS);
            }

            messageSentSuccessfully = false;
        }

        maybeSleepUntilQuotaRefresh();

        return messageSentSuccessfully;
    }

    /**
     * Utils
     */
    private void maybeSleepUntilQuotaRefresh() {
        mUsedMessageQuota++;
        if (mUsedMessageQuota == MESSAGES_PER_MINUTE) {
            sleepForDelay(QUOTA_REFRESH_TIME_MS);
            mUsedMessageQuota = 0;
        }
    }

    private void awaitPendingMessageCountDownLatch() {
        try {
            mPendingMessageCountdownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
