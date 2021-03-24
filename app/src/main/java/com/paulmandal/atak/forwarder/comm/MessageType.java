package com.paulmandal.atak.forwarder.comm;

public enum MessageType {
    PLI,
    CHAT,
    OTHER;

    private static final String TYPE_PLI = "a-f-G-U-C";
    private static final String TYPE_CHAT = "b-t-f";

    public static MessageType fromCotEventType(String eventType) {
        if (eventType.equals(TYPE_PLI)) {
            return PLI;
        } else if (eventType.equals(TYPE_CHAT)) {
            return CHAT;
        }
        return OTHER;
    }
}
