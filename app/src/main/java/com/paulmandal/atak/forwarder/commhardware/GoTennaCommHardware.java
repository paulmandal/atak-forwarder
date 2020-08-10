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
import com.paulmandal.atak.forwarder.MainActivity;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GoTennaCommHardware implements CommHardware, GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener {
    private static final String TAG = "ATAKDBG." + GoTennaCommHardware.class.getSimpleName();

    private static final long GOTENNA_LOCAL_GID = MainActivity.GOTENNA_LOCAL_GID;
    private static final long GOTENNA_REMOTE_GID = MainActivity.GOTENNA_REMOTE_GID;
    private static final double LATITUDE = MainActivity.LATITUDE;
    private static final double LONGITUDE = MainActivity.LONGITUDE;

    private static final int MAX_MESSAGE_LENGTH = MainActivity.MAX_MESSAGE_LENGTH;
    private static final int MESSAGE_CHUNK_LENGTH = MainActivity.MESSAGE_CHUNK_LENGTH;
    private static final int DELAY_BETWEEN_MESSAGES_MS = MainActivity.DELAY_BETWEEN_MESSAGES_MS;

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;

    private boolean mConnected = false;
    private boolean mPendingMessage = false;

    private List<Listener> mListeners = new CopyOnWriteArrayList<>();

    @Override
    public void init() {
        mGtConnectionManager = GTConnectionManager.getInstance();
        mGtCommandCenter = GTCommandCenter.getInstance();
        scanForGotenna(GTDeviceType.MESH);
    }

    @Override
    public void sendMessage(String message) {
        if (!mConnected) {
            Log.d(TAG, "sendMessage: not connected yet");
            return;
        }

        if (mPendingMessage) {
            Log.d(TAG, "sendMessage: already sending message");
            return;
        }

        // Check message length and break up if necessary
        List<String> messages = new ArrayList<>();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            int chunks = (int)Math.ceil((double)message.length() / (double) MESSAGE_CHUNK_LENGTH);
            for (int i = 0; i < chunks; i++) {
                String submessage = i + "/" + (chunks - 1) + "!" + message.substring(i * MESSAGE_CHUNK_LENGTH, Math.min((i + 1) * MESSAGE_CHUNK_LENGTH, message.length()));
                messages.add(submessage);
            }
        } else {
            messages.add("1/1!" + message);
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

    private void sendMessagesAsync(List<String> messages) {
        mPendingMessage = true;
        new Thread(() -> {
            Log.d(TAG, "  sending message async");
            for (String msg : messages) {
                // Transmit to GoTenna
                Log.d(TAG, "    sending chunk to: " + GOTENNA_REMOTE_GID + ", " + msg);
                mGtCommandCenter.sendMessage(msg.getBytes(),
                        GOTENNA_REMOTE_GID,
                        (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "  sendMessage response: " + gtSendMessageResponse.toString()),
                        (GTError gtError) -> Log.d(TAG, "  sendMessage error: " + gtError.toString()),
                        true);

                try {
                    Thread.sleep(DELAY_BETWEEN_MESSAGES_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        String messageChunk = new String(messageData.getDataToProcess());
        Log.d(TAG, "onIncomingMessage(GTMessageData messageData), msg: " + messageChunk);

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
            handleMessageChunk(messageChunk);
        } else if (gtBaseMessageData instanceof GTGroupCreationMessageData) {
            // Somebody invited us to a group!
            GTGroupCreationMessageData gtGroupCreationMessageData = (GTGroupCreationMessageData) gtBaseMessageData;
            Log.d(TAG, " group creation invite: " + gtGroupCreationMessageData.getGroupGID());
        }
    }

    private void handleMessageChunk(String messageChunk) {
        String[] messageHeaderSplit = messageChunk.split("!");
        String[] messageHeaderValues = messageHeaderSplit[0].split("/");
        int messageIndex = Integer.parseInt(messageHeaderValues[0]);
        int messageCount = Integer.parseInt(messageHeaderValues[1]);
        String chunk = messageHeaderSplit[1];

        handleMessageChunk(messageIndex, messageCount, chunk);
    }

    private void handleMessageChunk(int messageIndex, int messageCount, String messageChunk) {
        mIncomingMessages.add(new MessageChunk(messageIndex, messageCount, messageChunk));

        if (messageIndex == messageCount) {
            // Message complete!
            String[] messagePieces = new String[messageCount + 1];
            for (MessageChunk messagePiece : mIncomingMessages) {
                messagePieces[messagePiece.index] = messagePiece.chunk;
            }

            StringBuilder messageBuilder = new StringBuilder();

            for (String messagePiece : messagePieces) {
                messageBuilder.append(messagePiece);
            }

            mIncomingMessages = new ArrayList<>();
            notifyListeners(messageBuilder.toString());
        }
    }

    private void notifyListeners(String message) {
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
        public String chunk;

        public MessageChunk(int index, int count, String chunk) {
            this.index = index;
            this.count = count;
            this.chunk = chunk;
        }
    }

}
