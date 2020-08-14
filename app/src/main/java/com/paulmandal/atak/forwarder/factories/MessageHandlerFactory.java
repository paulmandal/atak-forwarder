package com.paulmandal.atak.forwarder.factories;

import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.comm.CotMessageCache;
import com.paulmandal.atak.forwarder.comm.MessageQueue;
import com.paulmandal.atak.forwarder.comm.interfaces.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotProtobufConverter;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(CommHardware commHardware,
                                                                 CotProtobufConverter cotProtobufConverter) {
        return new InboundMessageHandler(commHardware, cotProtobufConverter);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(MessageQueue messageQueue,
                                                                   CotMessageCache cotMessageCache,
                                                                   CotProtobufConverter cotProtobufConverter) {
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        return new OutboundMessageHandler(commsMapComponent, messageQueue, cotMessageCache, cotProtobufConverter);
    }
}
