package com.paulmandal.atak.gotenna.mesh.handlers;

import android.util.Log;

import com.paulmandal.atak.gotenna.mesh.MainActivity;
import com.paulmandal.atak.gotenna.mesh.interfaces.CommHardware;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class InboundMessageHandler implements CommHardware.Listener {
    public interface Listener {
        void onMessageReceived(String message);
    }

    private static final String TAG = "ATAKDBG." + InboundMessageHandler.class.getSimpleName();

    private static final int INBOUND_MESSAGE_DEST_PORT = MainActivity.INBOUND_MESSAGE_DEST_PORT;
    private static final int INBOUND_MESSAGE_SRC_PORT = MainActivity.INBOUND_MESSAGE_SRC_PORT;

    private CommHardware mCommHardware;
    private Listener mListener;

    public InboundMessageHandler(CommHardware commHardware) {
        mCommHardware = commHardware;
        mCommHardware.addListener(this);
    }

    public void addListener(Listener listener) {
        mListener = listener;
    }

    public void removeListener() {
        mListener = null;
    }

    @Override
    public void onMessageReceived(String message) {
        new Thread(() -> {
            Log.d(TAG, "InboundMessageHandler.onMessageReceived: " + message);
            try (DatagramSocket socket = new DatagramSocket(INBOUND_MESSAGE_SRC_PORT)) {
                InetAddress serverAddr = InetAddress.getLocalHost();
                byte[] buf = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, INBOUND_MESSAGE_DEST_PORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException while trying to send message to UDP");
            }
        }).start();

        if (mListener != null) {
            mListener.onMessageReceived(message);
        }
    }
}
