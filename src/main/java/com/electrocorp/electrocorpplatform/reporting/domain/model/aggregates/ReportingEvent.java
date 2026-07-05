package com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates;

import com.electrocorp.electrocorpplatform.shared.domain.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "reporting_events",
        indexes = {
                @Index(name = "idx_reporting_events_user_occurred", columnList = "user_id,occurred_on"),
                @Index(name = "idx_reporting_events_event_key", columnList = "event_key")
        }
)
public class ReportingEvent extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_key", nullable = false, unique = true, length = 240)
    private String eventKey;

    @Column(name = "event_name", nullable = false, length = 120)
    private String eventName;

    @Column(name = "source_context", nullable = false, length = 80)
    private String sourceContext;

    @Column(name = "subject_type", nullable = false, length = 80)
    private String subjectType;

    @Column(name = "subject_id", length = 80)
    private String subjectId;

    @Column(nullable = false, length = 240)
    private String summary;

    @Column(length = 800)
    private String detail;

    @Column(name = "occurred_on", nullable = false)
    private LocalDateTime occurredOn;
}
