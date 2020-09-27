package com.paulmandal.atak.forwarder.factories;

import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.fallback.FallbackCotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(CommHardware commHardware,
                                                                 CotEventProtobufConverter cotEventProtobufConverter,
                                                                 FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter) {
        return new InboundMessageHandler(commHardware, cotEventProtobufConverter, fallbackCotEventProtobufConverter);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(CommHardware commHardware,
                                                                   CommandQueue commandQueue,
                                                                   QueuedCommandFactory queuedCommandFactory,
                                                                   CotMessageCache cotMessageCache,
                                                                   CotEventProtobufConverter cotEventProtobufConverter,
                                                                   FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter) {
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        return new OutboundMessageHandler(commsMapComponent, commHardware, commandQueue, queuedCommandFactory, cotMessageCache, cotEventProtobufConverter, fallbackCotEventProtobufConverter);
    }
}
