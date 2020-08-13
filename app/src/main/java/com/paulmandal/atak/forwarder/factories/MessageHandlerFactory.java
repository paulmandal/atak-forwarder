package com.paulmandal.atak.forwarder.factories;

import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;
import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.helpers.DefaultEXIFactory;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(CommHardware commHardware) {
        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        return new InboundMessageHandler(commHardware, exiFactory);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(CommHardware commHardware) {
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        return new OutboundMessageHandler(commsMapComponent, commHardware, exiFactory);
    }
}
