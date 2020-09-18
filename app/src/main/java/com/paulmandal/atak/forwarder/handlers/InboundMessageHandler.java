package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.comm.commhardware.CommHardware;
import com.paulmandal.atak.forwarder.comm.protobuf.fallback.FallbackCotEventProtobufConverter;
import com.paulmandal.atak.forwarder.comm.protobuf.CotEventProtobufConverter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class InboundMessageHandler implements CommHardware.MessageListener {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + InboundMessageHandler.class.getSimpleName();

    private static final int INBOUND_MESSAGE_DEST_PORT = Config.INBOUND_MESSAGE_DEST_PORT;
    private static final int INBOUND_MESSAGE_SRC_PORT = Config.INBOUND_MESSAGE_SRC_PORT;

    private CotEventProtobufConverter mCotEventProtobufConverter;
    private FallbackCotEventProtobufConverter mFallbackCotEventProtobufConverter;

    public InboundMessageHandler(CommHardware commHardware,
                                 CotEventProtobufConverter cotEventProtobufConverter,
                                 FallbackCotEventProtobufConverter fallbackCotEventProtobufConverter) {
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
        String cotEventString = cotEvent.toString();
        byte[] cotEventBytes = cotEventString.getBytes();

        Log.d(TAG, "retransmitCotToLocalhost(): " + cotEventString);
        try (DatagramSocket socket = new DatagramSocket(INBOUND_MESSAGE_SRC_PORT)) {
            InetAddress serverAddr = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(cotEventBytes, cotEventBytes.length, serverAddr, INBOUND_MESSAGE_DEST_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException while trying to send message to UDP");
        }
    }
}
