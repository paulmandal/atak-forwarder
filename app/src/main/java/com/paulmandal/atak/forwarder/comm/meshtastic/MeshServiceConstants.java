package com.paulmandal.atak.forwarder.comm.meshtastic;

public class MeshServiceConstants {
    /**
     * Service Intent
     */
    public static final String PACKAGE_NAME = "com.geeksville.mesh";
    public static final String CLASS_NAME = "com.geeksville.mesh.service.MeshService";

    /**
     * Intents the Meshtastic service can send
     */
    public static final String ACTION_MESH_CONNECTED = "com.geeksville.mesh.MESH_CONNECTED";
    public static final String ACTION_RECEIVED_ATAK_FORWARDER = "com.geeksville.mesh.RECEIVED.ATAK_FORWARDER";
    public static final String ACTION_RECEIVED_NODEINFO_APP = "com.geeksville.mesh.RECEIVED.NODEINFO_APP";
    public static final String ACTION_RECEIVED_POSITION_APP = "com.geeksville.mesh.RECEIVED.POSITION_APP";
    public static final String ACTION_NODE_CHANGE = "com.geeksville.mesh.NODE_CHANGE";
    public static final String ACTION_MESSAGE_STATUS = "com.geeksville.mesh.MESSAGE_STATUS";

    /**
     * Extra data fields from the Meshtastic service
     */
    public static final String EXTRA_CONNECTED = "com.geeksville.mesh.Connected";
    public static final String EXTRA_PERMANENT = "com.geeksville.mesh.Permanent";

    public static final String EXTRA_PAYLOAD = "com.geeksville.mesh.Payload";
    public static final String EXTRA_NODEINFO = "com.geeksville.mesh.NodeInfo";
    public static final String EXTRA_PACKET_ID = "com.geeksville.mesh.PacketId";
    public static final String EXTRA_STATUS = "com.geeksville.mesh.Status";

    public static final String STATE_CONNECTED = "CONNECTED";
    public static final String STATE_DISCONNECTED = "DISCONNECTED";
    public static final String STATE_DEVICE_SLEEP = "DEVICE_SLEEP";
}
