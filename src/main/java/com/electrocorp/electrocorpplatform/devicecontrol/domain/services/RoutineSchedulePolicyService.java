package com.electrocorp.electrocorpplatform.devicecontrol.domain.services;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.RoutineRepeatType;
import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates.Routine;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoutineSchedulePolicyService {

    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";
    private static final Map<String, DayOfWeek> WEEK_DAY_CODES = Map.ofEntries(
            Map.entry("MON", DayOfWeek.MONDAY),
            Map.entry("MONDAY", DayOfWeek.MONDAY),
            Map.entry("LUN", DayOfWeek.MONDAY),
            Map.entry("1", DayOfWeek.MONDAY),
            Map.entry("TUE", DayOfWeek.TUESDAY),
            Map.entry("TUESDAY", DayOfWeek.TUESDAY),
            Map.entry("MAR", DayOfWeek.TUESDAY),
            Map.entry("2", DayOfWeek.TUESDAY),
            Map.entry("WED", DayOfWeek.WEDNESDAY),
            Map.entry("WEDNESDAY", DayOfWeek.WEDNESDAY),
            Map.entry("MIE", DayOfWeek.WEDNESDAY),
            Map.entry("3", DayOfWeek.WEDNESDAY),
            Map.entry("THU", DayOfWeek.THURSDAY),
            Map.entry("THURSDAY", DayOfWeek.THURSDAY),
            Map.entry("JUE", DayOfWeek.THURSDAY),
            Map.entry("4", DayOfWeek.THURSDAY),
            Map.entry("FRI", DayOfWeek.FRIDAY),
            Map.entry("FRIDAY", DayOfWeek.FRIDAY),
            Map.entry("VIE", DayOfWeek.FRIDAY),
            Map.entry("5", DayOfWeek.FRIDAY),
            Map.entry("SAT", DayOfWeek.SATURDAY),
            Map.entry("SATURDAY", DayOfWeek.SATURDAY),
            Map.entry("SAB", DayOfWeek.SATURDAY),
            Map.entry("6", DayOfWeek.SATURDAY),
            Map.entry("SUN", DayOfWeek.SUNDAY),
            Map.entry("SUNDAY", DayOfWeek.SUNDAY),
            Map.entry("DOM", DayOfWeek.SUNDAY),
            Map.entry("7", DayOfWeek.SUNDAY)
    );

    public void validateSchedule(
            String executionTime,
            RoutineRepeatType repeatType,
            String daysOfWeek,
            Integer intervalDays,
            String startsOn
    ) {
        requireExecutionTime(executionTime);

        RoutineRepeatType effectiveRepeatType = repeatType != null ? repeatType : RoutineRepeatType.DAILY;

        if (effectiveRepeatType == RoutineRepeatType.WEEKLY && parseScheduledDays(daysOfWeek).isEmpty()) {
            throw new IllegalArgumentException("Weekly routines require at least one day.");
        }

        if (effectiveRepeatType == RoutineRepeatType.CUSTOM_INTERVAL
                && (intervalDays == null || intervalDays < 1)) {
            throw new IllegalArgumentException("Routine interval must be greater than zero.");
        }

        if (effectiveRepeatType == RoutineRepeatType.ONCE) {
            requireStartDate(startsOn);
            return;
        }

        if (startsOn != null && !startsOn.isBlank()) {
            requireStartDate(startsOn);
        }
    }

    public boolean isDue(Routine routine, LocalDateTime now) {
        if (routine == null || now == null || !routine.isEnabled()) {
            return false;
        }

        Optional<LocalTime> executionTime = parseExecutionTime(routine.getExecutionTime());
        if (executionTime.isEmpty() || hasInvalidStartDate(routine.getStartsOn())) {
            return false;
        }

        return switch (routine.getEffectiveRepeatType()) {
            case ONCE -> isOnceDue(routine, now, executionTime.get());
            case DAILY -> isDailyDue(routine, now, executionTime.get());
            case WEEKLY -> isWeeklyDue(routine, now, executionTime.get());
            case CUSTOM_INTERVAL -> isCustomIntervalDue(routine, now, executionTime.get());
        };
    }

    private boolean isOnceDue(Routine routine, LocalDateTime now, LocalTime executionTime) {
        LocalDate scheduledDate = parseStartDate(routine.getStartsOn())
                .or(() -> Optional.ofNullable(routine.getCreatedAt()).map(LocalDateTime::toLocalDate))
                .orElse(now.toLocalDate());

        LocalDateTime scheduledAt = scheduledDate.atTime(executionTime);

        return hasReached(scheduledAt, now) && hasNotExecutedOccurrence(routine, scheduledAt);
    }

    private boolean isDailyDue(Routine routine, LocalDateTime now, LocalTime executionTime) {
        if (startsAfterToday(routine, now.toLocalDate())) {
            return false;
        }

        LocalDateTime scheduledAt = now.toLocalDate().atTime(executionTime);

        return hasReached(scheduledAt, now)
                && wasCreatedBeforeOccurrence(routine, scheduledAt)
                && hasNotExecutedOccurrence(routine, scheduledAt);
    }

    private boolean isWeeklyDue(Routine routine, LocalDateTime now, LocalTime executionTime) {
        Set<DayOfWeek> scheduledDays = parseScheduledDays(routine.getDaysOfWeek());

        if (scheduledDays.isEmpty()
                || !scheduledDays.contains(now.getDayOfWeek())
                || startsAfterToday(routine, now.toLocalDate())) {
            return false;
        }

        LocalDateTime scheduledAt = now.toLocalDate().atTime(executionTime);

        return hasReached(scheduledAt, now)
                && wasCreatedBeforeOccurrence(routine, scheduledAt)
                && hasNotExecutedOccurrence(routine, scheduledAt);
    }

    private boolean isCustomIntervalDue(Routine routine, LocalDateTime now, LocalTime executionTime) {
        LocalDate anchorDate = parseStartDate(routine.getStartsOn())
                .or(() -> Optional.ofNullable(routine.getCreatedAt()).map(LocalDateTime::toLocalDate))
                .orElse(now.toLocalDate());

        long daysFromAnchor = ChronoUnit.DAYS.between(anchorDate, now.toLocalDate());
        if (daysFromAnchor < 0 || daysFromAnchor % routine.getEffectiveIntervalDays() != 0) {
            return false;
        }

        LocalDateTime scheduledAt = now.toLocalDate().atTime(executionTime);

        return hasReached(scheduledAt, now)
                && wasCreatedBeforeOccurrence(routine, scheduledAt)
                && hasNotExecutedOccurrence(routine, scheduledAt);
    }

    private LocalTime requireExecutionTime(String executionTime) {
        if (executionTime == null || executionTime.isBlank()) {
            throw new IllegalArgumentException("Routine time is required.");
        }

        String trimmed = executionTime.trim();
        if (!trimmed.matches(TIME_PATTERN)) {
            throw new IllegalArgumentException("Routine time must use HH:mm format.");
        }

        return LocalTime.parse(trimmed);
    }

    private LocalDate requireStartDate(String startsOn) {
        if (startsOn == null || startsOn.isBlank()) {
            throw new IllegalArgumentException("Routine start date is required.");
        }

        try {
            return LocalDate.parse(startsOn.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Routine start date must use yyyy-MM-dd format.");
        }
    }

    private Optional<LocalTime> parseExecutionTime(String executionTime) {
        try {
            return Optional.of(requireExecutionTime(executionTime));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> parseStartDate(String startsOn) {
        if (startsOn == null || startsOn.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(startsOn.trim()));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private boolean hasInvalidStartDate(String startsOn) {
        return startsOn != null && !startsOn.isBlank() && parseStartDate(startsOn).isEmpty();
    }

    private boolean startsAfterToday(Routine routine, LocalDate today) {
        return parseStartDate(routine.getStartsOn())
                .map(today::isBefore)
                .orElse(false);
    }

    private Set<DayOfWeek> parseScheduledDays(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(daysOfWeek.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(WEEK_DAY_CODES::get)
                .filter(day -> day != null)
                .collect(Collectors.toSet());
    }

    private boolean hasReached(LocalDateTime scheduledAt, LocalDateTime now) {
        return !scheduledAt.isAfter(now);
    }

    private boolean wasCreatedBeforeOccurrence(Routine routine, LocalDateTime scheduledAt) {
        return routine.getCreatedAt() == null || !scheduledAt.isBefore(routine.getCreatedAt());
    }

    private boolean hasNotExecutedOccurrence(Routine routine, LocalDateTime scheduledAt) {
        return routine.getLastExecutedAt() == null || routine.getLastExecutedAt().isBefore(scheduledAt);
    }
}
