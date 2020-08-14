package com.paulmandal.atak.forwarder.comm.commhardware;


import android.content.Context;
import android.location.Location;
import android.util.Base64;
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
import com.paulmandal.atak.forwarder.comm.interfaces.CommHardware;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GoTennaCommHardware implements CommHardware, GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener {
    private static final String TAG = "ATAKDBG." + GoTennaCommHardware.class.getSimpleName();

    public interface GroupListener {
        void onUserDiscoveryBroadcastReceived(String callsign, long gId, String atakUid);

        void onGroupCreated(long groupId, List<Long> memberGids);
    }

    private static final double FALLBACK_LATITUDE = Config.FALLBACK_LATITUDE;
    private static final double FALLBACK_LONGITUDE = Config.FALLBACK_LONGITUDE;

    private static final int MESSAGE_CHUNK_LENGTH = Config.MESSAGE_CHUNK_LENGTH;
    private static final int DELAY_BETWEEN_MESSAGES_MS = Config.DELAY_BETWEEN_MESSAGES_MS;
    private static final int DELAY_BETWEEN_POLLING_FOR_MESSAGES = Config.DELAY_BETWEEN_POLLING_FOR_MESSAGES;

    private static final String BCAST_MARKER = "ATAKBCAST";

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;

    private String mCallsign;
    private long mGid;
    private String mAtakUid;
    long mGroupId;

    private boolean mConnected = false;
    private boolean mDestroyed = false;

    private List<Listener> mListeners = new CopyOnWriteArrayList<>();
    private GroupListener mGroupListener;
    private GroupTracker mGroupTracker;
    private MessageQueue mMessageQueue;

    private Thread mMessageWorkerThread;

    public GoTennaCommHardware(GroupListener userListener,
                               GroupTracker groupTracker,
                               MessageQueue messageQueue) {
        mGroupListener = userListener;
        mGroupTracker = groupTracker;
        mMessageQueue = messageQueue;
    }

    @Override
    public void init(@NonNull Context context, @NonNull String callsign, long gId, String atakUid) {
        try {
            GoTenna.setApplicationToken(context, Config.GOTENNA_SDK_TOKEN);
        } catch (GTInvalidAppTokenException e) {
            e.printStackTrace();
        }

        mCallsign = callsign;
        mGid = gId;
        mAtakUid = atakUid;

        mGtConnectionManager = GTConnectionManager.getInstance();
        mGtCommandCenter = GTCommandCenter.getInstance();

        scanForGotenna(GTDeviceType.MESH);
    }

    @Override
    public void destroy() {
        mGtCommandCenter.setMessageListener(null);
        mGtConnectionManager.disconnect();
        mDestroyed = true;
    }

    @Override
    public void broadcastDiscoveryMessage() {
        broadcastDiscoveryMessage(false);
    }

    private void broadcastDiscoveryMessage(boolean initialDiscoveryMessage) {
        String broadcastData = BCAST_MARKER + "," + mGid + "," + mAtakUid + "," + mCallsign + "," + (initialDiscoveryMessage ? 1 : 0);
        mMessageQueue.queueMessage(null, broadcastData.getBytes(), null, MessageQueue.PRIORITY_HIGHEST, MessageQueue.QueuedMessage.XMIT_TYPE_BROADCAST, false);
    }

    @Override
    public void createGroup(List<Long> memberGids) {
        if (!mConnected) {
            Log.d(TAG, "createGroup: not connected yet");
            return;
        }
        memberGids.add(mGid);

        Log.d(TAG, "  sending group creation request, member gids: " + memberGids);
        try {
            mGroupId = mGtCommandCenter.createGroupWithGIDs(memberGids,
                    (GTResponse gtResponse, long l) -> {
                        Log.d(TAG, "    created group: " + gtResponse);
                        mGroupListener.onGroupCreated(mGroupId, memberGids);
                    },
                    (GTError gtError, long l) -> Log.d(TAG, "    error creating group: " + gtError));
        } catch (GTDataMissingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addToGroup(List<Long> allMemberGids, List<Long> newMemberGids) {
        if (!mConnected) {
            Log.d(TAG, "addToGroup: not connected yet");
            return;
        }

        allMemberGids.add(mGid);
        long groupId = mGroupTracker.getGroup().groupId;
        for (long newMemberGid : newMemberGids) {
            try {
                mGtCommandCenter.sendIndividualGroupInvite(groupId, allMemberGids, newMemberGid,
                        (GTResponse gtResponse, long l) -> Log.d(TAG, "  Success adding user to group: " + gtResponse),
                        (GTError gtError, long l) -> Log.d(TAG, "  Error inviting user to group: " + gtError));
            } catch (GTDataMissingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
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
            default:
                // TODO: start scanning when DISCONNECTED after previous connection?
                mConnected = false;
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
    private List<MessageChunk> mIncomingMessages = new ArrayList<>();

    @Override
    public void onIncomingMessage(GTMessageData messageData) {
        byte[] messagePayload = messageData.getDataToProcess();
        String messageString = new String(messagePayload);

        Log.d(TAG, "onIncomingMessage(GTMessageData messageData), msg: " + messageString);

        if (messageString.startsWith(BCAST_MARKER)) {
            handleBroadcast(messageString);
        } else {
            handleMessageChunk(messagePayload);
        }
    }

    @Override
    public void onIncomingMessage(GTBaseMessageData gtBaseMessageData) {
        Log.d(TAG, "onIncomingMessage(GTBaseMessageData gtBaseMessageData)");
        // This is where you would receive incoming messages that the SDK automatically knows how to parse
        // such as GTTextOnlyMessageData among the many other MessageData classes.
        if (gtBaseMessageData instanceof GTTextOnlyMessageData) {
            // Somebody sent us a message, try to parse it
            GTTextOnlyMessageData gtTextOnlyMessageData = (GTTextOnlyMessageData) gtBaseMessageData;
            String messageChunk = gtTextOnlyMessageData.getText();
            handleMessageChunk(messageChunk.getBytes());
        } else if (gtBaseMessageData instanceof GTGroupCreationMessageData) {
            // Somebody invited us to a group!
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

    private void handleMessageChunk(byte[] messageChunk) {
        int messageIndex = messageChunk[0] >> 4 & 0x0f;
        int messageCount = messageChunk[0] & 0x0f;

        byte[] chunk = new byte[messageChunk.length - 1];
        for (int idx = 0, i = 1; i < messageChunk.length; i++, idx++) {
            chunk[idx] = messageChunk[i];
        }
        handleMessageChunk(messageIndex, messageCount, chunk);
    }

    private void handleMessageChunk(int messageIndex, int messageCount, byte[] messageChunk) {
        mIncomingMessages.add(new MessageChunk(messageIndex, messageCount, messageChunk));

        if (messageIndex == messageCount - 1) {
            // Message complete!
            byte[][] messagePieces = new byte[messageCount][];
            int totalLength = 0;
            for (MessageChunk messagePiece : mIncomingMessages) {
                if (messagePiece.count > messageCount) {
                    // TODO: better handling for mis-ordered messages
                    continue;
                }
                messagePieces[messagePiece.index] = messagePiece.chunk;
                totalLength = totalLength + messagePiece.chunk.length;
            }

            byte[] message = new byte[totalLength];
            for (int idx = 0, i = 0; i < messagePieces.length; i++) {
                if (messagePieces[i] != null) {
                    for (int j = 0; j < messagePieces[i].length; j++, idx++) {
                        message[idx] = messagePieces[i][j];
                    }
                }
            }
            notifyListeners(message);
        }
    }

    private void notifyListeners(byte[] message) {
        Log.d("BASE64", "Incoming message: " + Base64.encodeToString(message, Base64.DEFAULT));
        for (Listener listener : mListeners) {
            listener.onMessageReceived(message);
        }
    }

    /**
     * GoTenna Connection Stuff
     */

    private void scanForGotenna(GTDeviceType deviceType) {
        try {
            mGtCommandCenter.setMessageListener(this);
            mGtConnectionManager.addGtConnectionListener(this);
            mGtConnectionManager.clearConnectedGotennaAddress();
            mGtConnectionManager.scanAndConnect(deviceType);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    private void onGoTennaConnected() {
        Log.d(TAG, "onGoTennaConnected");
        GTDeviceType deviceType = GTConnectionManager.getInstance().getDeviceType();

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
                            Log.d(TAG, "Setting GoTenna GID: " + mGid);
                            mGtCommandCenter.setGoTennaGID(mGid, mCallsign,
                                    (GTError gtError) -> {
                                        Log.d(TAG, " Error setting GoTenna ID");
                                        mConnected = false;
                                    });
                            mConnected = true;
                            broadcastDiscoveryMessage(true);
                            startWorkerThread();
                        } else {
                            Log.d(TAG, "Error setting GID");
                        }
                    },
                    (GTError error) -> Log.d(TAG, "Error setting frequencies: " + error.toString()));
        }).execute();
    }

    private void startWorkerThread() {
        mMessageWorkerThread = new Thread(() -> {
            while (!mDestroyed) {
                sleepForDelay(DELAY_BETWEEN_POLLING_FOR_MESSAGES);

                while (!mConnected) {
                    sleepForDelay(DELAY_BETWEEN_POLLING_FOR_MESSAGES);
                }

                MessageQueue.QueuedMessage queuedMessage = mMessageQueue.popHighestPriorityMessage();
                if (queuedMessage != null) {
                    Log.d(TAG, "got msg type: " + queuedMessage.xmitType);
                    if (queuedMessage.xmitType == MessageQueue.QueuedMessage.XMIT_TYPE_BROADCAST) {
                        broadcastMessage(queuedMessage.message);
                    } else {
                        sendMessage(queuedMessage.message, queuedMessage.toUIDs);
                    }
                }
            }
        });
        mMessageWorkerThread.start();
    }

    public void broadcastMessage(byte[] message) {
        mGtCommandCenter.sendBroadcastMessage(message,
                (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "    Sent callsign/gid: " + gtSendMessageResponse),
                (GTError gtError) -> Log.d(TAG, "    Error sending initial broadcast: " + gtError));

    }

    public void sendMessage(byte[] message, String[] toUIDs) {
        // Check message length and break up if necessary
        int chunks = (int) Math.ceil((double) message.length / (double) MESSAGE_CHUNK_LENGTH);

        if (chunks > 15) {
            Log.e(TAG, "Cannot break message into more than 15 pieces since we only have 1 byte for the header");
            return;
        }

        Log.d(TAG, "Message length: " + message.length + " chunks: " + chunks);
        Log.d("BASE64", "Message: " + Base64.encodeToString(message, Base64.DEFAULT));

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


            if (i > 0 && i % 4 == 0) {
                sleepForMessageDelay(i + 1);
            }
        }

        if (messages.length > 4) {
            sleepForMessageDelay(messages.length % 4);
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

                if (i > 0 && i % 4 == 0) {
                    sleepForMessageDelay(i + 1);
                }

                if (mDestroyed) {
                    return;
                }
            }

            if (messages.length > 4) {
                sleepForMessageDelay(messages.length % 4);
            }
        }
    }


    /**
     * Utils
     */
    private void sleepForMessageDelay(int messageCount) {
        sleepForDelay(DELAY_BETWEEN_MESSAGES_MS * messageCount);
    }

    private void sleepForDelay(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
