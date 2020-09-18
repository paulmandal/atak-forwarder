package com.paulmandal.atak.forwarder.comm.commhardware;

import android.os.Handler;
import android.util.Log;

import com.geeksville.mesh.DataPacket;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.channel.ChannelTracker;
import com.paulmandal.atak.forwarder.channel.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public abstract class MessageLengthLimitedCommHardware extends CommHardware {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MessageLengthLimitedCommHardware.class.getSimpleName();

    private ChannelTracker mChannelTracker;
    private final int mMessageChunkLength;

    private final Map<String, List<MessageChunk>> mIncomingMessages = new HashMap<>();

    public MessageLengthLimitedCommHardware(Handler uiThreadHandler, CommandQueue commandQueue, QueuedCommandFactory queuedCommandFactory, ChannelTracker channelTracker, int messageChunkLength, UserInfo selfInfo) {
        super(uiThreadHandler, commandQueue, queuedCommandFactory, selfInfo);

        mChannelTracker = channelTracker;
        mMessageChunkLength = messageChunkLength;
    }

    @Override
    protected void handleSendMessage(SendMessageCommand sendMessageCommand) {
        sendMessageToUserOrGroup(sendMessageCommand);
    }

    protected void handleMessageChunk(int messageId, String meshId, byte[] messageChunk) {
        int messageIndex = messageChunk[0] >> 4 & 0x0f;
        int messageCount = messageChunk[0] & 0x0f;

        Log.d(TAG, "<---  messageChunk: " + messageIndex + "/" + messageCount + " from: " + meshId);

        byte[] chunk = new byte[messageChunk.length - 1];
        for (int idx = 0, i = 1; i < messageChunk.length; i++, idx++) {
            chunk[idx] = messageChunk[i];
        }
        handleMessageChunk(messageId, meshId, messageIndex, messageCount, chunk);
    }

    private void handleMessageChunk(int messageId, String meshId, int messageIndex, int messageCount, byte[] messageChunk) {
        synchronized (mIncomingMessages) { // TODO: better sync block?
            List<MessageChunk> incomingMessagesFromUser = mIncomingMessages.get(meshId);
            if (incomingMessagesFromUser == null) {
                incomingMessagesFromUser = new ArrayList<>();
                mIncomingMessages.put(meshId, incomingMessagesFromUser);
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
                    if (messagePieces[i] == null) {
                        // We're missing a chunk of this message so we can't rebuild it
                        Log.e(TAG, "Missing chunk: " + (i + 1) + "/" + messagePieces.length);
                        return;
                    }
                    for (int j = 0; j < messagePieces[i].length; j++, idx++) {
                        message[idx] = messagePieces[i][j];
                    }
                }
                notifyMessageListeners(messageId, message);
            }
        }
    }

    /**
     * Message handling
     */
    protected void sendMessageToUserOrGroup(SendMessageCommand sendMessageCommand) {
        byte[] message = sendMessageCommand.message;
        String[] toUIDs = sendMessageCommand.toUIDs;
        // Check message length and break up if necessary
        int chunks = (int) Math.ceil((double) message.length / (double) mMessageChunkLength);

        if (chunks > 15) {
            Log.e(TAG, "Cannot break message into more than 15 pieces since we only have 1 byte for the header");
            return;
        }

        Log.d(TAG, "Message length: " + message.length + " chunks: " + chunks);

        byte[][] messages = new byte[chunks][];
        for (int i = 0; i < chunks; i++) {
            int start = i * mMessageChunkLength;
            int end = Math.min((i + 1) * mMessageChunkLength, message.length);
            int length = end - start;

            messages[i] = new byte[length + 1];
            messages[i][0] = (byte) (i << 4 | chunks);

            for (int idx = 1, j = start; j < end; j++, idx++) {
                messages[i][idx] = message[j];
            }
        }

        if (toUIDs == null) {
            sendMessagesToGroup(messages, sendMessageCommand.cotEvent.getType().equals(TYPE_PLI));
        } else {
            sendMessagesToUsers(messages, toUIDs);
        }
    }

    private void sendMessagesToGroup(byte[][] messages, boolean isPli) {
        for (int i = 0; i < messages.length; i++) {
            byte[] message = messages[i];
            String groupId = DataPacket.ID_BROADCAST;

            Log.d(TAG, "---> sending chunk " + (i + 1) + "/" + messages.length + " to groupId: " + groupId + ", " + new String(message));

            boolean messageSent = sendMessageSegment(message, groupId);

            if (!messageSent && isPli) {
                // Do not attempt to re-send PLIs
                return;
            }

            if (!messageSent) {
                i--;
            }

            if (isDestroyed()) {
                return;
            }
        }
    }

    private void sendMessagesToUsers(byte[][] messages, String[] toUIDs) {
        for (String uId : toUIDs) {
            for (int i = 0; i < messages.length; i++) {
                byte[] message = messages[i];
                String meshId = mChannelTracker.getMeshIdForUid(uId);
                if (meshId.equals(ChannelTracker.USER_NOT_FOUND)) {
                    Log.d(TAG, "msg can't find user: " + uId);
                    continue;
                }

                // Send message to individual
                Log.d(TAG, "--->  sending chunk " + (i + 1) + "/" + messages.length + " to individual: " + meshId + ", " + new String(message));

                if (!sendMessageSegment(message, meshId)) {
                    i--;
                }

                if (isDestroyed()) {
                    return;
                }
            }
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

    /**
     * To be implemented by child classes
     */
    protected abstract boolean sendMessageSegment(byte[] message, String targetId);
}
