package com.electrocorp.electrocorpplatform.reporting.application.services;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.notifications.application.commandservices.NotificationCommandService;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.CreateAlertCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.NotificationPreferenceRepository;
import com.electrocorp.electrocorpplatform.reporting.application.commandservices.ReportingCommandService;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.GenerateConsumptionReportCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MonthlyConsumptionReportSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonthlyConsumptionReportSchedulerService.class);

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ReportingCommandService reportingCommandService;
    private final NotificationCommandService notificationCommandService;

    @Scheduled(cron = "${electrocorp.reporting.monthly-cron:0 15 8 1 * *}", zone = "America/Lima")
    public void generatePreviousMonthReports() {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        generateReportsForPeriod(currentMonthStart.minusMonths(1), currentMonthStart.minusDays(1));
    }

    void generateReportsForPeriod(LocalDate startDate, LocalDate endDate) {
        userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .filter(this::monthlyEmailEnabled)
                .forEach(user -> generateReportForUser(user.getId(), startDate, endDate));
    }

    private boolean monthlyEmailEnabled(User user) {
        return preferenceRepository.findByUserId(user.getId())
                .map(preference -> Boolean.TRUE.equals(preference.getMonthlyReportEnabled())
                        && Boolean.TRUE.equals(preference.getEmailEnabled()))
                .orElse(true);
    }

    private void generateReportForUser(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            ConsumptionReport report = reportingCommandService.handle(
                    new GenerateConsumptionReportCommand(userId, startDate, endDate)
            );
            notifyMonthlyReportReady(userId, report, startDate, endDate);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Could not generate monthly consumption report for user {} between {} and {}.",
                    userId,
                    startDate,
                    endDate,
                    exception
            );
        }
    }

    private void notifyMonthlyReportReady(
            Long userId,
            ConsumptionReport report,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String reportId = report.getId() == null
                ? "%s_%s".formatted(startDate, endDate)
                : report.getId().toString();

        notificationCommandService.handle(new CreateAlertCommand(
                userId,
                "Reporte mensual listo",
                "Tu reporte mensual de consumo del %s al %s ya esta disponible.".formatted(startDate, endDate),
                "INFO",
                "REPORT",
                reportId,
                "Reporte mensual",
                "REPORT_SUMMARY",
                "REPORT:%s:%s:%s".formatted(userId, startDate, endDate),
                "Total W: %s. Promedio W: %s. Pico W: %s.".formatted(
                        report.getTotalWatts(),
                        report.getAverageWatts(),
                        report.getHighestWatts()
                ),
                "Generado automaticamente para el periodo mensual cerrado.",
                "Revisa el reporte desde la seccion de reportes y exportalo si tu plan lo permite.",
                45,
                LocalDateTime.now().plusDays(14).toString()
        ));
    }
}
