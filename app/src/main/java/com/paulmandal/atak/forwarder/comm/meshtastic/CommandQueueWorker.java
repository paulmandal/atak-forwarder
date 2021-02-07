package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.Constants;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandQueueWorker implements Destroyable, MeshServiceController.ConnectionStateListener {
    private static final String TAG =  Constants.DEBUG_TAG_PREFIX + CommandQueueWorker.class.getSimpleName();

    private static final int CHECK_MESSAGE_QUEUE_INTERVAL_MS = 300;

    private final ScheduledExecutorService mExecutor;
    private final CommandQueue mCommandQueue;
    private final MeshSender mMeshSender;

    private ConnectionState mConnectionState;
    private boolean mDestroyed = false;

    public CommandQueueWorker(List<Destroyable> destroyables,
                              MeshServiceController meshServiceController,
                              CommandQueue commandQueue,
                              MeshSender meshSender,
                              ScheduledExecutorService scheduledExecutorService) {
        mExecutor = scheduledExecutorService;
        mCommandQueue = commandQueue;
        mMeshSender = meshSender;

        destroyables.add(this);
        meshServiceController.addConnectionStateListener(this);

// TODO put for factory:
//        Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
//            Thread thread = new Thread(r);
//            thread.setName(CommHardware.class.getSimpleName() + ".Worker");
//            return thread;
//        });

        mConnectionState = ConnectionState.NO_SERVICE_CONNECTION;

        startWorker();
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mExecutor.shutdown();
        mDestroyed = true;
    }

    @Override
    public void onConnectionStateChanged(ConnectionState connectionState) {
        mConnectionState = connectionState;
    }

    private void startWorker() {
        mExecutor.scheduleAtFixedRate(() -> {
            if (mDestroyed || mMeshSender.isSendingMessage() || mMeshSender.isSuspended()) {
                return;
            }

            QueuedCommand queuedCommand = mCommandQueue.popHighestPriorityCommand(mConnectionState == ConnectionState.DEVICE_CONNECTED);

            if (queuedCommand == null) {
                return;
            }

            switch (queuedCommand.commandType) {
                case BROADCAST_DISCOVERY_MSG:
                    mMeshSender.sendDiscoveryMessage((BroadcastDiscoveryCommand) queuedCommand);
                    break;
                case SEND_TO_CHANNEL:
                case SEND_TO_INDIVIDUAL:
                    mMeshSender.sendMessage((SendMessageCommand) queuedCommand);
                    break;
            }
        }, 0, CHECK_MESSAGE_QUEUE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
}
