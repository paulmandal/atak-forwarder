package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.cotutils.MeshtasticCotEvent;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

public class InboundMessageHandler implements CommHardware.MessageListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + InboundMessageHandler.class.getSimpleName();

    private final CotDispatcher mInternalCotDispatcher;
    private final CotDispatcher mExternalCotDispatcher;
    private final CotShrinker mCotShrinker;

    public InboundMessageHandler(CotDispatcher internalCotDispatcher,
                                 CotDispatcher externalCotDispatcher,
                                 CommHardware commHardware,
                                 CotShrinker cotShrinker) {
        mInternalCotDispatcher = internalCotDispatcher;
        mExternalCotDispatcher = externalCotDispatcher;
        mCotShrinker = cotShrinker;

        commHardware.addMessageListener(this);
    }

    @Override
    public void onMessageReceived(int messageId, byte[] message) {
        Thread messageConversionAndDispatchThread = new Thread(() -> {
            CotEvent cotEvent = mCotShrinker.toCotEvent(message);
            if (cotEvent == null) {
                Log.e(TAG, "Error in onMessageReceived, cotEvent did not parse");
                return;
            }

            retransmitCotToLocalhost(cotEvent);
        });
        messageConversionAndDispatchThread.setName("InboundMessageHandler.onMessageReceived");
        messageConversionAndDispatchThread.start();
    }

    public void retransmitCotToLocalhost(CotEvent cotEvent) {
        MeshtasticCotEvent meshtasticCotEvent = new MeshtasticCotEvent(cotEvent);
        mInternalCotDispatcher.dispatch(meshtasticCotEvent);
        mExternalCotDispatcher.dispatch(meshtasticCotEvent);
    }
}
