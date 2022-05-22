package com.paulmandal.atak.forwarder.comm.queue;

import android.os.Handler;

import androidx.annotation.Nullable;

import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.MessageType;
import com.paulmandal.atak.forwarder.comm.queue.commands.CommandType;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CommandQueue {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + CommandQueue.class.getSimpleName();

    public interface Listener {
        void onMessageQueueSizeChanged(int size);
    }

    private final Handler mHandler;

    private final CotComparer mCotComparer;

    private final List<QueuedCommand> mQueuedCommands;
    private Listener mListener;

    private CountDownLatch mCountDownLatch;

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
                    // Do not create duplicates for broadcasting discovery
                    if (commandToQueue.commandType == CommandType.BROADCAST_DISCOVERY_MSG) {
                        return;
                    }
                }
            }

            mQueuedCommands.add(commandToQueue);
        }

        releaseLatch();
    }

    public void queueSendMessage(SendMessageCommand sendMessageCommand, boolean overwriteSimilar) {
        int messageQueueSize;
        synchronized (mQueuedCommands) {
            if (overwriteSimilar) {
                for (QueuedCommand queuedCommand : mQueuedCommands) {
                    if (queuedCommand instanceof SendMessageCommand) {
                        SendMessageCommand queuedSendMessageCommand = (SendMessageCommand)queuedCommand;
                        if (mCotComparer.areCotEventsEqual(sendMessageCommand.cotEvent, queuedSendMessageCommand.cotEvent)
                                || MessageType.fromCotEventType(queuedSendMessageCommand.cotEvent.getType()) == MessageType.PLI
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

        releaseLatch();

        notifyListener(messageQueueSize);
    }

    @Nullable
    public QueuedCommand popHighestPriorityCommand(boolean isConnected) {
        // All commands currently require connectivity
        if (!isConnected) {
            return null;
        }

        QueuedCommand highestPriorityCommand = null;
        int messageQueueSize = 0;
        boolean messageQueueSizeChanged = false;
        synchronized (mQueuedCommands) {
            for (QueuedCommand queuedCommand : mQueuedCommands) {

//                if (!isConnected && (queuedCommand.commandType == CommandType.BROADCAST_DISCOVERY_MSG
//                        || queuedCommand.commandType == CommandType.SEND_TO_INDIVIDUAL
//                        || queuedCommand.commandType == CommandType.SEND_TO_CHANNEL)) {
//                    // Ignore commands that require connectivity
//                    continue;
//                }
//
                if (highestPriorityCommand == null
                        || queuedCommand.priority > highestPriorityCommand.priority
                        || (queuedCommand.priority == highestPriorityCommand.priority
                        && queuedCommand.queuedTime < highestPriorityCommand.queuedTime)) {
                    highestPriorityCommand = queuedCommand;
                }
            }

            if (highestPriorityCommand != null) {
                mQueuedCommands.remove(highestPriorityCommand);
                messageQueueSize = mQueuedCommands.size();
                messageQueueSizeChanged = true;
            }
        }

        if (messageQueueSizeChanged) {
            notifyListener(messageQueueSize);
        }

        if (highestPriorityCommand == null) {
            if (mCountDownLatch == null) {
                mCountDownLatch = new CountDownLatch(1);
            }

            try {
                mCountDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return highestPriorityCommand;
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

    private void releaseLatch() {
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
            mCountDownLatch = null;
        }
    }

    private void notifyListener(int messageQueueSize) {
        if (mListener != null) {
            mHandler.post(() -> mListener.onMessageQueueSizeChanged(messageQueueSize));
        }
    }
}
