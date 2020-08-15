package com.paulmandal.atak.forwarder.comm;

import android.support.annotation.Nullable;

import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.cotutils.CotComparer;

import java.util.ArrayList;
import java.util.List;

public class MessageQueue {
    private static final String TAG = "ATAKDBG." + MessageQueue.class.getSimpleName();

    public interface Listener {
        void onMessageQueueSizeChanged(int size);
    }

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_HIGHEST = 3;

    private CotComparer mCotComparer;

    private final List<QueuedMessage> mQueuedMessages;
    private Listener mListener;

    public MessageQueue(CotComparer cotComparer) {
        mCotComparer = cotComparer;
        mQueuedMessages =  new ArrayList<>();
    }

    public void queueMessage(CotEvent cotEvent, byte[] message, String[] toUIDs, int priority, int xmitType, boolean overwriteSimilar) {
        int messageQueueSize = 0;
        synchronized (mQueuedMessages) {
            if (overwriteSimilar && cotEvent != null) {
                for (QueuedMessage queuedMessage : mQueuedMessages) {
                    if (mCotComparer.areCotEventsEqual(cotEvent, queuedMessage.cotEvent)
                            && mCotComparer.areUidsEqual(toUIDs, queuedMessage.toUIDs)) {
                        queuedMessage.cotEvent = cotEvent;
                        queuedMessage.message = message;
                        queuedMessage.toUIDs = toUIDs;
                        return;
                    }
                }
            }

            mQueuedMessages.add(new QueuedMessage(cotEvent, message, toUIDs, priority, System.currentTimeMillis(), xmitType));
            messageQueueSize = mQueuedMessages.size();
        }

        notifyListener(messageQueueSize);
    }

    @Nullable
    public QueuedMessage popHighestPriorityMessage(boolean readyForGroupMessages) {
        QueuedMessage highestPriorityMessage = null;
        int messageQueueSize = 0;
        synchronized (mQueuedMessages) {
            for (QueuedMessage queuedMessage : mQueuedMessages) {

                if ((queuedMessage.toUIDs == null
                        && !readyForGroupMessages
                        && queuedMessage.xmitType != QueuedMessage.XMIT_TYPE_BROADCAST)) {
                    // Ignore group messages for now
                    continue;
                }

                if (highestPriorityMessage == null
                        || queuedMessage.priority > highestPriorityMessage.priority
                        || (queuedMessage.priority == highestPriorityMessage.priority
                        && queuedMessage.queueTime < highestPriorityMessage.queueTime)) {
                    highestPriorityMessage = queuedMessage;
                }
            }

            if (highestPriorityMessage != null) {
                mQueuedMessages.remove(highestPriorityMessage);
            }

            messageQueueSize = mQueuedMessages.size();
        }

        notifyListener(messageQueueSize);

        return highestPriorityMessage;
    }

    public int getQueueSize() {
        synchronized (mQueuedMessages) {
            return mQueuedMessages.size();
        }
    }

    public void clearData() {
        synchronized (mQueuedMessages) {
            mQueuedMessages.clear();
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private void notifyListener(int messageQueueSize) {
        if (mListener != null) {
            mListener.onMessageQueueSizeChanged(messageQueueSize);
        }
    }

    public static final class QueuedMessage {
        public static final int XMIT_TYPE_NORMAL = 0;
        public static final int XMIT_TYPE_BROADCAST = 1;

        public CotEvent cotEvent;
        public byte[] message;
        public String[] toUIDs;
        public int priority;
        public long queueTime;
        public int xmitType;

        public QueuedMessage(CotEvent cotEvent, byte[] message, String[] toUIDs, int priority, long queueTime, int xmitType) {
            this.cotEvent = cotEvent;
            this.message = message;
            this.toUIDs = toUIDs;
            this.priority = priority;
            this.queueTime = queueTime;
            this.xmitType = xmitType;
        }
    }
}
