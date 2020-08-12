package com.paulmandal.atak.forwarder;

public class Config {
    /**
     * Basic configuration
     */
    public static final String GOTENNA_SDK_TOKEN = ;

    /**
     * IMPORTANT this is used to set the GoTenna frequencies, please adjust to your approx lat/lon
     */
    public static final double LATITUDE = ; // 40.619373
    public static final double LONGITUDE = ; // -74.102977

    /**
     * You will need one primary and one secondary device to test this, when building for the secondary device set this to false
     *
     * Basically this sets the GIDs
     */
    private static final boolean PRIMARY_DEVICE = true;

    /**
     * Tweaks to message handling -- GoTenna max message length is 235 bytes with a max transmission rate of 5 msgs per minute (approx, according to their error messages)
     */
    public static final int MESSAGE_CHUNK_LENGTH = 192;
    public static final int DELAY_BETWEEN_MESSAGES_MS = 10000;

    /**
     * Test GIDs
     */
    public static final long GOTENNA_LOCAL_GID = PRIMARY_DEVICE ? 123456789 : 987654321;
    public static final long GOTENNA_REMOTE_GID = PRIMARY_DEVICE ? 987654321 : 123456789;

    /**
     * IP and port to retransmit inbound messages to, this should work with the defaults in ATAK
     * Settings / Network Connections / Network Connections / Manage Inputs / 0.0.0.0:4242:udp
     */
    public static final int INBOUND_MESSAGE_DEST_PORT = 4242;

    /**
     * This can be anything > 1024 and < 65535
     */
    public static final int INBOUND_MESSAGE_SRC_PORT = 17233;
}
