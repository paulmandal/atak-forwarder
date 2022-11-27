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
                                                               String channelName,
                                                               int channelMode,
                                                               byte[] channelPsk,
                                                               ConfigProtos.Config.DeviceConfig.Role routingRole) {
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
                channelName,
                channelMode,
                channelPsk,
                routingRole);
    }
}
