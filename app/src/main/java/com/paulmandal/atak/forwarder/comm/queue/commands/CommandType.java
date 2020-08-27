package com.paulmandal.atak.forwarder.comm.queue.commands;

public enum CommandType {
    BROADCAST_DISCOVERY_MSG,
    CREATE_GROUP,
    ADD_TO_GROUP,
    SCAN_FOR_COMM_DEVICE,
    DISCONNECT_FROM_COMM_DEVICE,
    SEND_TO_INDIVIDUAL,
    SEND_TO_GROUP,
    GET_BATTERY_STATUS
}
