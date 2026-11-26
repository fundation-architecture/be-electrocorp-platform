package com.electrocorp.electrocorpplatform.devicecontrol.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.devicecontrol.domain.model.*;

import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routines")
public class Routine extends AuditableEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = true)
    private Long deviceId;

    @Column(nullable = true)
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineAction action;

    @Column(nullable = false, length = 10)
    private String time;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private RoutineRepeatType repeatType = RoutineRepeatType.DAILY;

    @Column(length = 40)
    private String daysOfWeek;

    @Column
    private Integer intervalDays = 1;

    @Column(length = 10)
    private String startsOn;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_execution_status", length = 20)
    private RoutineExecutionStatus lastExecutionStatus;

    @Column(name = "last_execution_message", length = 255)
    private String lastExecutionMessage;

    /** Momento en que se cambio por ultima vez el horario (US-12). */
    @Column(name = "schedule_changed_at")
    private LocalDateTime scheduleChangedAt;

    public void toggle() {
        this.enabled = this.enabled == null || !this.enabled;
    }

    public boolean isEnabled() {
        return this.enabled == null || this.enabled;
    }

    public String getExecutionTime() {
        return this.time;
    }

    public void markExecutedAt(LocalDateTime executedAt) {
        this.lastExecutedAt = executedAt != null ? executedAt : LocalDateTime.now();

        if (getEffectiveRepeatType() == RoutineRepeatType.ONCE) {
            this.enabled = false;
        }
    }

    /**
     * Registra el resultado de la ejecucion de la ocurrencia programada. La ocurrencia queda
     * marcada como ejecutada aunque no se haya aplicado, para no reintentarla cada minuto.
     */
    public void recordExecution(RoutineExecutionStatus status, String message, LocalDateTime executedAt) {
        this.lastExecutionStatus = status;
        this.lastExecutionMessage = message;
        markExecutedAt(executedAt);
    }

    /**
     * Fecha desde la que cuentan las ocurrencias del horario vigente: la creacion de la rutina o
     * su ultima modificacion de horario. Las ocurrencias anteriores no se ejecutan.
     */
    public LocalDateTime getScheduleEffectiveFrom() {
        if (scheduleChangedAt == null) {
            return getCreatedAt();
        }
        if (getCreatedAt() == null || scheduleChangedAt.isAfter(getCreatedAt())) {
            return scheduleChangedAt;
        }
        return getCreatedAt();
    }

    public RoutineRepeatType getEffectiveRepeatType() {
        return this.repeatType != null ? this.repeatType : RoutineRepeatType.DAILY;
    }

    public Integer getEffectiveIntervalDays() {
        return this.intervalDays != null && this.intervalDays > 0 ? this.intervalDays : 1;
    }
}
