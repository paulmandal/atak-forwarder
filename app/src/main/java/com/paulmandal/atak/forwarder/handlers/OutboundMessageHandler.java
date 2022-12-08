package com.paulmandal.atak.forwarder.handlers;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageType;
import com.paulmandal.atak.forwarder.comm.meshtastic.ConnectionStateHandler;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.MeshtasticCotEvent;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

import java.util.List;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor, Destroyable  {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + OutboundMessageHandler.class.getSimpleName();

    private final CommsMapComponent mCommsMapComponent;
    private final ConnectionStateHandler mConnectionStateHandler;
    private final CommandQueue mCommandQueue;
    private final QueuedCommandFactory mQueuedCommandFactory;
    private final CotMessageCache mCotMessageCache;
    private final CotShrinker mCotShrinker;
    private final Logger mLogger;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent,
                                  ConnectionStateHandler connectionStateHandler,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  CotMessageCache cotMessageCache,
                                  CotShrinker cotShrinker,
                                  List<Destroyable> destroyables,
                                  Logger logger) {
        mCommsMapComponent = commsMapComponent;
        mConnectionStateHandler = connectionStateHandler;
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
        mCotMessageCache = cotMessageCache;
        mCotShrinker = cotShrinker;
        mLogger = logger;

        destroyables.add(this);
        commsMapComponent.registerPreSendProcessor(this);
    }

    @Override
    public void processCotEvent(CotEvent cotEvent, String[] toUIDs) {
        if (cotEvent instanceof MeshtasticCotEvent) {
            // Drop CotEvents that we have retransmitted from Meshtastic
            return;
        }
        mLogger.v(TAG, "processCotEvent: " + cotEvent);
        String eventType = cotEvent.getType();
        boolean isChat = MessageType.fromCotEventType(eventType) == MessageType.CHAT;
        if (mConnectionStateHandler.getConnectionState() == ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED && !isChat) {
            if (mCotMessageCache.checkIfRecentlySent(cotEvent)) {
                mLogger.v(TAG, "  Discarding recently sent event: " + cotEvent);
                return;
            }
            mCotMessageCache.cacheEvent(cotEvent);
        }

        byte[] cotAsBytes = mCotShrinker.toByteArrayLossy(cotEvent);
        MessageType messageType = MessageType.fromCotEventType(eventType);
        boolean overwriteSimilar = messageType != MessageType.CHAT;
        mCommandQueue.queueSendMessage(mQueuedCommandFactory.createSendMessageCommand(determineMessagePriority(cotEvent), cotEvent, cotAsBytes, toUIDs, messageType), overwriteSimilar);
    }

    private int determineMessagePriority(CotEvent cotEvent) {
        if (MessageType.fromCotEventType(cotEvent.getType()) == MessageType.CHAT) {
            return QueuedCommand.PRIORITY_MEDIUM;
        } else {
            return QueuedCommand.PRIORITY_LOW;
        }
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mCommsMapComponent.registerPreSendProcessor(null);
    }
}
