package com.paulmandal.atak.forwarder.commhardware;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gotenna.sdk.connection.GTConnectionError;
import com.gotenna.sdk.connection.GTConnectionManager;
import com.gotenna.sdk.connection.GTConnectionState;
import com.gotenna.sdk.data.GTCommandCenter;
import com.gotenna.sdk.data.GTDeviceType;
import com.gotenna.sdk.data.GTError;
import com.gotenna.sdk.data.GTResponse;
import com.gotenna.sdk.data.GTSendMessageResponse;
import com.gotenna.sdk.data.Place;
import com.gotenna.sdk.data.messages.GTBaseMessageData;
import com.gotenna.sdk.data.messages.GTGroupCreationMessageData;
import com.gotenna.sdk.data.messages.GTMessageData;
import com.gotenna.sdk.data.messages.GTTextOnlyMessageData;
import com.gotenna.sdk.georegion.PlaceFinderTask;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GoTennaCommHardware implements CommHardware, GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener {
    private static final String TAG = "ATAKDBG." + GoTennaCommHardware.class.getSimpleName();

    private static final long GOTENNA_LOCAL_GID = Config.GOTENNA_LOCAL_GID;
    private static final long GOTENNA_REMOTE_GID = Config.GOTENNA_REMOTE_GID;
    private static final double LATITUDE = Config.LATITUDE;
    private static final double LONGITUDE = Config.LONGITUDE;

    private static final int MAX_MESSAGE_LENGTH = Config.MAX_MESSAGE_LENGTH;
    private static final int MESSAGE_CHUNK_LENGTH = Config.MESSAGE_CHUNK_LENGTH;
    private static final int DELAY_BETWEEN_MESSAGES_MS = Config.DELAY_BETWEEN_MESSAGES_MS;

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;

    private boolean mConnected = false;
    private boolean mPendingMessage = false;
    private Thread mThread;

    private List<Listener> mListeners = new CopyOnWriteArrayList<>();

    @Override
    public void init() {
        mGtConnectionManager = GTConnectionManager.getInstance();
        mGtCommandCenter = GTCommandCenter.getInstance();
        scanForGotenna(GTDeviceType.MESH);
    }

    @Override
    public void destroy() {
        mGtCommandCenter.setMessageListener(null);
        mGtConnectionManager.disconnect();
        mThread.stop();
    }

    @Override
    public void sendMessage(byte[] message) {
        if (!mConnected) {
            Log.d(TAG, "sendMessage: not connected yet");
            return;
        }

        if (mPendingMessage) {
            Log.d(TAG, "sendMessage: already sending message");
            return;
        }

        // Check message length and break up if necessary
        int chunks = (int)Math.ceil((double)message.length / (double) MESSAGE_CHUNK_LENGTH);

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
            messages[i][0] = (byte) (i << 4 | chunks - 1);

            Log.d(TAG, "  message: " + (i + 1) + " / " + chunks);
            Log.d(TAG, "  marker bytez: " + String.format("%8s", Integer.toBinaryString(messages[i][0])));
            for (int idx = 1, j = start; j < end; j++, idx++) {
                messages[i][idx] = message[j];
            }
        }

        sendMessagesAsync(messages);
    }

    @Override
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void sendMessagesAsync(byte[][] messages) {
        mPendingMessage = true;
        mThread = new Thread(() -> {
            Log.d(TAG, "  sending message async");
            for (int i = 0 ; i < messages.length ; i++) {
                byte[] message = messages[i];
                // Transmit to GoTenna
                Log.d(TAG, "    sending chunk " + (i + 1) + "/" + messages.length + " to: " + GOTENNA_REMOTE_GID + ", " + new String(message));
                mGtCommandCenter.sendMessage(message,
                        GOTENNA_REMOTE_GID,
                        (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "      sendMessage response: " + gtSendMessageResponse.toString()),
                        (GTError gtError) -> Log.d(TAG, "      sendMessage error: " + gtError.toString()),
                        true);

                try {
                    Thread.sleep(DELAY_BETWEEN_MESSAGES_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mPendingMessage = false;
        });
        mThread.start();
    }

    /**
     * GoTenna Connection Handling
     */
    @Override
    public void onConnectionStateUpdated(@NonNull GTConnectionState connectionState) {
        Log.d(TAG, "onConnectionStateUpdated: " + connectionState);
        switch (connectionState) {
            case CONNECTED:
                onGotennaConnected();
                break;
            default:
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
        byte[] messageChunk = messageData.getDataToProcess();
        Log.d(TAG, "onIncomingMessage(GTMessageData messageData), msg: " + new String(messageChunk));

        handleMessageChunk(messageChunk);
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
        }
    }

    private void handleMessageChunk(byte[] messageChunk) {
        Log.d(TAG, "handleMessageChunk byte: " + Integer.toBinaryString(messageChunk[0]));
        int messageIndex = messageChunk[0] >> 4 & 0x0f;
        int messageCount = messageChunk[0] & 0x0f;

        Log.d(TAG, "messageIndex: " + messageIndex + ", messageCount: " + messageCount);

        byte[] chunk = new byte[messageChunk.length - 1];
        for (int idx = 0, i = 1 ; i < messageChunk.length ; i++, idx++)
        {
            chunk[idx] = messageChunk[i];
        }
        handleMessageChunk(messageIndex, messageCount, chunk);
    }

    private void handleMessageChunk(int messageIndex, int messageCount, byte[] messageChunk) {
        mIncomingMessages.add(new MessageChunk(messageIndex, messageCount, messageChunk));

        if (messageIndex == messageCount) {
            // Message complete!
            byte[][] messagePieces = new byte[messageCount][];
            int totalLength = 0;
            for (MessageChunk messagePiece : mIncomingMessages) {
                messagePieces[messagePiece.index] = messagePiece.chunk;
                totalLength = totalLength + messagePiece.chunk.length;
            }

            Log.d(TAG, "handleMessageChunk, total length: " + totalLength);
            byte[] message = new byte[totalLength];
            for (int idx = 0, i = 0 ; i < messagePieces.length ; i++) {
                for (int j = 0 ; j < messagePieces[i].length ; j++, idx++) {
                    message[idx] = messagePieces[i][j];
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

    private void onGotennaConnected() {
        Log.d(TAG, "onGotennaConnected");
        GTDeviceType deviceType = GTConnectionManager.getInstance().getDeviceType();

        switch (deviceType) {
            case MESH:
                findAndSetMeshLocation();
                break;
        }
    }

    private void findAndSetMeshLocation() {
        // TODO: Use Android GPS to determine user's current location instead of hardcoding lat lng
        // TODO: Must use location to correctly set goTenna Mesh Device Frequency for the user's current location as per FCC rules
        Location location = new Location("Custom");
        location.setLatitude(LATITUDE);
        location.setLongitude(LONGITUDE);

        new PlaceFinderTask(location, (@NonNull Place place) -> {
            if (place == Place.UNKNOWN) {
                // Default to North America if we can't find the actual location
                place = Place.NORTH_AMERICA;
            }

            mGtCommandCenter.sendSetGeoRegion(place,
                    (GTResponse response) -> {
                        if (response.getResponseCode() == GTResponse.GTCommandResponseCode.POSITIVE) {
                            Log.d(TAG, "Setting GoTenna GID: " + GOTENNA_LOCAL_GID);
                            mGtCommandCenter.setGoTennaGID(GOTENNA_LOCAL_GID, "atak-poc-user",
                                    (GTError gtError) -> {
                                        Log.d(TAG, " Error setting GoTenna ID");
                                        mConnected = false;
                                    });
                            mConnected = true;
                        } else {
                            Log.d(TAG, "Error setting GID");
                        }
                    },
                    (GTError error) -> {
                        Log.d(TAG, "Error setting frequencies: " + error.toString());
                    });
        }).execute();
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
