package com.electrocorp.electrocorpplatform.reporting.interfaces.rest.transform;

import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.interfaces.rest.resources.ReportingEventResource;

public final class ReportingEventResourceFromEntityAssembler {

    private ReportingEventResourceFromEntityAssembler() {
    }

    public static ReportingEventResource toResourceFromEntity(ReportingEvent event) {
        return new ReportingEventResource(
                event.getId(),
                event.getEventName(),
                event.getSourceContext(),
                event.getSubjectType(),
                event.getSubjectId(),
                event.getSummary(),
                event.getDetail(),
                event.getOccurredOn()
        );
    }
}
