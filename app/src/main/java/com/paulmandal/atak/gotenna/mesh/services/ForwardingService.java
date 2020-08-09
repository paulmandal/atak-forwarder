package com.paulmandal.atak.gotenna.mesh.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gotenna.sdk.connection.GTConnectionError;
import com.gotenna.sdk.connection.GTConnectionManager;
import com.gotenna.sdk.connection.GTConnectionState;
import com.gotenna.sdk.data.GTCommand;
import com.gotenna.sdk.data.GTCommandCenter;
import com.gotenna.sdk.data.GTDeviceType;
import com.gotenna.sdk.data.GTError;
import com.gotenna.sdk.data.GTErrorListener;
import com.gotenna.sdk.data.GTResponse;
import com.gotenna.sdk.data.GTSendMessageResponse;
import com.gotenna.sdk.data.Place;
import com.gotenna.sdk.data.messages.GTBaseMessageData;
import com.gotenna.sdk.data.messages.GTGroupCreationMessageData;
import com.gotenna.sdk.data.messages.GTMessageData;
import com.gotenna.sdk.data.messages.GTTextOnlyMessageData;
import com.gotenna.sdk.georegion.PlaceFinderTask;
import com.paulmandal.atak.gotenna.mesh.handlers.AtakMessageHandler;
import com.paulmandal.atak.gotenna.mesh.models.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

/**
 * Listens to
 */
public class ForwardingService extends Service implements GTConnectionManager.GTConnectionListener, GTCommandCenter.GTMessageListener {
    private static final String TAG = "GTDBG";

    private static final String TEST0_USERNAME = "test0";
    private static final String TEST1_USERNAME = "test1";
    private static final long TEST0_GID = 1234567890;
    private static final long TEST1_GID = 987654321;

//    private static final String ACTIVE_USERNAME = TEST0_USERNAME;
//    private static final long ACTIVE_GID = TEST0_GID;
//    private static final String INACTIVE_USERNAME = TEST1_USERNAME;
//    private static final long INACTIVE_GID = TEST1_GID;

    private static final String ACTIVE_USERNAME  = TEST1_USERNAME;
    private static final long ACTIVE_GID = TEST1_GID;
    private static final String INACTIVE_USERNAME = TEST0_USERNAME;
    private static final long INACTIVE_GID = TEST0_GID;

    private AtakMessageHandler mAtakMessageHandler;

    private GTConnectionManager mGtConnectionManager;
    private GTCommandCenter mGtCommandCenter;

    /**
     * The IP and port to listen to for message FROM your local ATAK
     * <p>
     * TODO: better comment
     */
    private static final String OUTBOUND_IP = "127.0.0.1";
    private static final int OUTBOUND_PORT = 31337;

    /**
     * The IP and port to forward GoTenna Mesh messages to
     * <p>
     * TODO: better comment
     */
    private static final String INBOUND_IP = "127.0.0.1";
    private static final int INBOUND_PORT = 31338;

    public interface MessageListener {
        void onMessage(String senderIp, String message);
    }

    private final IBinder binder = new ForwardingServiceBinder();
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
    public class ForwardingServiceBinder extends Binder {
        public ForwardingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ForwardingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "UdpListenerService started");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGtConnectionManager = GTConnectionManager.getInstance();
        mGtCommandCenter = GTCommandCenter.getInstance();

        mAtakMessageHandler = new AtakMessageHandler();

        Log.d(TAG, "attempting to connect to GoTenna Mesh");
        scanForGotenna(GTDeviceType.MESH);


//        mShouldRestartListener = true;
////        listenForUdpMulticastAsync();
//        listenForUdpUnicastAsync();
//        Log.d(TAG, "UdpListenerService started -- onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mShouldRestartListener = false;
//        mMulticastSocket.close();
        mUnicastSocket.close();
    }

    private void scanForGotenna(GTDeviceType deviceType) {
        try {
            mGtConnectionManager.addGtConnectionListener(this);
            mGtConnectionManager.scanAndConnect(deviceType);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionStateUpdated(@NonNull GTConnectionState connectionState) {
        Log.d(TAG, "onConnectionStateUpdated: " + connectionState);
        switch (connectionState) {
            case CONNECTED:
                onGotennaConnected();
                break;
        }
    }

    @Override
    public void onConnectionError(@NonNull GTConnectionState connectionState, @NonNull GTConnectionError error) {
        Log.d(TAG, "Error connection to GoTenna: " + error.getDetailString());
    }

    private void onGotennaConnected() {
        // TODO: Notify listener
        Log.d(TAG, "onGotennaConnected");
        GTDeviceType deviceType = GTConnectionManager.getInstance().getDeviceType();

        switch (deviceType) {
            case MESH:
                findAndSetMeshLocation();
                break;
        }
    }

    private void findAndSetMeshLocation() {
        // TODO: Use Android GPS to determine user's current location instead of hardcoding lat lng
        // TODO: Must use location to correctly set goTenna Mesh Device Frequency for the user's current location as per FCC rules
        Location location = new Location("Custom");
        location.setLatitude(40.619373);
        location.setLongitude(-74.102977);

        new PlaceFinderTask(location, new PlaceFinderTask.PlaceFinderListener() {
            @Override
            public void onPlaceFound(@NonNull Place place) {
                if (place == Place.UNKNOWN) {
                    // Default to North America if we can't find the actual location
                    place = Place.NORTH_AMERICA;
                }

                mGtCommandCenter.sendSetGeoRegion(place, new GTCommand.GTCommandResponseListener() {
                    @Override
                    public void onResponse(GTResponse response) {
                        if (response.getResponseCode() == GTResponse.GTCommandResponseCode.POSITIVE) {
                            // TODO: create or join group
                            Log.d(TAG, "setting GID");
                            mGtCommandCenter.setGoTennaGID(ACTIVE_GID, ACTIVE_USERNAME, (GTError gtError) -> Log.d(TAG, " Error setting gotenna ID"));
                            startForwardingMessages();
                        } else {
                            Log.d(TAG, "Error setting frequencies");
                        }
                    }
                }, new GTErrorListener() {
                    @Override
                    public void onError(GTError error) {
                        Log.d(TAG, "Error setting frequencies: " + error.toString());
                    }
                });
            }
        }).execute();
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

    @Override
    public void onIncomingMessage(GTMessageData messageData) {
        // We do not send any custom formatted messages in this app,
        // but if you wanted to send out messages with your own format, this is where
        // you would receive those incoming messages.
    }

    @Override
    public void onIncomingMessage(GTBaseMessageData gtBaseMessageData) {
        // This is where you would receive incoming messages that the SDK automatically knows how to parse
        // such as GTTextOnlyMessageData among the many other MessageData classes.
        if (gtBaseMessageData instanceof GTTextOnlyMessageData) {
            // Somebody sent us a message, try to parse it
            GTTextOnlyMessageData gtTextOnlyMessageData = (GTTextOnlyMessageData) gtBaseMessageData;
            Message incomingMessage = Message.createMessageFromData(gtTextOnlyMessageData);
            Log.d(TAG, "incoming message");
        } else if (gtBaseMessageData instanceof GTGroupCreationMessageData) {
            // Somebody invited us to a group!
            GTGroupCreationMessageData gtGroupCreationMessageData = (GTGroupCreationMessageData) gtBaseMessageData;
            Log.d(TAG, " group creation invite: " + gtGroupCreationMessageData.getGroupGID());
        }
    }

    /**
     * Internal
     */

    private void startForwardingMessages() {
        mGtCommandCenter.setMessageListener(this);
//        listenForUdpUnicastAsync();
        sendTestMessage();
    }

    private void sendTestMessage() {
        Thread sendMessageThread = new Thread(() -> {
            while (true) {
                String testMessage = "test timestamp: " + new Date().toString();
                Log.d(TAG, "sending message: " + testMessage);
                mGtCommandCenter.sendMessage(testMessage.getBytes(),
                        INACTIVE_GID,
                        (GTSendMessageResponse gtSendMessageResponse) -> Log.d(TAG, "send repsonse: " + gtSendMessageResponse.toString()),
                        (GTError gtError) -> Log.d(TAG, "Error: " + gtError.toString()),
                        true);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        sendMessageThread.start();
    }

    private void notifyListener(DatagramPacket packet) {
        if (mListener == null) {
            return;
        }

        Log.d(TAG, "Waiting for UDP unicast");

        String senderIp = packet.getAddress().getHostAddress();
        String message = new String(packet.getData()).trim();

        Log.d(TAG, "Got UDP unicast from " + senderIp + ", message: " + message);

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
        DatagramSocket s = new DatagramSocket(port);
        byte[] buf = new byte[4096];

        while (true) {
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            // TODO: switch this to another thread
            mAtakMessageHandler.handleMessage(recv);
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
//            Log.d(TAG, "Waiting for UDP multicast");
//
//            String senderIP = recv.getAddress().getHostAddress();
//            String message = new String(recv.getData()).trim();
//
//            Log.d(TAG, "Got UDP multicast from " + senderIP + ", message: " + message);
//        }
//    }
}
