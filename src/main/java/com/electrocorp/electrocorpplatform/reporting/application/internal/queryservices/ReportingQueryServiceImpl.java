package com.electrocorp.electrocorpplatform.reporting.application.internal.queryservices;

import com.electrocorp.electrocorpplatform.reporting.application.queryservices.ReportingQueryService;
import com.electrocorp.electrocorpplatform.reporting.application.services.ReportingApplicationService;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.EnergyGoal;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.domain.model.queries.GetConsumptionReportsByUserQuery;
import com.electrocorp.electrocorpplatform.reporting.domain.model.queries.GetEnergyGoalsByUserQuery;
import com.electrocorp.electrocorpplatform.reporting.domain.model.queries.GetReportingEventsByUserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingQueryServiceImpl implements ReportingQueryService {
    private final ReportingApplicationService applicationService;

    @Override
    public List<ConsumptionReport> handle(GetConsumptionReportsByUserQuery query) {
        return applicationService.getReports(query.userId());
    }

    @Override
    public List<EnergyGoal> handle(GetEnergyGoalsByUserQuery query) {
        return applicationService.getGoals(query.userId());
    }

    @Override
    public List<ReportingEvent> handle(GetReportingEventsByUserQuery query) {
        return applicationService.getEvents(query.userId());
    }
}
