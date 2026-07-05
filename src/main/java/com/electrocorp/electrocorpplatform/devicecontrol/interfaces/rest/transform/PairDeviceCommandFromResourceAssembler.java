package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.PairDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.PairDeviceResource;

public class PairDeviceCommandFromResourceAssembler {
    public static PairDeviceCommand toCommandFromResource(PairDeviceResource resource, Long userId) {
        return new PairDeviceCommand(
                userId,
                resource.pairingCode(),
                resource.alias(),
                resource.room()
        );
    }
}
