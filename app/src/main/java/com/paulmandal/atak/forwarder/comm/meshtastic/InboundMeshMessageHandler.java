package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.geeksville.mesh.DataPacket;
import com.geeksville.mesh.Portnums;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class InboundMeshMessageHandler extends MeshEventHandler {
    public interface MessageListener {
        void onMessageReceived(int messageId, byte[] message);
    }

    public static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + InboundMeshMessageHandler.class.getSimpleName();
    private final Handler mUiThreadHandler;

    private final Map<String, List<MessageChunk>> mIncomingMessages = new HashMap<>();
    private final Set<MessageListener> mMessageListeners = new CopyOnWriteArraySet<>();

    public InboundMeshMessageHandler(Context atakContext,
                                     List<Destroyable> destroyables,
                                     ConnectionStateHandler connectionStateHandler,
                                     Handler uiThreadHandler,
                                     Logger logger) {
        super(atakContext,
                logger,
                new String[] {
                        MeshServiceConstants.ACTION_RECEIVED_ATAK_FORWARDER
                },
                destroyables,
                connectionStateHandler);

        mUiThreadHandler = uiThreadHandler;
    }

    public void addMessageListener(MessageListener listener) {
        mMessageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        mMessageListeners.remove(listener);
    }

    @Override
    protected void handleReceive(Context context, Intent intent) {
        DataPacket payload = intent.getParcelableExtra(MeshServiceConstants.EXTRA_PAYLOAD);

        int dataType = payload.getDataType();

        if (dataType == Portnums.PortNum.ATAK_FORWARDER.getNumber()) {
            String message = new String(payload.getBytes());
            if (!message.substring(1).startsWith(ForwarderConstants.DISCOVERY_BROADCAST_MARKER)) {
                mLogger.i(TAG, "<--- Received packet: " + (message.replace("\n", "").replace("\r", "")));
                handleMessageChunk(payload.getId(), payload.getFrom(), payload.getBytes());
            }
        }
    }

    private void handleMessageChunk(int messageId, String meshId, byte[] messageChunk) {
        int messageIndex = messageChunk[0] >> 4 & 0x0f;
        int messageCount = messageChunk[0] & 0x0f;

        mLogger.i(TAG, "        messageChunk: " + (messageIndex + 1) + "/" + messageCount + " from: " + meshId);

        byte[] chunk = new byte[messageChunk.length - 1];
        for (int idx = 0, i = 1; i < messageChunk.length; i++, idx++) {
            chunk[idx] = messageChunk[i];
        }
        handleMessageChunk(messageId, meshId, messageIndex, messageCount, chunk);
    }

    private void handleMessageChunk(int messageId, String meshId, int messageIndex, int messageCount, byte[] messageChunk) {
        List<MessageChunk> incomingMessagesFromUser;
        synchronized (mIncomingMessages) {
            incomingMessagesFromUser = mIncomingMessages.get(meshId);
            if (incomingMessagesFromUser == null) {
                incomingMessagesFromUser = new ArrayList<>();
                mIncomingMessages.put(meshId, incomingMessagesFromUser);
            }
            incomingMessagesFromUser.add(new MessageChunk(messageIndex, messageCount, messageChunk));
        }

        synchronized (incomingMessagesFromUser) {
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
                        mLogger.e(TAG, "Missing chunk: " + (i + 1) + "/" + messagePieces.length);
                        return;
                    }
                    for (int j = 0; j < messagePieces[i].length; j++, idx++) {
                        message[idx] = messagePieces[i][j];
                    }
                }

                mLogger.i(TAG, "Message re-assembled, notifying listeners");
                notifyMessageListeners(messageId, message);
            }
        }
    }

    private void notifyMessageListeners(int messageId, byte[] message) {
        for (MessageListener listener : mMessageListeners) {
            mUiThreadHandler.post(() -> listener.onMessageReceived(messageId, message));
        }
    }
}
