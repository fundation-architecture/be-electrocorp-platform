package com.electrocorp.electrocorpplatform.iam.domain.services;

import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessProfilePolicyServiceTest {

    private final AccessProfilePolicyService policyService = new AccessProfilePolicyService();

    @Test
    void ownerCanManageBillingAndAccess() {
        assertTrue(policyService.resolvePermissions("OWNER").contains(AccessPermission.MANAGE_BILLING));
        assertTrue(policyService.resolvePermissions("OWNER").contains(AccessPermission.MANAGE_ACCESS));
    }

    @Test
    void guestCanOnlyUseLimitedDailyControl() {
        var permissions = policyService.resolvePermissions("GUEST");

        assertTrue(permissions.contains(AccessPermission.CONTROL_DEVICES));
        assertFalse(permissions.contains(AccessPermission.MANAGE_ACCESS));
        assertFalse(permissions.contains(AccessPermission.VIEW_REPORTS));
    }
}
