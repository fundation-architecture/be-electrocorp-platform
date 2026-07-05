package com.electrocorp.electrocorpplatform.shared.application.result;

public record ApplicationError(
        String code,
        String message
) {
    public static ApplicationError notFound(String message) {
        return new ApplicationError("RESOURCE_NOT_FOUND", message);
    }

    public static ApplicationError conflict(String message) {
        return new ApplicationError("CONFLICT", message);
    }

    public static ApplicationError businessRuleViolation(String message) {
        return new ApplicationError("DOMAIN_RULE_VIOLATION", message);
    }

    public static ApplicationError badRequest(String message) {
        return new ApplicationError("BAD_REQUEST", message);
    }

    public static ApplicationError unexpected(String message) {
        return new ApplicationError("INTERNAL_SERVER_ERROR", message);
    }
}
