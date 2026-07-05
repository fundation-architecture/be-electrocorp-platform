package com.electrocorp.electrocorpplatform.reporting.application.services;

import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.UserRepository;
import com.electrocorp.electrocorpplatform.notifications.application.commandservices.NotificationCommandService;
import com.electrocorp.electrocorpplatform.notifications.domain.model.aggregates.NotificationPreference;
import com.electrocorp.electrocorpplatform.notifications.domain.model.commands.CreateAlertCommand;
import com.electrocorp.electrocorpplatform.notifications.domain.repositories.NotificationPreferenceRepository;
import com.electrocorp.electrocorpplatform.reporting.application.commandservices.ReportingCommandService;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.GenerateConsumptionReportCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MonthlyConsumptionReportSchedulerServiceTest {

    @Test
    void generatesMonthlyReportAndNotificationForActiveUser() {
        UserRepository userRepository = mock(UserRepository.class);
        NotificationPreferenceRepository preferenceRepository = mock(NotificationPreferenceRepository.class);
        ReportingCommandService reportingCommandService = mock(ReportingCommandService.class);
        NotificationCommandService notificationCommandService = mock(NotificationCommandService.class);
        MonthlyConsumptionReportSchedulerService scheduler = new MonthlyConsumptionReportSchedulerService(
                userRepository,
                preferenceRepository,
                reportingCommandService,
                notificationCommandService
        );
        User user = activeUser(42L);
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        ConsumptionReport report = new ConsumptionReport(
                42L,
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(320),
                start,
                end
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(preferenceRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(reportingCommandService.handle(argThat((GenerateConsumptionReportCommand command) ->
                command.userId().equals(42L)
                        && command.startDate().equals(start)
                        && command.endDate().equals(end)
        ))).thenReturn(report);

        scheduler.generateReportsForPeriod(start, end);

        verify(notificationCommandService).handle(argThat((CreateAlertCommand command) ->
                command.userId().equals(42L)
                        && command.sourceType().equals("REPORT")
                        && command.eventType().equals("REPORT_SUMMARY")
                        && command.threadKey().equals("REPORT:42:2026-06-01:2026-06-30")
        ));
    }

    @Test
    void skipsUserWhenMonthlyReportPreferenceIsDisabled() {
        UserRepository userRepository = mock(UserRepository.class);
        NotificationPreferenceRepository preferenceRepository = mock(NotificationPreferenceRepository.class);
        ReportingCommandService reportingCommandService = mock(ReportingCommandService.class);
        NotificationCommandService notificationCommandService = mock(NotificationCommandService.class);
        MonthlyConsumptionReportSchedulerService scheduler = new MonthlyConsumptionReportSchedulerService(
                userRepository,
                preferenceRepository,
                reportingCommandService,
                notificationCommandService
        );
        User user = activeUser(7L);
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(7L);
        preference.setMonthlyReportEnabled(false);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(preferenceRepository.findByUserId(7L)).thenReturn(Optional.of(preference));

        scheduler.generateReportsForPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        verifyNoInteractions(reportingCommandService, notificationCommandService);
    }

    private User activeUser(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setFullName("ElectroCorp User");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        return user;
    }
}
