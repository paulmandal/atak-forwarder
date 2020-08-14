package com.paulmandal.atak.forwarder;

public class Config {
    /**
     * Basic configuration
     */
    public static final String GOTENNA_SDK_TOKEN = ;

    /**
     * IMPORTANT this is used to set the GoTenna frequencies, please adjust to your approx lat/lon
     */
    public static final double FALLBACK_LATITUDE = ; // 40.619373
    public static final double FALLBACK_LONGITUDE = ; // -74.102977

    /**
     * Tweaks to message handling -- GoTenna max message length is 235 bytes with a max transmission rate of 5 msgs per minute (approx, according to their error messages)
     */
    public static final int MESSAGE_CHUNK_LENGTH = 192;
    public static final int QUOTA_REFRESH_TIME_MS = 60000;
    public static final int MESSAGES_PER_MINUTE = 5;
    public static final int DELAY_BETWEEN_POLLING_FOR_MESSAGES = 2000;

    /**
     * How long messages live in the cache CotMessageCache (preventing them being resent)
     */
    public static final int DEFAULT_CACHE_PURGE_TIME_MS = 600000;

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
