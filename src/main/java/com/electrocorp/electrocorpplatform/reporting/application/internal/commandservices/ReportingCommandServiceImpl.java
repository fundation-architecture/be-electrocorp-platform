package com.electrocorp.electrocorpplatform.reporting.application.internal.commandservices;

import com.electrocorp.electrocorpplatform.reporting.application.commandservices.ReportingCommandService;
import com.electrocorp.electrocorpplatform.reporting.application.services.ReportingApplicationService;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.EnergyGoal;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.CreateConsumptionReportCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.CreateEnergyGoalCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.GenerateConsumptionReportCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.RecordReportingEventCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.UpdateEnergyGoalCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportingCommandServiceImpl implements ReportingCommandService {
    private final ReportingApplicationService applicationService;

    @Override
    public ConsumptionReport handle(CreateConsumptionReportCommand command) {
        return applicationService.createReport(command);
    }

    @Override
    public ConsumptionReport handle(GenerateConsumptionReportCommand command) {
        return applicationService.generateReport(command);
    }

    @Override
    public void deleteReport(Long userId, Long reportId) {
        applicationService.deleteReport(userId, reportId);
    }

    @Override
    public EnergyGoal handle(CreateEnergyGoalCommand command) {
        return applicationService.createGoal(command);
    }

    @Override
    public EnergyGoal handle(UpdateEnergyGoalCommand command) {
        return applicationService.updateGoal(command);
    }

    @Override
    public void deleteGoal(Long userId, Long goalId) {
        applicationService.deleteGoal(userId, goalId);
    }

    @Override
    public ReportingEvent handle(RecordReportingEventCommand command) {
        return applicationService.recordEvent(command);
    }
}
