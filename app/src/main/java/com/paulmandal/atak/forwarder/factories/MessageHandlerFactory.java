package com.paulmandal.atak.forwarder.factories;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.comm.meshtastic.RConnectionStateHandler;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

import java.util.List;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(InboundMeshMessageHandler inboundMeshMessageHandler,
                                                                 CotShrinker cotShrinker,
                                                                 InboundMessageHandler.InboundPliListener inboundPliListener,
                                                                 Logger logger) {
        return new InboundMessageHandler(CotMapComponent.getInternalDispatcher(), CotMapComponent.getExternalDispatcher(), inboundMeshMessageHandler, cotShrinker, inboundPliListener, logger);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(RConnectionStateHandler connectionStateHandler,
                                                                   CommandQueue commandQueue,
                                                                   QueuedCommandFactory queuedCommandFactory,
                                                                   CotMessageCache cotMessageCache,
                                                                   CotShrinker cotShrinker,
                                                                   List<Destroyable> destroyables,
                                                                   Logger logger) {
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        return new OutboundMessageHandler(commsMapComponent, connectionStateHandler, commandQueue, queuedCommandFactory, cotMessageCache, cotShrinker, destroyables, logger);
    }
}
