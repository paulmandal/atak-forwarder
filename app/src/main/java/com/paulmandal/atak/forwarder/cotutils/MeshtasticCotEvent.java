package com.paulmandal.atak.forwarder.cotutils;

import com.atakmap.coremap.cot.event.CotEvent;

/**
 * A CotEvent that we have received from Meshtastic and are retransmitting to the ATAK Mesh
 * we should avoid transmitting these messages back to the Meshtastic network when they go through the PreSendProcessor
 */
public class MeshtasticCotEvent extends CotEvent {
    public MeshtasticCotEvent(CotEvent cotEvent) {
        super(cotEvent);
    }
}
