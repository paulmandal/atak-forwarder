package com.paulmandal.atak.forwarder.comm.meshtastic;

import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;

public class DiscoveryBroadcaster {
    private final CommandQueue mCommandQueue;
    private final QueuedCommandFactory mQueuedCommandFactory;

    public DiscoveryBroadcaster(CommandQueue commandQueue, QueuedCommandFactory queuedCommandFactory) {
        mCommandQueue = commandQueue;
        mQueuedCommandFactory = queuedCommandFactory;
    }


}
