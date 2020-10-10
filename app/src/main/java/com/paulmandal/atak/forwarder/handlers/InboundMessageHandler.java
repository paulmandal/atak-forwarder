package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.fallback.FallbackCotEventProtobufConverter;
import com.paulmandal.atak.forwarder.cotutils.RetransmittedCotEvent;

public class InboundMessageHandler implements CommHardware.MessageListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + InboundMessageHandler.class.getSimpleName();

    private CotDispatcher mInternalCotDispatcher;
    private CotDispatcher mExternalCotDispatcher;
    private CotEventProtobufConverter mCotEventProtobufConverter;
    private FallbackCotEventProtobufConverter mFallbackCotEventProtobufConverter;

    public InboundMessageHandler(CotDispatcher internalCotDispatcher,
                                 CotDispatcher externalCotDispatcher,
                                 CommHardware commHardware,
                                 CotEventProtobufConverter cotEventProtobufConverter,
                                 FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter) {
        mInternalCotDispatcher = internalCotDispatcher;
        mExternalCotDispatcher = externalCotDispatcher;
        mCotEventProtobufConverter = cotEventProtobufConverter;
        mFallbackCotEventProtobufConverter = fallbackCotEventProtobufConverter;

        commHardware.addMessageListener(this);
    }

    @Override
    public void onMessageReceived(int messageId, byte[] message) {
        Thread messageConversionAndDispatchThread = new Thread(() -> {
            CotEvent cotEvent = mCotEventProtobufConverter.toCotEvent(message);

            if (cotEvent == null) {
                cotEvent = mFallbackCotEventProtobufConverter.toCotEvent(message);
            }
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
        RetransmittedCotEvent retransmittedCotEvent = new RetransmittedCotEvent(cotEvent);
        mInternalCotDispatcher.dispatch(retransmittedCotEvent);
        mExternalCotDispatcher.dispatch(retransmittedCotEvent);
    }
}
