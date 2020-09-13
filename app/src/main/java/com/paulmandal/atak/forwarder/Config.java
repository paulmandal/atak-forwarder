package com.paulmandal.atak.forwarder;

public class Config {
    /**
     * Pre-shared Key Length, 16 for AES128, 32 for AES256
     */
    public static final int PSK_LENGTH = 32;

    /**
     * How long to wait after attempting to stop the service when the Paired button is clicked
     */
    public static final int DELAY_AFTER_STOPPING_SERVICE = 5000;

    /**
     * Meshtastic Radio Config
     */
    public static final int POSITION_BROADCAST_INTERVAL_S = 3600;
    public static final int LCD_SCREEN_ON_S = 1;

    /**
     * Tweaks to message handling
     */
    public static final int MESHTASTIC_MESSAGE_CHUNK_LENGTH = 200;
    public static final int DELAY_BETWEEN_POLLING_FOR_MESSAGES_MS = 2000;
    public static final int MESSAGE_AWAIT_TIMEOUT_MS = 20000; // TODO: how do to this better?

    /**
     * How long shape/PLI messages live in the cache CotMessageCache (preventing them being resent)
     */
    public static final int DEFAULT_CACHE_PURGE_TIME_MS = 1800000;
    public static final int DEFAULT_PLI_CACHE_PURGE_TIME_MS = 60000;

    /**
     * IP and port to retransmit inbound messages to, this should work with the defaults in ATAK
     * Settings / Network Connections / Network Connections / Manage Inputs / 0.0.0.0:4242:udp
     */
    public static final int INBOUND_MESSAGE_DEST_PORT = 4242;

    /**
     * This can be anything > 1024 and < 65535
     */
    public static final int INBOUND_MESSAGE_SRC_PORT = 17233;

    /**
     * TAG prefix for all debug messages
     */
    public static final String DEBUG_TAG_PREFIX = "ATAKDBG.";
}
