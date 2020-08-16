package com.paulmandal.atak.forwarder.comm.queue;

import android.os.Handler;
import android.support.annotation.Nullable;

import com.paulmandal.atak.forwarder.comm.queue.commands.AddToGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.CommandType;
import com.paulmandal.atak.forwarder.comm.queue.commands.CreateGroupCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;

import java.util.ArrayList;
import java.util.List;

public class CommandQueue {
    private static final String TAG = "ATAKDBG." + CommandQueue.class.getSimpleName();

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
                    if (commandToQueue.commandType == CommandType.CREATE_GROUP) {
                        CreateGroupCommand queuedCommandAsCreateGroup = (CreateGroupCommand)queuedCommand;
                        CreateGroupCommand commandToQueueAsCreateGroup = (CreateGroupCommand)commandToQueue;

                        // Overwrite just in case anything changed
                        queuedCommandAsCreateGroup.memberGids = commandToQueueAsCreateGroup.memberGids;
                    }

                    if (commandToQueue.commandType == CommandType.ADD_TO_GROUP) {
                        AddToGroupCommand queuedCommandAsAddToGroup = (AddToGroupCommand)queuedCommand;
                        AddToGroupCommand commandToQueueAsAddToGroup = (AddToGroupCommand)commandToQueue;

                        // Overwrite just in case anything changed
                        queuedCommandAsAddToGroup.groupId = commandToQueueAsAddToGroup.groupId;
                        queuedCommandAsAddToGroup.allMemberGids = commandToQueueAsAddToGroup.allMemberGids;
                        queuedCommandAsAddToGroup.newMemberGids = commandToQueueAsAddToGroup.newMemberGids;
                    }
                }
            }
        }
    }

    public void queueSendMessage(SendMessageCommand sendMessageCommand, boolean overwriteSimilar) {
        int messageQueueSize = 0;
        synchronized (mQueuedCommands) {
            if (overwriteSimilar) {
                for (QueuedCommand queuedCommand : mQueuedCommands) {
                    if (queuedCommand instanceof SendMessageCommand) {
                        SendMessageCommand queuedSendMessageCommand = (SendMessageCommand)queuedCommand;
                        if (mCotComparer.areCotEventsEqual(sendMessageCommand.cotEvent, queuedSendMessageCommand.cotEvent)
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
    public QueuedCommand popHighestPriorityCommand(boolean isInGroup) {
        QueuedCommand highestPriorityCommand = null;
        int messageQueueSize = 0;
        synchronized (mQueuedCommands) {
            for (QueuedCommand queuedCommand : mQueuedCommands) {

                if ((queuedCommand.commandType == CommandType.SEND_TO_GROUP
                        && !isInGroup)) {
                    // Ignore group messages for now
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
        synchronized (mQueuedCommands) {
            mQueuedCommands.clear();
        }
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
