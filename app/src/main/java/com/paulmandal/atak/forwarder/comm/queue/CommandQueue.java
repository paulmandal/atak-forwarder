package com.paulmandal.atak.forwarder.comm.queue;

import android.os.Handler;

import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.queue.commands.CommandType;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.UpdateChannelCommand;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;

import java.util.ArrayList;
import java.util.List;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class CommandQueue {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + CommandQueue.class.getSimpleName();

    public interface Listener {
        void onMessageQueueSizeChanged(int size);
    }

    private Handler mHandler;

    private CotComparer mCotComparer;

    private final List<QueuedCommand> mQueuedCommands;
    private Listener mListener;

    public CommandQueue(Handler uiThreadHandler, CotComparer cotComparer) {
        mHandler = uiThreadHandler;
        mCotComparer = cotComparer;
        mQueuedCommands =  new ArrayList<>();
    }

    public void queueCommand(QueuedCommand commandToQueue) {
        if (commandToQueue instanceof SendMessageCommand) {
            throw new IllegalArgumentException("Use queueSendMessage() for SendMessageCommands");
        }

        synchronized (mQueuedCommands) {
            for (QueuedCommand queuedCommand : mQueuedCommands) {
                if (commandToQueue.commandType == queuedCommand.commandType) {
                    // Do not create duplicates for broadcasting discovery, and connect/disconnect from device
                    if (commandToQueue.commandType == CommandType.BROADCAST_DISCOVERY_MSG
                            || commandToQueue.commandType == CommandType.DISCONNECT_FROM_COMM_DEVICE
                            || commandToQueue.commandType == CommandType.SCAN_FOR_COMM_DEVICE) {
                        return;
                    }

                    if (commandToQueue.commandType == CommandType.UPDATE_CHANNEL) {
                        UpdateChannelCommand queuedCommandAsUpdateChannel = (UpdateChannelCommand) queuedCommand;
                        UpdateChannelCommand commandToQueueAsUpdateChannel = (UpdateChannelCommand) commandToQueue;

                        queuedCommandAsUpdateChannel.channelName = commandToQueueAsUpdateChannel.channelName;
                        queuedCommandAsUpdateChannel.psk = commandToQueueAsUpdateChannel.psk;
                        return;
                    }
                }
            }

            mQueuedCommands.add(commandToQueue);
        }
    }

    public void queueSendMessage(SendMessageCommand sendMessageCommand, boolean overwriteSimilar) {
        int messageQueueSize;
        synchronized (mQueuedCommands) {
            if (overwriteSimilar) {
                for (QueuedCommand queuedCommand : mQueuedCommands) {
                    if (queuedCommand instanceof SendMessageCommand) {
                        SendMessageCommand queuedSendMessageCommand = (SendMessageCommand)queuedCommand;
                        if (mCotComparer.areCotEventsEqual(sendMessageCommand.cotEvent, queuedSendMessageCommand.cotEvent)
                                || queuedSendMessageCommand.cotEvent.getType().equals(TYPE_PLI)
                                && mCotComparer.areUidsEqual(sendMessageCommand.toUIDs, queuedSendMessageCommand.toUIDs)) {
                            queuedSendMessageCommand.takeStateFrom(sendMessageCommand);
                            return;
                        }
                    }
                }
            }

            mQueuedCommands.add(sendMessageCommand);
            messageQueueSize = mQueuedCommands.size();
        }

        notifyListener(messageQueueSize);
    }

    @Nullable
    public QueuedCommand popHighestPriorityCommand(boolean isConnected) {
        QueuedCommand highestPriorityCommand = null;
        int messageQueueSize;
        synchronized (mQueuedCommands) {
            for (QueuedCommand queuedCommand : mQueuedCommands) {

                if (!isConnected && (queuedCommand.commandType == CommandType.BROADCAST_DISCOVERY_MSG
                        || queuedCommand.commandType == CommandType.SEND_TO_INDIVIDUAL
                        || queuedCommand.commandType == CommandType.SEND_TO_CHANNEL
                        || queuedCommand.commandType == CommandType.UPDATE_CHANNEL)) {
                    // Ignore commands that require connectivity
                    continue;
                }

                if (highestPriorityCommand == null
                        || queuedCommand.priority > highestPriorityCommand.priority
                        || (queuedCommand.priority == highestPriorityCommand.priority
                        && queuedCommand.queuedTime < highestPriorityCommand.queuedTime)) {
                    highestPriorityCommand = queuedCommand;
                }
            }

            if (highestPriorityCommand != null) {
                mQueuedCommands.remove(highestPriorityCommand);
            }

            messageQueueSize = mQueuedCommands.size();
        }

        notifyListener(messageQueueSize);

        return highestPriorityCommand;
    }

    public int getQueueSize() {
        synchronized (mQueuedCommands) {
            return mQueuedCommands.size();
        }
    }

    public void clearData() {
        int messageQueueSize;
        synchronized (mQueuedCommands) {
            mQueuedCommands.clear();
            messageQueueSize = mQueuedCommands.size();
        }
        notifyListener(messageQueueSize);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private void notifyListener(int messageQueueSize) {
        if (mListener != null) {
            mHandler.post(() -> mListener.onMessageQueueSizeChanged(messageQueueSize));
        }
    }
}
