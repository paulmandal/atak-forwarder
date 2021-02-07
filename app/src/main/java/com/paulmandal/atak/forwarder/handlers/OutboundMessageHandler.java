package com.paulmandal.atak.forwarder.handlers;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.cotutils.MeshtasticCotEvent;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

import java.util.List;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_CHAT;
import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor, Destroyable  {
    private static final String TAG = Constants.DEBUG_TAG_PREFIX + OutboundMessageHandler.class.getSimpleName();

    private final CommsMapComponent mCommsMapComponent;
    private final CommHardware mCommHardware;
    private final CommandQueue mCommandQueue;
    private final QueuedCommandFactory mQueuedCommandFactory;
    private final CotMessageCache mCotMessageCache;
    private final CotShrinker mCotShrinker;
    private final Logger mLogger;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent,
                                  CommHardware commHardware,
                                  CommandQueue commandQueue,
                                  QueuedCommandFactory queuedCommandFactory,
                                  CotMessageCache cotMessageCache,
                                  CotShrinker cotShrinker,
                                  List<Destroyable> destroyables,
                                  Logger logger) {
        mCommsMapComponent = commsMapComponent;
        mCommHardware = commHardware;
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
        if (mCommHardware.getConnectionState() == CommHardware.ConnectionState.DEVICE_CONNECTED && !eventType.equals(TYPE_CHAT)) {
            if (mCotMessageCache.checkIfRecentlySent(cotEvent)) {
                mLogger.v(TAG, "Discarding recently sent event: " + cotEvent.toString());
                return;
            }
            mCotMessageCache.cacheEvent(cotEvent);
        }

        byte[] cotAsBytes = mCotShrinker.toByteArrayLossy(cotEvent);
        boolean overwriteSimilar = eventType.equals(TYPE_PLI) || !eventType.equals(TYPE_CHAT);
        mCommandQueue.queueSendMessage(mQueuedCommandFactory.createSendMessageCommand(determineMessagePriority(cotEvent), cotEvent, cotAsBytes, toUIDs), overwriteSimilar);
    }

    private int determineMessagePriority(CotEvent cotEvent) {
        if (cotEvent.getType().equals(TYPE_CHAT)) {
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
