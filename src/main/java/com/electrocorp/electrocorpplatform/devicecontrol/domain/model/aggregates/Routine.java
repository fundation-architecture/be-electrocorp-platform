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

    public RoutineRepeatType getEffectiveRepeatType() {
        return this.repeatType != null ? this.repeatType : RoutineRepeatType.DAILY;
    }

    public Integer getEffectiveIntervalDays() {
        return this.intervalDays != null && this.intervalDays > 0 ? this.intervalDays : 1;
    }
}
