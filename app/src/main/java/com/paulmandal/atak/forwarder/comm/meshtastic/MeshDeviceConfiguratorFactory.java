package com.paulmandal.atak.forwarder.comm.meshtastic;

import com.geeksville.mesh.ConfigProtos;
import com.paulmandal.atak.forwarder.helpers.HashHelper;
import com.paulmandal.atak.forwarder.helpers.Logger;

public class MeshDeviceConfiguratorFactory {
    public MeshDeviceConfigurator createMeshDeviceConfigurator(MeshServiceController meshServiceController,
                                                               DeviceConnectionHandler deviceConnectionHandler,
                                                               MeshtasticDeviceSwitcher meshtasticDeviceSwitcher,
                                                               HashHelper hashHelper,
                                                               Logger logger,
                                                               MeshtasticDevice meshtasticDevice,
                                                               String longName,
                                                               String shortName,
                                                               ConfigProtos.Config.LoRaConfig.RegionCode regionCode,
                                                               int pliUpdateInterval,
                                                               boolean gpsEnabled,
                                                               int screenOnSecs,
                                                               String channelName,
                                                               int channelMode,
                                                               byte[] channelPsk,
                                                               ConfigProtos.Config.DeviceConfig.Role routingRole,
                                                               boolean writeToDevice) {
        return new MeshDeviceConfigurator(
                meshServiceController,
                deviceConnectionHandler,
                meshtasticDeviceSwitcher,
                hashHelper,
                logger,
                meshtasticDevice,
                longName,
                shortName,
                regionCode,
                pliUpdateInterval,
                gpsEnabled,
                screenOnSecs,
                channelName,
                channelMode,
                channelPsk,
                routingRole,
                writeToDevice);
    }
}
