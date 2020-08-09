package com.paulmandal.atak.gotenna.mesh.handlers;

import java.net.DatagramPacket;

public class AtakMessageHandler {

    public void handleMessage(DatagramPacket packet) {
        // Split the message into chunks, forward to GoTenna
        String message = new String(packet.getData());

    }
}
