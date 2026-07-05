package com.electrocorp.electrocorpplatform.marketing.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NewsletterSubscriptionRequestResource(
        @NotBlank
        @Email
        String email,
        String source
) {
}
