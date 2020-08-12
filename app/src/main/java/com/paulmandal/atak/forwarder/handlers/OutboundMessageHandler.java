package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import java.util.Arrays;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "ATAKDBG." + OutboundMessageHandler.class.getSimpleName();

    private CommsMapComponent mCommsMapComponent;
    private CommHardware mCommHardware;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent, CommHardware commHardware) {
        mCommsMapComponent = commsMapComponent;
        mCommHardware = commHardware;

        commsMapComponent.registerPreSendProcessor(this);
    }

    public void destroy() {
        mCommsMapComponent.registerPreSendProcessor(null);
    }

    @Override
    public void processCotEvent(CotEvent cotEvent, String[] toUIDs) {
        // TODO: handle toUIDs here
        String cotString = cotEvent.toString();
        byte[] cotBytes = cotString.getBytes();
        Log.d(TAG, "processCotEvent(): length: " + cotString.length() + ", bytes: " + cotBytes.length + ", to UIDs: " + Arrays.toString(toUIDs));
            mCommHardware.sendMessage(cotBytes);
    }
}
