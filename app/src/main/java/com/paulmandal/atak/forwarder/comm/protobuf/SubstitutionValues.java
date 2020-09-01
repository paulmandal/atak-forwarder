package com.paulmandal.atak.forwarder.comm.protobuf;

import java.util.ArrayList;
import java.util.List;

public class SubstitutionValues {
    public static final String CHATROOM_SUBSTITUTION_MARKER = "#c";
    public static final String UID_SUBSTITUTION_MARKER = "#u";
    public static final String ID_SUBSTITUTION_MARKER = "#i";
    public static final String SENDER_CALLSIGN_SUBSTITUION_MARKER = "#s";

    public static final String VALUE_USER_GROUPS = "UserGroups";
    public static final String USER_GROUPS_SUBSTITUTION_MARKER = "#m";

    public static final String VALUE_GROUPS = "Groups";
    public static final String GROUPS_SUBSTITUION_MARKER = "#g";

    public String uidFromGeoChat;
    public String chatroomFromGeoChat;
    public String idFromChat;
    public String senderCallsignFromChat;
    public List<String> uidsFromChatGroup = new ArrayList<>();
    public List<String> uidsFromRoute = new ArrayList<>();
    public String uidFromVideo;
}