package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.MappingNotFoundException;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.comm.protobuf.fallback.FallbackCotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_CHAT;
import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + OutboundMessageHandler.class.getSimpleName();

    private CommsMapComponent mCommsMapComponent;
    private CommHardware mCommHardware;
    private CommandQueue mCommandQueue;
    private QueuedCommandFactory mQueuedCommandFactory;
    private CotMessageCache mCotMessageCache;
    private CotEventProtobufConverter mCotEventProtobufConverter;
    private FallbackCotEventProtobufConverter mFallbackCotEventProtobufConverter;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent,
                                  CommHardware commHardware,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  CotMessageCache cotMessageCache,
                                  CotEventProtobufConverter cotEventProtobufConverter,
                                  FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter) {
        mCommsMapComponent = commsMapComponent;
        mCommHardware = commHardware;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mCotMessageCache = cotMessageCache;
        mCotEventProtobufConverter = cotEventProtobufConverter;
        mFallbackCotEventProtobufConverter = fallbackCotEventProtobufConverter;

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
            cotProtobuf = mCotEventProtobufConverter.toByteArray(cotEvent);
            marshalledAsMinimal = true;
        } catch (MappingNotFoundException | UnknownDetailFieldException e) {
            Log.e(TAG, e.getMessage());
            cotProtobuf = mFallbackCotEventProtobufConverter.toByteArray(cotEvent);
        }
        boolean overwriteSimilar = eventType.equals(TYPE_PLI) || !eventType.equals(TYPE_CHAT);
        mCommandQueue.queueSendMessage(mQueuedCommandFactory.createSendMessageCommand(determineMessagePriority(cotEvent), cotEvent, cotProtobuf, toUIDs), overwriteSimilar);

        // TODO: remove this debugging
        if (marshalledAsMinimal) {
            byte[] cotProtobufOriginal = mFallbackCotEventProtobufConverter.toByteArray(cotEvent);
            byte[] minimalProtobuf = new byte[1];
            try {
                 minimalProtobuf = mCotEventProtobufConverter.toByteArray(cotEvent);
            } catch (MappingNotFoundException | UnknownDetailFieldException e) {
                Log.e(TAG, e.getMessage());
            }
            Log.d(TAG, "l protobuf len: " + cotProtobufOriginal.length);
            Log.d(TAG, "m protobuf len: " + minimalProtobuf.length);

            CotEvent minimalCotEvent = mCotEventProtobufConverter.toCotEvent(minimalProtobuf);

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
