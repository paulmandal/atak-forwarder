package com.paulmandal.atak.forwarder;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ForwarderConstants {
    /**
     * Pre-shared Key Length, 16 for AES128, 32 for AES256
     */
    public static final int PSK_LENGTH = 32;

    public static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.M;

    public static final int MAX_LOG_MESSAGES_OUTPUT = 100;

    /**
     * Meshtastic Radio Config
     */
    public static final int POSITION_BROADCAST_INTERVAL_S = 3600;
    public static final boolean GPS_ENABLED = false;
    public static final int LCD_SCREEN_ON_S = 5;
    public static final int GPS_ATTEMPT_TIME = 900;

    /**
     * Tweaks to message handling
     */
    public static final int MESHTASTIC_MESSAGE_CHUNK_LENGTH = 200;

    /**
     * Discovery Broadcast Marker
     */
    public static final String DISCOVERY_BROADCAST_MARKER = "ATAKBCAST";

    /**
     * TAG prefix for all debug messages
     */
    public static final String DEBUG_TAG_PREFIX = "FWDDBG.";
}
