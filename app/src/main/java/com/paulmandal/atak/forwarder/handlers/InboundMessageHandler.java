package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class InboundMessageHandler implements CommHardware.Listener {
    private static final String TAG = "ATAKDBG." + InboundMessageHandler.class.getSimpleName();

    private static final int INBOUND_MESSAGE_DEST_PORT = Config.INBOUND_MESSAGE_DEST_PORT;
    private static final int INBOUND_MESSAGE_SRC_PORT = Config.INBOUND_MESSAGE_SRC_PORT;

    private CommHardware mCommHardware;

    public InboundMessageHandler(CommHardware commHardware) {
        mCommHardware = commHardware;

        commHardware.addListener(this);
    }

    @Override
    public void onMessageReceived(byte[] message) {
        new Thread(() -> {
            String cotEvent = new String(message);
            Log.d(TAG, "onMessageReceived(): " + cotEvent);
            try (DatagramSocket socket = new DatagramSocket(INBOUND_MESSAGE_SRC_PORT)) {
                InetAddress serverAddr = InetAddress.getLocalHost();
                DatagramPacket packet = new DatagramPacket(message, message.length, serverAddr, INBOUND_MESSAGE_DEST_PORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException while trying to send message to UDP");
            }
        }).start();
    }
}
