package com.electrocorp.electrocorpplatform.reporting.interfaces.rest.resources;

import java.time.LocalDateTime;

public record ReportingEventResource(
        Long id,
        String eventName,
        String sourceContext,
        String subjectType,
        String subjectId,
        String summary,
        String detail,
        LocalDateTime occurredOn
) {
}
