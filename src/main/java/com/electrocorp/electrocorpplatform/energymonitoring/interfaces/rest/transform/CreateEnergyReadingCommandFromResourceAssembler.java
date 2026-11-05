package com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.CreateEnergyReadingCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.CreateEnergyReadingResource;

public class CreateEnergyReadingCommandFromResourceAssembler {
    public static CreateEnergyReadingCommand toCommandFromResource(CreateEnergyReadingResource resource, Long userId) {
        return new CreateEnergyReadingCommand(
                userId,
                resource.deviceId(),
                resource.deviceName(),
                resource.watts(),
                resource.sampleSeconds()
        );
    }
}
