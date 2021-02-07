package com.paulmandal.atak.forwarder.factories;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

import java.util.List;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(CommHardware commHardware,
                                                                 CotShrinker cotShrinker,
                                                                 Logger logger) {
        return new InboundMessageHandler(CotMapComponent.getInternalDispatcher(), CotMapComponent.getExternalDispatcher(), commHardware, cotShrinker, logger);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(CommHardware commHardware,
                                                                   CommandQueue commandQueue,
                                                                   QueuedCommandFactory queuedCommandFactory,
                                                                   CotMessageCache cotMessageCache,
                                                                   CotShrinker cotShrinker,
                                                                   List<Destroyable> destroyables,
                                                                   Logger logger) {
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        return new OutboundMessageHandler(commsMapComponent, commHardware, commandQueue, queuedCommandFactory, cotMessageCache, cotShrinker, destroyables, logger);
    }
}
