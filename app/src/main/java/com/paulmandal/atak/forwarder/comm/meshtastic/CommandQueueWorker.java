package com.paulmandal.atak.forwarder.comm.meshtastic;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.paulmandal.atak.forwarder.ForwarderConstants;
import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.BroadcastDiscoveryCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommand;
import com.paulmandal.atak.forwarder.comm.queue.commands.SendMessageCommand;
import com.paulmandal.atak.forwarder.plugin.Destroyable;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandQueueWorker implements Destroyable, ConnectionStateHandler.Listener {
    private static final String TAG =  ForwarderConstants.DEBUG_TAG_PREFIX + CommandQueueWorker.class.getSimpleName();

    private static final int CHECK_MESSAGE_QUEUE_INTERVAL_MS = 300;

    private final ScheduledExecutorService mExecutor;
    private final CommandQueue mCommandQueue;
    private final MeshSender mMeshSender;

    private ConnectionStateHandler.ConnectionState mConnectionState;
    private boolean mDestroyed = false;

    public CommandQueueWorker(List<Destroyable> destroyables,
                              ConnectionStateHandler connectionStateHandler,
                              CommandQueue commandQueue,
                              MeshSender meshSender,
                              ScheduledExecutorService scheduledExecutorService) {
        mExecutor = scheduledExecutorService;
        mCommandQueue = commandQueue;
        mMeshSender = meshSender;

        destroyables.add(this);
        connectionStateHandler.addListener(this);

        mConnectionState = ConnectionStateHandler.ConnectionState.NO_SERVICE_CONNECTION;

        startWorker();
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mExecutor.shutdown();
        mDestroyed = true;
    }

    @Override
    public void onConnectionStateChanged(ConnectionStateHandler.ConnectionState connectionState) {
        mConnectionState = connectionState;
    }

    private void startWorker() {
        mExecutor.scheduleAtFixedRate(() -> {
            if (mDestroyed || mMeshSender.isSendingMessage() || mConnectionState != ConnectionStateHandler.ConnectionState.DEVICE_CONNECTED) {
                return;
            }

            QueuedCommand queuedCommand = mCommandQueue.popHighestPriorityCommand(true);

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
