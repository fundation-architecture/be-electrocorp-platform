package com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.commands.UpdateDeviceCommand;
import com.electrocorp.electrocorpplatform.devicecontrol.interfaces.rest.resources.UpdateDeviceResource;

public class UpdateDeviceCommandFromResourceAssembler {

    private UpdateDeviceCommandFromResourceAssembler() {
    }

    public static UpdateDeviceCommand toCommandFromResource(
            UpdateDeviceResource resource,
            Long userId,
            Long deviceId
    ) {
        return new UpdateDeviceCommand(
                userId,
                deviceId,
                resource.name(),
                resource.room(),
                resource.type(),
                resource.powerWatts()
        );
    }
}
