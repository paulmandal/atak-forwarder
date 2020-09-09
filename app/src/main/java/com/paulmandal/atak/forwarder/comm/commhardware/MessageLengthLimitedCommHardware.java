package com.paulmandal.atak.forwarder.comm.commhardware;

import android.os.Handler;

import com.paulmandal.atak.forwarder.comm.queue.CommandQueue;
import com.paulmandal.atak.forwarder.comm.queue.commands.QueuedCommandFactory;
import com.paulmandal.atak.forwarder.group.GroupTracker;

public abstract class MessageLengthLimitedCommHardware extends CommHardware {
    public MessageLengthLimitedCommHardware(Handler uiThreadHandler, CommandQueue commandQueue, QueuedCommandFactory queuedCommandFactory, GroupTracker groupTracker) {
        super(uiThreadHandler, commandQueue, queuedCommandFactory, groupTracker);
    }
}
