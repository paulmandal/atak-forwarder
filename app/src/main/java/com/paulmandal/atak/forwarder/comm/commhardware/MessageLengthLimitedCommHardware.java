package com.paulmandal.atak.forwarder.comm.commhardware;

import android.os.Handler;
import android.util.Log;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.group.GroupInfo;
import com.paulmandal.atak.forwarder.group.GroupTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MessageLengthLimitedCommHardware extends CommHardware {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + MessageLengthLimitedCommHardware.class.getSimpleName();

    private static final int MESSAGE_CHUNK_LENGTH = Config.MESSAGE_CHUNK_LENGTH;

    private GroupTracker mGroupTracker;

    private final Map<Long, List<MessageChunk>> mIncomingMessages = new HashMap<>();

    public MessageLengthLimitedCommHardware(Handler uiThreadHandler, CommandQueue commandQueue, QueuedCommandFactory queuedCommandFactory, GroupTracker groupTracker) {
        super(uiThreadHandler, commandQueue, queuedCommandFactory, groupTracker);

        mGroupTracker = groupTracker;
    }

    @Override
    protected void handleSendMessage(SendMessageCommand sendMessageCommand) {
        sendMessageToUserOrGroup(sendMessageCommand);
    }

    protected void handleMessageChunk(long senderGid, byte[] messageChunk) {
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
                    if (messagePieces[i] == null) {
                        // We're missing a chunk of this message so we can't rebuild it
                        Log.e(TAG, "Missing chunk: " + (i + 1) + "/" + messagePieces.length);
                        return;
                    }
                    for (int j = 0; j < messagePieces[i].length; j++, idx++) {
                        message[idx] = messagePieces[i][j];
                    }
                }
                notifyMessageListeners(message);
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

            if (!sendMessageSegment(message, groupId)) {
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
                long gId = mGroupTracker.getGidForUid(uId);

                if (gId == GroupTracker.USER_NOT_FOUND) {
                    continue;
                }
                // Send message to individual
                Log.d(TAG, "    sending chunk " + (i + 1) + "/" + messages.length + " to individual: " + gId + ", " + new String(message));

                if (!sendMessageSegment(message, gId)) {
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
    protected abstract boolean sendMessageSegment(byte[] message, long targetId);
}
