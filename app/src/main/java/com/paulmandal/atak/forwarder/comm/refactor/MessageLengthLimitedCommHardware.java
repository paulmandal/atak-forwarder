package com.paulmandal.atak.forwarder.comm.refactor;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.geeksville.mesh.DataPacket;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.channel.UserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public abstract class MessageLengthLimitedCommHardware extends CommHardware {
    private static final String TAG = Constants.DEBUG_TAG_PREFIX + MessageLengthLimitedCommHardware.class.getSimpleName();

    private final UserTracker mUserTracker;
    private final int mMessageChunkLength;



    public MessageLengthLimitedCommHardware(List<Destroyable> destroyables,
                                            SharedPreferences sharedPreferences,
                                            String[] simplePreferencesKeys,
                                            String[] complexPreferencesKeys,
                                            Handler uiThreadHandler,
                                            CommandQueue commandQueue,
                                            QueuedCommandFactory queuedCommandFactory,
                                            UserTracker userTracker,
                                            int messageChunkLength,
                                            UserInfo selfInfo) {
        super(destroyables, sharedPreferences, simplePreferencesKeys, complexPreferencesKeys, uiThreadHandler, commandQueue, queuedCommandFactory, selfInfo);

        mUserTracker = userTracker;
        mMessageChunkLength = messageChunkLength;
    }

    @Override
    protected void handleSendMessage(SendMessageCommand sendMessageCommand) {
        sendMessageToUserOrGroup(sendMessageCommand);
    }


    /**
     * Message handling
     */
    protected void sendMessageToUserOrGroup(SendMessageCommand sendMessageCommand) {
        byte[] message = sendMessageCommand.message;
        String[] toUIDs = sendMessageCommand.toUIDs;
        // Check message length and break up if necessary




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

            Log.d(TAG, "---> sending chunk " + (i + 1) + "/" + messages.length + " to groupId: " + groupId + ", " + (new String(message).replace("\n", "").replace("\r", "")));

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
                String meshId = mUserTracker.getMeshIdForUid(uId);
                if (meshId.equals(UserTracker.USER_NOT_FOUND)) {
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
     * To be implemented by child classes
     */
    protected abstract boolean sendMessageSegment(byte[] message, String targetId);
}
