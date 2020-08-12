package com.paulmandal.atak.forwarder.factories;

import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler() {
        GoTennaCommHardware commHardware = CommHardwareFactory.getCommHardware();
        return new InboundMessageHandler(commHardware);
    }

    public static OutboundMessageHandler getOutboundMessageHandler() {
        GoTennaCommHardware commHardware = CommHardwareFactory.getCommHardware();
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        return new OutboundMessageHandler(commsMapComponent, commHardware);
    }
}
