package com.paulmandal.atak.forwarder.comm.commhardware;


import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.connection.GTConnectionError;
import com.gotenna.sdk.connection.GTConnectionManager;
import com.gotenna.sdk.connection.GTConnectionState;
import com.gotenna.sdk.data.GTCommandCenter;
import com.gotenna.sdk.data.GTDeviceType;
import com.gotenna.sdk.data.GTError;
import com.gotenna.sdk.data.GTResponse;
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
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoTennaCommHardware extends CommHardware implements GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener {
    private static final String TAG = "ATAKDBG." + GoTennaCommHardware.class.getSimpleName();

    public interface GroupListener {
        void onUserDiscoveryBroadcastReceived(String callsign, long gId, String atakUid);
        void onGroupCreated(long groupId, List<Long> memberGids);
    }

    private static final double FALLBACK_LATITUDE = Config.FALLBACK_LATITUDE;
    private static final double FALLBACK_LONGITUDE = Config.FALLBACK_LONGITUDE;

    private static final int SCAN_TIMEOUT_MS = Config.SCAN_TIMEOUT_MS;

    private static final int MESSAGE_CHUNK_LENGTH = Config.MESSAGE_CHUNK_LENGTH;
    private static final int QUOTA_REFRESH_TIME_MS = Config.QUOTA_REFRESH_TIME_MS;
    private static final int MESSAGES_PER_MINUTE = Config.MESSAGES_PER_MINUTE;

    private static final int DELAY_BETWEEN_MSGS_MS = 5000; // TODO: move this to Config

    private static final String BCAST_MARKER = "ATAKBCAST";

    private Handler mHandler;
    private GroupListener mGroupListener;
    private GroupTracker mGroupTracker;

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;
    int mUsedMessageQuota = 0;

    long mGroupId;

    private boolean mScanning = false;

    private Thread mQuotaWorkerThread;

    public GoTennaCommHardware(Handler handler,
                               GroupListener userListener,
                               GroupTracker groupTracker,
                               MessageQueue messageQueue) {
        super(messageQueue, groupTracker);
        mHandler = handler;
        mGroupListener = userListener;
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
        mGtConnectionManager.addGtConnectionListener(this);

        scanForGotenna(GTDeviceType.MESH);
    }


    @Override
    public void broadcastDiscoveryMessage() {
        broadcastDiscoveryMessage(false);
    }

    @Override
    public void createGroup(List<Long> memberGids) {
        if (!isConnected()) {
            Log.d(TAG, "createGroup: not connected yet");
            return;
        }
        memberGids.add(getSelfInto().gId);

        Log.d(TAG, "  sending group creation request, member gids: " + memberGids);
        try {
            mGroupId = mGtCommandCenter.createGroupWithGIDs(memberGids,
                    (GTResponse gtResponse, long l) -> {
                        Log.d(TAG, "    created group: " + gtResponse);
                        mGroupListener.onGroupCreated(mGroupId, memberGids);
                    },
                    (GTError gtError, long l) -> Log.d(TAG, "    error creating group: " + gtError));

            sleepForDelay(DELAY_BETWEEN_MSGS_MS);
            maybeSleepUntilQuotaRefresh();
        } catch (GTDataMissingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addToGroup(List<Long> allMemberGids, List<Long> newMemberGids) {
        if (!isConnected()) {
            Log.d(TAG, "addToGroup: not connected yet");
            return;
        }

        allMemberGids.add(getSelfInto().gId);
        long groupId = mGroupTracker.getGroup().groupId;
        for (long newMemberGid : newMemberGids) {
            try {
                mGtCommandCenter.sendIndividualGroupInvite(groupId, allMemberGids, newMemberGid,
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
    public void connect() {
        if (!isConnected() && !mScanning) {
            scanForGotenna(GTDeviceType.MESH);
        }
    }

    @Override
    public void forgetDevice() {
        mGtConnectionManager.disconnect();
        mGtConnectionManager.clearConnectedGotennaAddress();
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
                if (getScanListener() != null) {
                    getScanListener().onDeviceDisconnected();
                }
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
    private final Map<Long, List<MessageChunk>> mIncomingMessages = new HashMap<>();

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

    private void handleMessageChunk(long senderGid, byte[] messageChunk) {
        int messageIndex = messageChunk[0] >> 4 & 0x0f;
        int messageCount = messageChunk[0] & 0x0f;

        Log.d(TAG, "  messageChunk: " + messageIndex + "/" + messageCount + " from: " + senderGid);

        byte[] chunk = new byte[messageChunk.length - 1];
        for (int idx = 0, i = 1; i < messageChunk.length; i++, idx++) {
            chunk[idx] = messageChunk[i];
        }
        handleMessageChunk(senderGid, messageIndex, messageCount, chunk);
    }

    private void handleMessageChunk(long senderGid, int messageIndex, int messageCount, byte[] messageChunk) {
        synchronized (mIncomingMessages) { // TODO: better sync block?
            List<MessageChunk> incomingMessagesFromUser = mIncomingMessages.get(senderGid);
            if (incomingMessagesFromUser == null) {
                incomingMessagesFromUser = new ArrayList<>();
                mIncomingMessages.put(senderGid, incomingMessagesFromUser);
            }
            incomingMessagesFromUser.add(new MessageChunk(messageIndex, messageCount, messageChunk));

            if (messageIndex == messageCount - 1) {
                // Message complete!
                byte[][] messagePieces = new byte[messageCount][];
                int totalLength = 0;
                for (MessageChunk messagePiece : incomingMessagesFromUser) {
                    if (messagePiece.count > messageCount) {
                        // TODO: better handling for mis-ordered messages
                        continue;
                    }
                    messagePieces[messagePiece.index] = messagePiece.chunk;
                    totalLength = totalLength + messagePiece.chunk.length;
                }

                incomingMessagesFromUser.clear();

                byte[] message = new byte[totalLength];
                for (int idx = 0, i = 0; i < messagePieces.length; i++) {
                    if (messagePieces[i] != null) {
                        for (int j = 0; j < messagePieces[i].length; j++, idx++) {
                            message[idx] = messagePieces[i][j];
                        }
                    }
                }
                notifyMessageListeners(message);
            }
        }
    }

    /**
     * GoTenna Connection Stuff
     */
    private Runnable mScanTimeoutRunnable = () -> {
        mGtConnectionManager.disconnect();
        mScanning = false;
        if (getScanListener() != null) {
            getScanListener().onScanTimeout();
        }
    };

    private void scanForGotenna(GTDeviceType deviceType) {
        mScanning = true;
        if (getScanListener() != null) {
            getScanListener().onScanStarted();
        }
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
        Log.d(TAG, "onGoTennaConnected");
        if (getScanListener() != null) {
            getScanListener().onDeviceConnected();
        }
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
                            Log.d(TAG, "Setting GoTenna GID: " + getSelfInto().gId);
                            mGtCommandCenter.setGoTennaGID(getSelfInto().gId, getSelfInto().callsign,
                                    (GTError gtError) -> {
                                        Log.d(TAG, " Error setting GoTenna ID");
                                        setConnected(false);
                                    });
                            setConnected(true);
                            broadcastDiscoveryMessage(true);
                            startWorkerThreads();
                        } else {
                            Log.d(TAG, "Error setting GID");
                        }
                    },
                    (GTError error) -> Log.d(TAG, "Error setting frequencies: " + error.toString()));
        }).execute();
    }

    /**
     * Message handling
     */
    @Override
    protected void startWorkerThreads() {


        mQuotaWorkerThread = new Thread(() -> {
            while (!isDestroyed()) {
                mUsedMessageQuota = 0;
                sleepForDelay(QUOTA_REFRESH_TIME_MS);
            }
        });
        mQuotaWorkerThread.start();
    }

    @Override
    protected void broadcastMessage(byte[] message) {
        mGtCommandCenter.sendBroadcastMessage(message,
                (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "    Sent callsign/gid: " + gtSendMessageResponse),
                (GTError gtError) -> Log.d(TAG, "    Error sending initial broadcast: " + gtError));

        sleepForDelay(DELAY_BETWEEN_MSGS_MS);
        maybeSleepUntilQuotaRefresh();
    }

    @Override
    protected void sendMessage(byte[] message, String[] toUIDs) {
        // Check message length and break up if necessary
        int chunks = (int) Math.ceil((double) message.length / (double) MESSAGE_CHUNK_LENGTH);

        if (chunks > 15) {
            Log.e(TAG, "Cannot break message into more than 15 pieces since we only have 1 byte for the header");
            return;
        }

        Log.d(TAG, "Message length: " + message.length + " chunks: " + chunks);

        byte[][] messages = new byte[chunks][];
        for (int i = 0; i < chunks; i++) {
            int start = i * MESSAGE_CHUNK_LENGTH;
            int end = Math.min((i + 1) * MESSAGE_CHUNK_LENGTH, message.length);
            int length = end - start;

            messages[i] = new byte[length + 1];
            messages[i][0] = (byte) (i << 4 | chunks);

            for (int idx = 1, j = start; j < end; j++, idx++) {
                messages[i][idx] = message[j];
            }
        }

        if (toUIDs == null) {
            sendMessagesToGroup(messages);
        } else {
            sendMessagesToUsers(messages, toUIDs);
        }
    }

    private void sendMessagesToGroup(byte[][] messages) {
        GroupInfo groupInfo = mGroupTracker.getGroup();
        if (groupInfo == null) {
            Log.d(TAG, "  Tried to broadcast message without group, returning");
            return;
        }

        for (int i = 0; i < messages.length; i++) {
            byte[] message = messages[i];
            long groupId = groupInfo.groupId;

            Log.d(TAG, "    sending chunk " + (i + 1) + "/" + messages.length + " to groupId: " + groupId + ", " + new String(message));
            mGtCommandCenter.sendMessage(message,
                    groupId,
                    (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "      sendMessage response: " + gtSendMessageResponse.toString()),
                    (GTError gtError) -> Log.d(TAG, "      sendMessage error: " + gtError.toString()),
                    true);

            sleepForDelay(DELAY_BETWEEN_MSGS_MS);
            maybeSleepUntilQuotaRefresh();
        }
    }

    private void sendMessagesToUsers(byte[][] messages, String[] toUIDs) {
        for (String uId : toUIDs) {
            for (int i = 0; i < messages.length; i++) {
                byte[] message = messages[i];
                long gId = mGroupTracker.getGidForUid(uId);

                if (gId == GroupTracker.USER_NOT_FOUND) {
                    continue;
                }
                // Send message to individual
                Log.d(TAG, "    sending chunk " + (i + 1) + "/" + messages.length + " to individual: " + gId + ", " + new String(message));

                mGtCommandCenter.sendMessage(message,
                        gId,
                        (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "      sendMessage response: " + gtSendMessageResponse.toString()),
                        (GTError gtError) -> Log.d(TAG, "      sendMessage error: " + gtError.toString()),
                        true);

                sleepForDelay(DELAY_BETWEEN_MSGS_MS);
                maybeSleepUntilQuotaRefresh();

                if (isDestroyed()) {
                    return;
                }
            }
        }
    }

    /**
     * Peer discovery
     */
    private void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = BCAST_MARKER + "," + getSelfInto().gId + "," + getSelfInto().atakUid + "," + getSelfInto().callsign + "," + (initialDiscoveryMessage ? 1 : 0);
        queueMessage(null, broadcastData.getBytes(), null, MessageQueue.PRIORITY_HIGHEST, MessageQueue.QueuedMessage.XMIT_TYPE_BROADCAST, false);
    }

    /**
     * Utils
     */
    private void maybeSleepUntilQuotaRefresh() {
        mUsedMessageQuota++;
        if (mUsedMessageQuota == MESSAGES_PER_MINUTE) {
            sleepForDelay(QUOTA_REFRESH_TIME_MS);
        }
    }

    /**
     * Class Defs
     */
    private static class MessageChunk {
        public int index;
        public int count;
        public byte[] chunk;

        public MessageChunk(int index, int count, byte[] chunk) {
            this.index = index;
            this.count = count;
            this.chunk = chunk;
        }
    }

}
