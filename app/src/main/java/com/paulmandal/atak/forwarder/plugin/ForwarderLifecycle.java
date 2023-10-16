package com.paulmandal.atak.forwarder.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.paulmandal.atak.forwarder.plugin.ui.ForwarderMapComponent;

import gov.tak.api.plugin.IServiceController;

public class ForwarderLifecycle extends AbstractPlugin {
    public ForwarderLifecycle(IServiceController serviceController) {
        super(serviceController, new ForwarderMapComponent());
    }
}
