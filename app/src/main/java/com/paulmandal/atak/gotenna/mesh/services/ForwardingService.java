package com.paulmandal.atak.gotenna.mesh.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.paulmandal.atak.gotenna.mesh.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.gotenna.mesh.handlers.InboundMessageHandler;
import com.paulmandal.atak.gotenna.mesh.handlers.OutboundMessageHandler;

/**
 * Listens to
 */
public class ForwardingService extends Service implements OutboundMessageHandler.Listener, InboundMessageHandler.Listener {
    public interface MessageListener {
        void onOutboundMessage(String message);
        void onInboundMessage(String message);
    }

    private static final String TAG = "ATAKDBG." + ForwardingService.class.getSimpleName();

    private final IBinder binder = new ForwardingServiceBinder();

    private OutboundMessageHandler mOutboundMessageHandler;
    private InboundMessageHandler mInboundMessageHandler;

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
        Log.d(TAG, "onStartCommand");

        GoTennaCommHardware goTennaCommHardware = new GoTennaCommHardware();
        goTennaCommHardware.init();

        mOutboundMessageHandler = new OutboundMessageHandler(goTennaCommHardware);
        mOutboundMessageHandler.addListener(this);

        mInboundMessageHandler = new InboundMessageHandler(goTennaCommHardware);
        mInboundMessageHandler.addListener(this);


        return START_STICKY;
    }

    public void addListener(MessageListener listener) {
        mListener = listener;
    }

    public void removeListener() {
        mListener = null;
    }

    @Override
    public void onDestroy() {
        // TODO: probably do some cleanup here
    }

    @Override
    public void onMessageReceived(String message) {
        if (mListener != null) {
            mListener.onInboundMessage(message);
        }
    }

    @Override
    public void onMessageSent(String message) {
        if (mListener != null) {
            mListener.onOutboundMessage(message);
        }
    }
}
