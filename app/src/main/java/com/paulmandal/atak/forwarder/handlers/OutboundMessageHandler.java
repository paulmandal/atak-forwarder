package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.MinimalCotProtobufConverter;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_CHAT;
import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "ATAKDBG." + OutboundMessageHandler.class.getSimpleName();

    private CommsMapComponent mCommsMapComponent;
    private CommHardware mCommHardware;
    private CommandQueue mCommandQueue;
    private QueuedCommandFactory mQueuedCommandFactory;
    private CotMessageCache mCotMessageCache;
    private MinimalCotProtobufConverter mMinimalCotProtobufConverter;
    private CotProtobufConverter mCotProtobufConverter;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent,
                                  CommHardware commHardware,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  CotMessageCache cotMessageCache,
                                  MinimalCotProtobufConverter minimalCotProtobufConverter,
                                  CotProtobufConverter cotProtobufConverter) {
        mCommsMapComponent = commsMapComponent;
        mCommHardware = commHardware;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mCotMessageCache = cotMessageCache;
        mMinimalCotProtobufConverter = minimalCotProtobufConverter;
        mCotProtobufConverter = cotProtobufConverter;

        commsMapComponent.registerPreSendProcessor(this);
    }

    public void destroy() {
        mCommsMapComponent.registerPreSendProcessor(null);
    }

    @Override
    public void processCotEvent(CotEvent cotEvent, String[] toUIDs) {
        Log.d(TAG, "processCotEvent: " + cotEvent);
        String eventType = cotEvent.getType();
        if (mCommHardware.isConnected()
            && (toUIDs != null || mCommHardware.isInGroup())
            && !eventType.equals(TYPE_CHAT)) {
            if (mCotMessageCache.checkIfRecentlySent(cotEvent)) {
                Log.d(TAG, "Discarding recently sent event: " + cotEvent.toString()); // TODO: remove this
                return;
            }
            mCotMessageCache.cacheEvent(cotEvent);
        }

        byte[] cotProtobuf;
        boolean marshalledAsMinimal = false;
        try {
            cotProtobuf = mMinimalCotProtobufConverter.toByteArray(cotEvent);
            marshalledAsMinimal = true;
        } catch (MinimalCotProtobufConverter.MappingNotFoundException | MinimalCotProtobufConverter.UnknownDetailFieldException e) {
            Log.e(TAG, e.getMessage());
            cotProtobuf = mCotProtobufConverter.toByteArray(cotEvent);
        }
        boolean overwriteSimilar = eventType.equals(TYPE_PLI) || !eventType.equals(TYPE_CHAT);
        mCommandQueue.queueSendMessage(mQueuedCommandFactory.createSendMessageCommand(determineMessagePriority(cotEvent), cotEvent, cotProtobuf, toUIDs), overwriteSimilar);

        // TODO: remove this debugging
        if (marshalledAsMinimal) {
            byte[] cotProtobufOriginal = mCotProtobufConverter.toByteArray(cotEvent);
            byte[] minimalProtobuf = new byte[1];
            try {
                 minimalProtobuf = mMinimalCotProtobufConverter.toByteArray(cotEvent);
            } catch (MinimalCotProtobufConverter.MappingNotFoundException | MinimalCotProtobufConverter.UnknownDetailFieldException e) {
                Log.e(TAG, e.getMessage());
            }
            Log.d(TAG, "l protobuf len: " + cotProtobufOriginal.length);
            Log.d(TAG, "m protobuf len: " + minimalProtobuf.length);

            CotEvent minimalCotEvent = mMinimalCotProtobufConverter.toCotEvent(minimalProtobuf);

            if (minimalCotEvent == null) {
                Log.e(TAG, "cotEvent could not be parsed from protobuf for comparison!");
                return;
            }

            Log.d(TAG, "o: " + cotEvent.toString());
            Log.d(TAG, "m: " + minimalCotEvent.toString());
        }
    }

    private int determineMessagePriority(CotEvent cotEvent) {
        switch (cotEvent.getType()) {
            case TYPE_CHAT:
                return QueuedCommand.PRIORITY_MEDIUM;
            default:
                return QueuedCommand.PRIORITY_LOW;
        }
    }
}
