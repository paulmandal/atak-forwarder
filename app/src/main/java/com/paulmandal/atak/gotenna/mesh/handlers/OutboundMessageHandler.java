package com.paulmandal.atak.gotenna.mesh.handlers;

import android.util.Log;

import com.paulmandal.atak.gotenna.mesh.MainActivity;
import com.paulmandal.atak.gotenna.mesh.interfaces.CommHardware;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class OutboundMessageHandler {
    public interface Listener {
        void onMessageSent(String message);
    }

    private static final String TAG = "ATAKDBG." + OutboundMessageHandler.class.getSimpleName();

    private static final int OUTBOUND_MESSAGE_PORT = MainActivity.OUTBOUND_MESSAGE_PORT;

    private Thread mThread;
    private CommHardware mCommHardware;
    private boolean mKeepListening = true;

    private Listener mListener;

    public OutboundMessageHandler(CommHardware commHardware) {
        mCommHardware = commHardware;
        mThread = new Thread(this::listenForOutboundMessageAsync);
        mThread.start();
    }

    public void destroy() { // TODO: must be called
        mKeepListening = false;
    }

    public void addListener(Listener listener) {
        mListener = listener;
    }

    public void removeListener() {
        mListener = null;
    }

    private void listenForOutboundMessageAsync() {
        try (DatagramSocket socket = new DatagramSocket(OUTBOUND_MESSAGE_PORT)) {
            byte[] buf = new byte[4096];

            while (mKeepListening) {
                try {
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    socket.receive(recv);

                    String message = new String(recv.getData()).trim();

                    mCommHardware.sendMessage(message);

                    if (mListener != null) {
                        mListener.onMessageSent(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "IOException while waiting for packets from ATAK, UDP listener dying");
                    mKeepListening = false;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
