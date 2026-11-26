package com.electrocorp.electrocorpplatform.energymonitoring.application.queryservices;

import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergyDashboardSummaryQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergyReadingsByUserQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetEnergySamplingSettingsQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.queries.GetPeriodCostSummaryQuery;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.PeriodCostSummaryResource;
import com.electrocorp.electrocorpplatform.energymonitoring.interfaces.rest.resources.EnergyDashboardSummaryResource;

import java.util.List;

public interface EnergyMonitoringQueryService {
    List<EnergyReading> handle(GetEnergyReadingsByUserQuery query);
    EnergyDashboardSummaryResource handle(GetEnergyDashboardSummaryQuery query);
    Integer handle(GetEnergySamplingSettingsQuery query);
    PeriodCostSummaryResource handle(GetPeriodCostSummaryQuery query);
}
