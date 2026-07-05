package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

import java.util.Map;

public record TranslationBundleResource(
        String language,
        Map<String, String> messages
) {
}
