package com.paulmandal.atak.forwarder.factories;

import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotProtobufConverter;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(CommHardware commHardware,
                                                                 CotProtobufConverter cotProtobufConverter) {
        return new InboundMessageHandler(commHardware, cotProtobufConverter);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(CommHardware commHardware,
                                                                   CommandQueue commandQueue,
                                                                   QueuedCommandFactory queuedCommandFactory,
                                                                   CotMessageCache cotMessageCache,
                                                                   CotProtobufConverter cotProtobufConverter) {
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        return new OutboundMessageHandler(commsMapComponent, commHardware, commandQueue, queuedCommandFactory, cotMessageCache, cotProtobufConverter);
    }
}
