package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotProtobufConverter;

import org.xmlunit.builder.Input;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DOMDifferenceEngine;
import org.xmlunit.diff.DifferenceEngine;

import javax.xml.transform.Source;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "ATAKDBG." + OutboundMessageHandler.class.getSimpleName();

    /**
     * Do not cache/filter these message types
     */
    private static final String MSG_TYPE_SELF_PLI = "a-f-G-U-C";
    private static final String MSG_TYPE_CHAT = "b-t-f";

    private CommsMapComponent mCommsMapComponent;
    private CommHardware mCommHardware;
    private MessageQueue mMessageQueue;
    private CotMessageCache mCotMessageCache;
    private CotProtobufConverter mCotProtobufConverter;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent,
                                  CommHardware commHardware,
                                  MessageQueue messageQueue,
                                  CotMessageCache cotMessageCache,
                                  CotProtobufConverter cotProtobufConverter) {
        mCommsMapComponent = commsMapComponent;
        mCommHardware = commHardware;
        mMessageQueue = messageQueue;
        mCotMessageCache = cotMessageCache;
        mCotProtobufConverter = cotProtobufConverter;

        commsMapComponent.registerPreSendProcessor(this);
    }

    public void destroy() {
        mCommsMapComponent.registerPreSendProcessor(null);
    }

    @Override
    public void processCotEvent(CotEvent cotEvent, String[] toUIDs) {
        String eventType = cotEvent.getType();
        if (mCommHardware.isConnected()
            && (toUIDs != null || mCommHardware.isInGroup())
            && !eventType.equals(MSG_TYPE_SELF_PLI)
            && !eventType.equals(MSG_TYPE_CHAT)) {
            if (mCotMessageCache.checkIfRecentlySent(cotEvent)) {
                Log.d(TAG, "Discarding recently sent event: " + cotEvent.toString()); // TODO: remove this
                return;
            }
            mCotMessageCache.cacheEvent(cotEvent);
        }

        byte[] cotProtobuf =  mCotProtobufConverter.cotEventToByteArray(cotEvent);
        boolean overwriteSimilar = eventType.equals(MSG_TYPE_SELF_PLI) || !eventType.equals(MSG_TYPE_CHAT);
        mMessageQueue.queueMessage(cotEvent, cotProtobuf, toUIDs, determineMessagePriority(cotEvent), MessageQueue.QueuedMessage.XMIT_TYPE_NORMAL, overwriteSimilar);

        // TODO: remove this validation when we're satisfied that this works well
        try {
            String protoBufString = mCotProtobufConverter.cotEventFromProtoBuf(cotProtobuf).toString();
            String cotString = cotEvent.toString();
            Log.d(TAG, "o: " + cotString);
            Log.d(TAG, "p: " + protoBufString);
            Source original = Input.fromString(cotString).build();
            Source protobuffed = Input.fromString(protoBufString).build();
            DifferenceEngine diff = new DOMDifferenceEngine();
            diff.addDifferenceListener((Comparison comparison, ComparisonResult outcome) -> {
                Log.d(TAG, "  found difference b/t original CotEvent and protobuf(CotEvent):" + comparison);
            });
            diff.compare(original, protobuffed);
            Log.d(TAG, "Compare finished");
        } catch(UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    private int determineMessagePriority(CotEvent cotEvent) {
        switch (cotEvent.getType()) {
            case MSG_TYPE_CHAT:
                return MessageQueue.PRIORITY_HIGH;
            default:
                return MessageQueue.PRIORITY_MEDIUM;
        }
    }
}
