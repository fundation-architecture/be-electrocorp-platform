package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

import java.util.List;

public record LandingContentResource(
        String language,
        String heroTitle,
        String heroSubtitle,
        List<String> valuePropositions,
        List<PlanSummaryResource> plans,
        List<TestimonialResource> testimonials
) {
    public record PlanSummaryResource(
            String code,
            String name,
            String price,
            String description
    ) {
    }

    public record TestimonialResource(
            String author,
            String role,
            String quote
    ) {
    }
}
