package com.paulmandal.atak.forwarder.factories;

import com.atakmap.android.cot.CotMapComponent;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

public class MessageHandlerFactory {
    public static InboundMessageHandler getInboundMessageHandler(InboundMeshMessageHandler inboundMeshMessageHandler,
                                                                 CotShrinker cotShrinker,
                                                                 InboundMessageHandler.InboundPliListener inboundPliListener,
                                                                 Logger logger) {
        return new InboundMessageHandler(CotMapComponent.getInternalDispatcher(), CotMapComponent.getExternalDispatcher(), inboundMeshMessageHandler, cotShrinker, inboundPliListener, logger);
    }
}
