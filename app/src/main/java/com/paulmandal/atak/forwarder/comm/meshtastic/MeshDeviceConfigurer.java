package com.paulmandal.atak.forwarder.comm.meshtastic;

public class MeshDeviceConfigurer {
    // Listens to connect, when we get it the first time it does setDeviceAddress with MeshtasticDeviceSwitcher
    // on every subsequent connect, we check channel/radioconfig against our settings
    // if they don't match, we do setRadio
}
