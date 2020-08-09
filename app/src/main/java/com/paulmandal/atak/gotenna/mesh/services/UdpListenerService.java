package com.paulmandal.atak.gotenna.mesh.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpListenerService extends Service {

    public interface MessageListener {
        void onMessage(String senderIp, String message);
    }

    private final IBinder binder = new LocalBinder();
//    private MulticastSocket mMulticastSocket;
//    private Thread mUdpMulticastListenerThread;

    private DatagramSocket mUnicastSocket;
    private Thread mUdpUnicastListenerThread;
    private boolean mShouldRestartListener;

    private MessageListener mListener;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public UdpListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return UdpListenerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("UDPDBG", "UdpListenerService started");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mShouldRestartListener = true;
//        listenForUdpMulticastAsync();
        listenForUdpUnicastAsync();
        Log.d("UDPDBG", "UdpListenerService started -- onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mShouldRestartListener = false;
//        mMulticastSocket.close();
        mUnicastSocket.close();
    }

    /**
     * API
     */

    public void addListener(MessageListener listener) {
        mListener = listener;
    }

    public void removeListener() {
        mListener = null;
    }

    /**
     * Internal
     */

    private void notifyListener(DatagramPacket packet) {
        if (mListener == null) {
            return;
        }

        Log.d("UDPDBG", "Waiting for UDP unicast");

        String senderIp = packet.getAddress().getHostAddress();
        String message = new String(packet.getData()).trim();

        Log.d("UDPDBG", "Got UDP unicast from " + senderIp + ", message: " + message);

        mListener.onMessage(senderIp, message);
    }

    private void listenForUdpUnicastAsync() {
        Thread udpListenerThread = new Thread(() -> {
            try {
                InetAddress unicastIp = InetAddress.getByName("127.0.0.1");
                int port = 31337;
                while (mShouldRestartListener) {
                    listenForUdpUnicast(unicastIp, port);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mUnicastSocket != null) {
                    mUnicastSocket.close();
                }
            }
        });
        udpListenerThread.start();
        mUdpUnicastListenerThread = udpListenerThread;
    }

    private void listenForUdpUnicast(InetAddress unicastIp, int port) throws IOException {
        // join a Multicast group and send the group salutations
        DatagramSocket s = new DatagramSocket(port);
        byte[] buf = new byte[4096];

        while (true) {
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            notifyListener(recv);
        }
    }

//    private void listenForUdpMulticastAsync() {
//        Thread udpListenerThread = new Thread(() -> {
//            try {
//                InetAddress multicastIp = InetAddress.getByName("239.2.3.1");
//                int port = 6969;
//                while (mShouldRestartListener) {
//                    listenForUdpMulticast(multicastIp, port);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                if (mMulticastSocket != null) {
//                    mMulticastSocket.close();
//                }
//            }
//        });
//        udpListenerThread.start();
//        mUdpMulticastListenerThread = udpListenerThread;
//    }
//
//    private void listenForUdpMulticast(InetAddress multicastIp, int port) throws IOException {
//        // join a Multicast group and send the group salutations
//        MulticastSocket s = new MulticastSocket(port);
//        s.joinGroup(multicastIp);
//        byte[] buf = new byte[4096];
//
//        while (true) {
//            DatagramPacket recv = new DatagramPacket(buf, buf.length);
//            s.receive(recv);
//            Log.d("UDPDBG", "Waiting for UDP multicast");
//
//            String senderIP = recv.getAddress().getHostAddress();
//            String message = new String(recv.getData()).trim();
//
//            Log.d("UDPDBG", "Got UDP multicast from " + senderIP + ", message: " + message);
//        }
//    }
}
