package com.paulmandal.atak.forwarder.commhardware;

import android.content.Context;
import android.location.Location;
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
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GoTennaCommHardware implements CommHardware, GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener {
    private static final String TAG = "ATAKDBG." + GoTennaCommHardware.class.getSimpleName();

    public interface GroupListener { // TODO: refactor this
        void onUserDiscoveryBroadcast(String callsign, long gId, String atakUid);

        void onGroupCreated(long groupId, List<Long> memberGids);
    }

    private static final double FALLBACK_LATITUDE = Config.FALLBACK_LATITUDE;
    private static final double FALLBACK_LONGITUDE = Config.FALLBACK_LONGITUDE;

    private static final int MESSAGE_CHUNK_LENGTH = Config.MESSAGE_CHUNK_LENGTH;
    private static final int DELAY_BETWEEN_MESSAGES_MS = Config.DELAY_BETWEEN_MESSAGES_MS;

    private static final String BCAST_MARKER = "ATAKBCAST--"; // TODO: change back
    private static final long NO_GROUP = -1;

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;

    private String mCallsign;
    private long mGid;
    private String mAtakUid;
    long mGroupId = NO_GROUP;

    private boolean mConnected = false;
    private boolean mPendingMessage = false;
    private boolean mDestroyed = false;

    private List<Listener> mListeners = new CopyOnWriteArrayList<>();
    private GroupListener mGroupListener;
    private GroupTracker mGroupTracker;

    public GoTennaCommHardware(GroupListener userListener, GroupTracker groupTracker) {
        mGroupListener = userListener;
        mGroupTracker = groupTracker;
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

    @Override
    public void createGroup(List<Long> memberGids) {
        if (!mConnected) {
            Log.d(TAG, "createGroup: not connected yet");
            return;
        }
        memberGids.add(mGid);

        Log.d("GRP" + TAG, "  sending group creation request, member gids: " + memberGids);
        try {
            mGroupId = mGtCommandCenter.createGroupWithGIDs(memberGids,
                    (GTResponse gtResponse, long l) -> {
                        Log.d("GRP" + TAG, "    created group: " + gtResponse);
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
    public void sendMessage(byte[] message, String[] toUIDs) {
        if (!mConnected) {
            Log.d(TAG, "sendMessage: not connected yet");
            return;
        }

        if (mPendingMessage) {
            Log.d(TAG, "sendMessage: already sending message");
            return;
        }

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

        sendMessagesAsync(messages, toUIDs);
    }

    @Override
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void sendMessagesAsync(byte[][] messages, String[] toUIDs) {
        mPendingMessage = true;
        new Thread(() -> {
            Log.d(TAG, "  sending message async");
            for (int i = 0; i < messages.length; i++) {
                byte[] message = messages[i];
                // Transmit to GoTenna
                if (toUIDs == null) {
                    // Send message to group

                    GroupInfo groupInfo = mGroupTracker.getGroup();
                    if (groupInfo == null) {
                        Log.d(TAG, "  Tried to broadcast message without group, returning");
                        mPendingMessage = false;
                        return;
                    }

                    long groupId = groupInfo.groupId;

                    Log.d(TAG, "    sending chunk " + (i + 1) + "/" + messages.length + " to groupId: " + groupId + ", " + new String(message));

                    mGtCommandCenter.sendMessage(message,
                            groupId,
                            (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "      sendMessage response: " + gtSendMessageResponse.toString()),
                            (GTError gtError) -> Log.d(TAG, "      sendMessage error: " + gtError.toString()),
                            true);
                    sleepForMessageDelay();
                } else {
                    for (String uId : toUIDs) {
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

                        sleepForMessageDelay();

                        if (mDestroyed) {
                            break;
                        }
                    }
                }

                if (mDestroyed) {
                    break;
                }
            }
            mPendingMessage = false;
        }).start();
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
                // TODO: start scanning when DISCONNECTED afte previous connection?
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
            Log.d("GRP" + TAG, " group creation invite: " + gtGroupCreationMessageData.getGroupGID());
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
        mGroupListener.onUserDiscoveryBroadcast(callsign, gId, atakUid);
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
                            broadcastDiscoveryMessage(true);
                            mConnected = true;
                        } else {
                            Log.d(TAG, "Error setting GID");
                        }
                    },
                    (GTError error) -> Log.d(TAG, "Error setting frequencies: " + error.toString()));
        }).execute();
    }

    private void broadcastDiscoveryMessage(boolean retry) {
        // Broadcast our existence to the group
        String broadcastData = BCAST_MARKER + "," + mGid + "," + mAtakUid + "," + mCallsign;
        mGtCommandCenter.sendBroadcastMessage(broadcastData.getBytes(),
                (GTSendMessageResponse gtSendMessageResponse) -> {
                    Log.d(TAG, "    Sent callsign/gid: " + gtSendMessageResponse);
                },
                (GTError gtError) -> {
                    Log.d(TAG, "    Error sending initial broadcast: " + gtError);
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            if (retry) {
                                broadcastDiscoveryMessage(retry);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
    }

    private void sleepForMessageDelay() {
        try {
            Thread.sleep(DELAY_BETWEEN_MESSAGES_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
