package com.electrocorp.electrocorpplatform.reporting.application.queryservices;

import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.EnergyGoal;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.domain.model.queries.GetConsumptionReportsByUserQuery;
import com.electrocorp.electrocorpplatform.reporting.domain.model.queries.GetEnergyGoalsByUserQuery;
import com.electrocorp.electrocorpplatform.reporting.domain.model.queries.GetReportingEventsByUserQuery;

import java.util.List;

public interface ReportingQueryService {
    List<ConsumptionReport> handle(GetConsumptionReportsByUserQuery query);
    List<EnergyGoal> handle(GetEnergyGoalsByUserQuery query);
    List<ReportingEvent> handle(GetReportingEventsByUserQuery query);
}
