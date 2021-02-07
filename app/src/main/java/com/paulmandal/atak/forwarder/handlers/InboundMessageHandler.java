package com.paulmandal.atak.forwarder.handlers;

import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.cotutils.MeshtasticCotEvent;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

public class InboundMessageHandler implements InboundMeshMessageHandler.MessageListener {
    private static final String TAG = Constants.DEBUG_TAG_PREFIX + InboundMessageHandler.class.getSimpleName();

    private final CotDispatcher mInternalCotDispatcher;
    private final CotDispatcher mExternalCotDispatcher;
    private final CotShrinker mCotShrinker;
    private final Logger mLogger;

    public InboundMessageHandler(CotDispatcher internalCotDispatcher,
                                 CotDispatcher externalCotDispatcher,
                                 InboundMeshMessageHandler inboundMeshMessageHandler,
                                 CotShrinker cotShrinker,
                                 Logger logger) {
        mInternalCotDispatcher = internalCotDispatcher;
        mExternalCotDispatcher = externalCotDispatcher;
        mCotShrinker = cotShrinker;
        mLogger = logger;

        inboundMeshMessageHandler.addMessageListener(this);
    }

    @Override
    public void onMessageReceived(int messageId, byte[] message) {
        Thread messageConversionAndDispatchThread = new Thread(() -> {
            CotEvent cotEvent = mCotShrinker.toCotEvent(message);
            if (cotEvent == null) {
                mLogger.e(TAG, "Error in onMessageReceived, cotEvent did not parse");
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
