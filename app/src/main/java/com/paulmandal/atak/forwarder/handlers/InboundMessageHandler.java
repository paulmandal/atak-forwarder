package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.cotutils.MeshtasticCotEvent;
import com.paulmandal.atak.libcotshrink.api.CotShrinker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class InboundMessageHandler implements CommHardware.MessageListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + InboundMessageHandler.class.getSimpleName();

    private CotDispatcher mInternalCotDispatcher;
    private CotDispatcher mExternalCotDispatcher;
    private CotShrinker mCotShrinker;

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

    public void sneakyStuff(CotEvent cotEvent) {
        new Thread(() -> {
            String cotEventString = cotEvent.toString();
            byte[] cotEventBytes = cotEventString.getBytes();

            Log.d(TAG, "retransmitCotToLocalhost(): " + cotEventString);
            try (DatagramSocket socket = new DatagramSocket(31337)) {
                InetAddress serverAddr = InetAddress.getLocalHost();
                DatagramPacket packet = new DatagramPacket(cotEventBytes, cotEventBytes.length, serverAddr, 4242);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException while trying to send message to UDP");
            }
        }).start();
    }
}
