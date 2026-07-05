package com.electrocorp.electrocorpplatform.reporting.application.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.entities.DeviceGroupDevice;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceGroupDeviceRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceGroupRepository;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.repositories.DeviceRepository;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.model.aggregates.EnergyReading;
import com.electrocorp.electrocorpplatform.energymonitoring.domain.repositories.EnergyReadingRepository;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.CreateConsumptionReportCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.CreateEnergyGoalCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.GenerateConsumptionReportCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.RecordReportingEventCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.commands.UpdateEnergyGoalCommand;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ConsumptionReport;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.EnergyGoal;
import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.domain.repositories.ConsumptionReportRepository;
import com.electrocorp.electrocorpplatform.reporting.domain.repositories.EnergyGoalRepository;
import com.electrocorp.electrocorpplatform.reporting.domain.repositories.ReportingEventRepository;
import com.electrocorp.electrocorpplatform.reporting.domain.services.ConsumptionReportPolicyService;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.DeviceAssignment;
import com.electrocorp.electrocorpplatform.workplace.domain.model.aggregates.Room;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.DeviceAssignmentRepository;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.LocationRepository;
import com.electrocorp.electrocorpplatform.workplace.domain.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingApplicationService {

    private final ConsumptionReportRepository reportRepository;
    private final EnergyGoalRepository goalRepository;
    private final ReportingEventRepository reportingEventRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final DeviceGroupDeviceRepository deviceGroupDeviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;
    private final ConsumptionReportPolicyService reportPolicyService;

    @Transactional(readOnly = true)
    public List<ReportingEvent> getEvents(Long userId) {
        return reportingEventRepository.findTop50ByUserIdOrderByOccurredOnDescIdDesc(userId);
    }

    @Transactional
    public ReportingEvent recordEvent(RecordReportingEventCommand command) {
        LocalDateTime occurredOn = command.occurredOn() != null ? command.occurredOn() : LocalDateTime.now();
        String eventKey = buildEventKey(command, occurredOn);

        return reportingEventRepository.findByEventKey(eventKey)
                .orElseGet(() -> reportingEventRepository.save(new ReportingEvent(
                        command.userId(),
                        eventKey,
                        normalize(command.eventName(), "UNKNOWN_EVENT", 120),
                        normalize(command.sourceContext(), "UNKNOWN", 80),
                        normalize(command.subjectType(), "SYSTEM", 80),
                        normalizeNullable(command.subjectId(), 80),
                        normalize(command.summary(), "Platform activity recorded.", 240),
                        normalizeNullable(command.detail(), 800),
                        occurredOn
                )));
    }

    @Transactional(readOnly = true)
    public List<ConsumptionReport> getReports(Long userId) {
        Set<String> existingCycles = new HashSet<>();

        return reportRepository.findByUserIdOrderByStartDateAscEndDateAscIdAsc(userId)
                .stream()
                .filter(report -> existingCycles.add(reportCycleKey(report)))
                .toList();
    }

    @Transactional
    public ConsumptionReport createReport(CreateConsumptionReportCommand command) {
        reportPolicyService.validateReportRange(command.startDate(), command.endDate());

        ConsumptionReport existingReport = reportRepository
                .findByUserIdAndStartDateAndEndDateOrderByIdAsc(command.userId(), command.startDate(), command.endDate())
                .stream()
                .findFirst()
                .orElse(null);

        if (existingReport != null && isClosedReportCycle(command.endDate())) {
            return existingReport;
        }

        var startDateTime = command.startDate().atStartOfDay();
        var endDateTime = command.endDate().atTime(LocalTime.MAX);

        List<EnergyReading> readings = energyReadingRepository
                .findByUserIdAndRecordedAtBetween(
                        command.userId(),
                        startDateTime,
                        endDateTime
                );

        if (readings.isEmpty()) {
            if (existingReport != null) {
                return existingReport;
            }
            throw new IllegalArgumentException("There are no energy readings for the selected date range.");
        }

        BigDecimal totalWatts = readings.stream()
                .map(EnergyReading::getWatts)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageWatts = totalWatts.divide(
                BigDecimal.valueOf(readings.size()),
                2,
                java.math.RoundingMode.HALF_UP
        );

        BigDecimal highestWatts = readings.stream()
                .map(EnergyReading::getWatts)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        ConsumptionReport report = existingReport != null
                ? existingReport
                : new ConsumptionReport(
                        command.userId(),
                        totalWatts,
                        averageWatts,
                        highestWatts,
                        command.startDate(),
                        command.endDate()
                );

        if (existingReport != null) {
            report.refreshMetrics(totalWatts, averageWatts, highestWatts);
        }

        return reportRepository.save(report);
    }

    @Transactional
    public ConsumptionReport generateReport(GenerateConsumptionReportCommand command) {
        return createReport(new CreateConsumptionReportCommand(
                command.userId(),
                command.startDate(),
                command.endDate()
        ));
    }

    private boolean isClosedReportCycle(LocalDate endDate) {
        return endDate.isBefore(LocalDate.now());
    }

    private String reportCycleKey(ConsumptionReport report) {
        return report.getStartDate() + ":" + report.getEndDate();
    }

    @Transactional
    public void deleteReport(Long userId, Long reportId) {
        ConsumptionReport report = reportRepository.findById(reportId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Consumption report not found."));
        reportRepository.delete(report);
    }

    @Transactional(readOnly = true)
    public List<EnergyGoal> getGoals(Long userId) {
        return goalRepository.findByUserId(userId)
                .stream()
                .map(this::refreshGoalProgress)
                .toList();
    }

    @Transactional
    public EnergyGoal createGoal(CreateEnergyGoalCommand command) {
        String scopeType = normalizeScopeType(command.scopeType());
        if (!"GENERAL".equals(scopeType) && command.scopeId() == null) {
            throw new IllegalArgumentException("Scoped energy goals require a scope id.");
        }
        validateGoalScope(command.userId(), scopeType, command.scopeId());

        EnergyGoal goal = new EnergyGoal(
                command.userId(),
                command.title(),
                normalizeLegacyTargetKilowattHours(command.targetKilowattHours()),
                BigDecimal.ZERO,
                command.deadline(),
                "ACTIVE",
                scopeType,
                command.scopeId(),
                command.scopeName(),
                command.activeFrom(),
                command.activeTo()
        );

        return goalRepository.save(goal);
    }

    @Transactional
    public EnergyGoal updateGoal(UpdateEnergyGoalCommand command) {
        EnergyGoal goal = goalRepository.findById(command.goalId())
                .filter(item -> item.getUserId().equals(command.userId()))
                .orElseThrow(() -> new IllegalArgumentException("Energy goal not found."));

        String scopeType = normalizeScopeType(command.scopeType());
        if (!"GENERAL".equals(scopeType) && command.scopeId() == null) {
            throw new IllegalArgumentException("Scoped energy goals require a scope id.");
        }
        validateGoalScope(command.userId(), scopeType, command.scopeId());

        if (command.title() != null && !command.title().trim().isEmpty()) {
            goal.setTitle(command.title().trim());
        }
        if (command.targetKilowattHours() != null) {
            goal.setTargetKilowattHours(normalizeLegacyTargetKilowattHours(command.targetKilowattHours()));
        }
        if (command.currentKilowattHours() != null) {
            goal.setCurrentKilowattHours(command.currentKilowattHours());
        }
        if (command.deadline() != null) {
            goal.setDeadline(command.deadline());
        }
        if (command.status() != null && !command.status().trim().isEmpty()) {
            goal.setStatus(command.status().trim().toUpperCase());
        }
        goal.setScopeType(scopeType);
        goal.setScopeId(command.scopeId());
        goal.setScopeName(command.scopeName());
        goal.setActiveFrom(command.activeFrom());
        goal.setActiveTo(command.activeTo());

        return goalRepository.save(refreshGoalProgress(goal));
    }

    @Transactional
    public void deleteGoal(Long userId, Long goalId) {
        EnergyGoal goal = goalRepository.findById(goalId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Energy goal not found."));
        goalRepository.delete(goal);
    }

    private EnergyGoal refreshGoalProgress(EnergyGoal goal) {
        goal.setTargetKilowattHours(normalizeLegacyTargetKilowattHours(goal.getTargetKilowattHours()));

        var startDateTime = goal.getCreatedAt() != null
                ? goal.getCreatedAt()
                : goal.getDeadline().atStartOfDay();
        var endDateTime = goal.getDeadline().atTime(LocalTime.MAX);

        Set<Long> scopedDeviceIds = getScopedDeviceIds(goal);

        BigDecimal currentKilowattHours = energyReadingRepository
                .findByUserIdAndRecordedAtBetween(goal.getUserId(), startDateTime, endDateTime)
                .stream()
                .filter(reading -> matchesGoalScope(reading, goal, scopedDeviceIds))
                .filter(reading -> matchesActiveWindow(reading, goal))
                .map(reading -> reading.getKilowattHours() != null ? reading.getKilowattHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        goal.setCurrentKilowattHours(currentKilowattHours);

        if (currentKilowattHours.compareTo(goal.getTargetKilowattHours()) > 0) {
            goal.setStatus("FAILED");
        } else {
            goal.setStatus("ACTIVE");
        }

        return goal;
    }

    private boolean matchesGoalScope(EnergyReading reading, EnergyGoal goal, Set<Long> scopedDeviceIds) {
        String scopeType = normalizeScopeType(goal.getScopeType());

        if ("DEVICE".equals(scopeType)) {
            return goal.getScopeId() != null && goal.getScopeId().equals(reading.getDeviceId());
        }

        if ("GROUP".equals(scopeType) || "WORKPLACE".equals(scopeType) || "ROOM".equals(scopeType)) {
            return scopedDeviceIds.contains(reading.getDeviceId());
        }

        return true;
    }

    private boolean matchesActiveWindow(EnergyReading reading, EnergyGoal goal) {
        if (goal.getActiveFrom() == null || goal.getActiveTo() == null || reading.getRecordedAt() == null) {
            return true;
        }

        LocalTime recordedTime = reading.getRecordedAt().toLocalTime();
        LocalTime activeFrom = goal.getActiveFrom();
        LocalTime activeTo = goal.getActiveTo();

        if (activeFrom.equals(activeTo)) {
            return true;
        }

        if (activeFrom.isBefore(activeTo)) {
            return !recordedTime.isBefore(activeFrom) && recordedTime.isBefore(activeTo);
        }

        return !recordedTime.isBefore(activeFrom) || recordedTime.isBefore(activeTo);
    }

    private Set<Long> getScopedDeviceIds(EnergyGoal goal) {
        String scopeType = normalizeScopeType(goal.getScopeType());

        if (goal.getScopeId() == null) {
            return Set.of();
        }

        return switch (scopeType) {
            case "GROUP" -> deviceGroupDeviceRepository.findByDeviceGroupId(goal.getScopeId())
                    .stream()
                    .map(DeviceGroupDevice::getDeviceId)
                    .collect(Collectors.toSet());
            case "WORKPLACE" -> deviceAssignmentRepository.findByLocationId(goal.getScopeId())
                    .stream()
                    .map(DeviceAssignment::getDeviceId)
                    .collect(Collectors.toSet());
            case "ROOM" -> deviceAssignmentRepository.findByRoomId(goal.getScopeId())
                    .stream()
                    .map(DeviceAssignment::getDeviceId)
                    .collect(Collectors.toSet());
            default -> Set.of();
        };
    }

    private void validateGoalScope(Long userId, String scopeType, Long scopeId) {
        switch (scopeType) {
            case "DEVICE" -> deviceRepository.findByIdAndUserId(scopeId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Device scope was not found for this user."));
            case "GROUP" -> deviceGroupRepository.findById(scopeId)
                    .filter(group -> group.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Group scope was not found for this user."));
            case "WORKPLACE" -> locationRepository.findById(scopeId)
                    .filter(location -> location.getUserId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Workplace scope was not found for this user."));
            case "ROOM" -> {
                Room room = roomRepository.findById(scopeId)
                        .orElseThrow(() -> new IllegalArgumentException("Room scope was not found."));
                boolean belongsToUser = locationRepository.findById(room.getLocationId())
                        .map(location -> location.getUserId().equals(userId))
                        .orElse(false);
                if (!belongsToUser) {
                    throw new IllegalArgumentException("Room scope was not found for this user.");
                }
            }
            default -> {
            }
        }
    }

    private String normalizeScopeType(String scopeType) {
        if (scopeType == null || scopeType.isBlank()) {
            return "GENERAL";
        }

        String normalized = scopeType.trim().toUpperCase();
        return switch (normalized) {
            case "LOCATION" -> "WORKPLACE";
            case "DEVICE", "GROUP", "WORKPLACE", "ROOM" -> normalized;
            default -> "GENERAL";
        };
    }

    private BigDecimal normalizeLegacyTargetKilowattHours(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        boolean looksLikeLegacyWattsTemplate = value.compareTo(BigDecimal.valueOf(100)) >= 0
                && value.compareTo(BigDecimal.valueOf(10000)) <= 0
                && value.stripTrailingZeros().scale() <= 0;

        if (!looksLikeLegacyWattsTemplate) {
            return value;
        }

        return value.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private String buildEventKey(RecordReportingEventCommand command, LocalDateTime occurredOn) {
        return String.join(
                ":",
                normalize(command.sourceContext(), "UNKNOWN", 40),
                normalize(command.eventName(), "UNKNOWN_EVENT", 80),
                normalize(command.subjectType(), "SYSTEM", 40),
                normalize(command.subjectId(), "N/A", 60),
                occurredOn.toString()
        );
    }

    private String normalize(String value, String fallback, int maxLength) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String normalizeNullable(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().length() <= maxLength ? value.trim() : value.trim().substring(0, maxLength);
    }
}
