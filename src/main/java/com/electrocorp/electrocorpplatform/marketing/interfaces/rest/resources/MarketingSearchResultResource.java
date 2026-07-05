package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

public record MarketingSearchResultResource(
        String term,
        String sectionId,
        String label,
        boolean found
) {
}
