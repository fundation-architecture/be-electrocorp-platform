package com.electrocorp.electrocorpplatform.energymonitoring.application.internal.commandservices;

import com.electrocorp.electrocorpplatform.energymonitoring.application.commandservices.EnergyMonitoringCommandService;
import com.electrocorp.electrocorpplatform.energymonitoring.application.services.EnergyReadingIngestionService;
import com.electrocorp.electrocorpplatform.energymonitoring.application.services.EnergySamplingSettingsService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.CreateEnergyReadingCommand;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.commands.UpdateEnergySamplingSettingsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnergyMonitoringCommandServiceImpl implements EnergyMonitoringCommandService {
    private final EnergyReadingIngestionService ingestionService;
    private final EnergySamplingSettingsService settingsService;

    @Override
    public EnergyReading handle(CreateEnergyReadingCommand command) {
        return ingestionService.createReading(command);
    }

    @Override
    public Integer handle(UpdateEnergySamplingSettingsCommand command) {
        return settingsService.updateSampleSeconds(command.sampleSeconds());
    }
}
