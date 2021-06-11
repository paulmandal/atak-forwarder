package com.paulmandal.atak.forwarder.handlers;

import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.MessageType;
import com.paulmandal.atak.forwarder.comm.meshtastic.InboundMeshMessageHandler;
import com.paulmandal.atak.forwarder.cotutils.MeshtasticCotEvent;
import com.paulmandal.atak.forwarder.helpers.Logger;
import com.paulmandal.atak.libcotshrink.pub.api.CotShrinker;

public class InboundMessageHandler implements InboundMeshMessageHandler.MessageListener {
    private static final String TAG = ForwarderConstants.DEBUG_TAG_PREFIX + InboundMessageHandler.class.getSimpleName();

    public interface InboundPliListener {
        void onInboundPli(CotEvent cotEvent);
    }

    private final CotDispatcher mInternalCotDispatcher;
    private final CotDispatcher mExternalCotDispatcher;
    private final CotShrinker mCotShrinker;
    private final InboundPliListener mInboundPliListener;
    private final Logger mLogger;

    public InboundMessageHandler(CotDispatcher internalCotDispatcher,
                                 CotDispatcher externalCotDispatcher,
                                 InboundMeshMessageHandler inboundMeshMessageHandler,
                                 CotShrinker cotShrinker,
                                 InboundPliListener inboundPliListener,
                                 Logger logger) {
        mInternalCotDispatcher = internalCotDispatcher;
        mExternalCotDispatcher = externalCotDispatcher;
        mCotShrinker = cotShrinker;
        mInboundPliListener = inboundPliListener;
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

            if (MessageType.fromCotEventType(cotEvent.getType()) == MessageType.PLI) {
                mInboundPliListener.onInboundPli(cotEvent);
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
