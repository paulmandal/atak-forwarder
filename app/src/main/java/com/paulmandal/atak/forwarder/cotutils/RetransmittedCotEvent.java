package com.paulmandal.atak.forwarder.cotutils;

import com.atakmap.coremap.cot.event.CotEvent;

/**
 * A CotEvent that we have retransmitted to the ATAK Mesh from Meshtastic, we should avoid retransmitting these messages back to the Meshtastic network
 */
public class RetransmittedCotEvent extends CotEvent {
    public RetransmittedCotEvent(CotEvent cotEvent) {
        super(cotEvent);
    }
}
