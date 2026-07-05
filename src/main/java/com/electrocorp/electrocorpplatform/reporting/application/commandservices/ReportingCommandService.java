package com.electrocorp.electrocorpplatform.reporting.application.commandservices;

import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.EnergyGoal;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.CreateConsumptionReportCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.CreateEnergyGoalCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.GenerateConsumptionReportCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.RecordReportingEventCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.UpdateEnergyGoalCommand;

public interface ReportingCommandService {
    ConsumptionReport handle(CreateConsumptionReportCommand command);
    ConsumptionReport handle(GenerateConsumptionReportCommand command);
    void deleteReport(Long userId, Long reportId);
    EnergyGoal handle(CreateEnergyGoalCommand command);
    EnergyGoal handle(UpdateEnergyGoalCommand command);
    void deleteGoal(Long userId, Long goalId);
    ReportingEvent handle(RecordReportingEventCommand command);
}
