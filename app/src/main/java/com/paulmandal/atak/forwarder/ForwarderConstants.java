package com.paulmandal.atak.forwarder;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ForwarderConstants {
    /**
     * Pre-shared Key Length, 16 for AES128, 32 for AES256
     */
    public static final int PSK_LENGTH = 32;
    public static final int DEFAULT_CHANNEL_MODE = 1;
    public static final String DEFAULT_CHANNEL_NAME = "Default";
    public static final byte[] DEFAULT_CHANNEL_PSK = new byte[] {
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    };

    public static final int MAX_CHANNELS = 3;

    public static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.M;

    /**
     * How long to wait for radioConfig to be available when writing to a Tracker device
     */
    public static final int RADIO_CONFIG_MISSING_RETRY_TIME_MS = 10000;

    public static final int DELAY_BEFORE_RESTARTING_MESH_SENDER_AFTER_CHANNEL_CHANGE = 3000;

    public static final int DELAY_BEFORE_RESTARTING_MESH_SENDER_AFTER_TRACKER_WRITE = 10000;

    /**
     * Meshtastic Radio Config
     */
    public static final int POSITION_BROADCAST_INTERVAL_S = 3600;
    public static final int LCD_SCREEN_ON_S = 5;
    public static final int WAIT_BLUETOOTH_S = 86400;
    public static final int PHONE_TIMEOUT_S = 86400;
    public static final int DEVICE_CONNECTION_TIMEOUT = 30000;
    public static final int GPS_UPDATE_INTERVAL = 0xFFFFFFFF; // MAX_INT for the device
    public static final int SEND_OWNER_INTERVAL = 0xFFFFFFFF; // MAX_INT for the device

    /**
     * Tweaks to message handling
     */
    public static final int MESHTASTIC_MESSAGE_CHUNK_LENGTH = 200;
    public static final int REJECT_STALE_NODE_CHANGE_TIME_MS = 1800000;

    /**
     * Discovery Broadcast Marker
     */
    public static final String DISCOVERY_BROADCAST_MARKER = "ATAKBCAST";

    /**
     * TAG prefix for all debug messages
     */
    public static final String DEBUG_TAG_PREFIX = "FWDDBG.";
}
