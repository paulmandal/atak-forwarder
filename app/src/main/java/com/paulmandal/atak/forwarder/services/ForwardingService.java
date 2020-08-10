package com.paulmandal.atak.forwarder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.paulmandal.atak.forwarder.MainActivity;
import com.paulmandal.atak.forwarder.R;
import com.paulmandal.atak.forwarder.commhardware.GoTennaCommHardware;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.handlers.OutboundMessageHandler;

/**
 * Listens to
 */
public class ForwardingService extends Service implements OutboundMessageHandler.Listener, InboundMessageHandler.Listener {
    public interface MessageListener {
        void onOutboundMessage(String message);

        void onInboundMessage(String message);
    }

    private static final String TAG = "ATAKDBG." + ForwardingService.class.getSimpleName();

    public static final String CHANNEL_ID = "AtakForwarderServiceChannel";

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

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Forwarding ATAK <-> GoTenna Mesh")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
