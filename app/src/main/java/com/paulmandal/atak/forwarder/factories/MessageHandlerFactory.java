package com.paulmandal.atak.forwarder.factories;

import android.content.Context;

import com.atakmap.comms.CommsMapComponent;
import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;
import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.helpers.DefaultEXIFactory;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(Context context) {
        GoTennaCommHardware commHardware = CommHardwareFactory.getCommHardware(context);
        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        return new InboundMessageHandler(commHardware, exiFactory);
    }

    public static OutboundMessageHandler getOutboundMessageHandler(Context context) {
        GoTennaCommHardware commHardware = CommHardwareFactory.getCommHardware(context);
        CommsMapComponent commsMapComponent = CommsMapComponent.getInstance();
        EXIFactory exiFactory = DefaultEXIFactory.newInstance();
        return new OutboundMessageHandler(commsMapComponent, commHardware, exiFactory);
    }
}
