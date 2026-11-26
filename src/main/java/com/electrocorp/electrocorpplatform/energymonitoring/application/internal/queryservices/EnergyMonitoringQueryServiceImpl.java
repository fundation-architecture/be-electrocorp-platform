package com.electrocorp.electrocorpplatform.energymonitoring.application.internal.queryservices;

import com.electrocorp.electrocorpplatform.energymonitoring.application.queryservices.EnergyMonitoringQueryService;
import com.electrocorp.electrocorpplatform.energymonitoring.application.services.EnergyMonitoringApplicationService;
import com.electrocorp.electrocorpplatform.energymonitoring.application.services.EnergySamplingSettingsService;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergyDashboardSummaryQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergyReadingsByUserQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergySamplingSettingsQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetPeriodCostSummaryQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.PeriodCostSummaryResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnergyMonitoringQueryServiceImpl implements EnergyMonitoringQueryService {
    private final EnergyMonitoringApplicationService applicationService;
    private final EnergySamplingSettingsService settingsService;

    @Override
    public List<EnergyReading> handle(GetEnergyReadingsByUserQuery query) {
        if (query.start() != null && query.end() != null) {
            return applicationService.getReadings(query.userId(), query.start(), query.end());
        }

        return applicationService.getReadings(query.userId());
    }

    @Override
    public EnergyDashboardSummaryResource handle(GetEnergyDashboardSummaryQuery query) {
        return applicationService.getDashboardSummary(query.userId());
    }

    @Override
    public Integer handle(GetEnergySamplingSettingsQuery query) {
        return settingsService.getSampleSeconds();
    }

    @Override
    public PeriodCostSummaryResource handle(GetPeriodCostSummaryQuery query) {
        return applicationService.getPeriodCostSummary(query.userId(), query.from(), query.to());
    }
}
