package com.electrocorp.electrocorpplatform.iam.domain.services;

import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.AccessProfile;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class AccessProfilePolicyService {

    public Set<AccessPermission> resolvePermissions(AccessProfile profile) {
        return resolvePermissions(profile != null ? profile.getName() : null);
    }

    public Set<AccessPermission> resolvePermissions(String profileName) {
        String normalized = profileName == null ? "GUEST" : profileName.trim().toUpperCase();

        return switch (normalized) {
            case "OWNER" -> EnumSet.allOf(AccessPermission.class);
            case "ADMIN" -> EnumSet.of(
                    AccessPermission.VIEW_HOME,
                    AccessPermission.CONTROL_DEVICES,
                    AccessPermission.MANAGE_DEVICES,
                    AccessPermission.MANAGE_ROUTINES,
                    AccessPermission.MANAGE_SPACES,
                    AccessPermission.VIEW_ENERGY,
                    AccessPermission.VIEW_REPORTS,
                    AccessPermission.MANAGE_ALERTS,
                    AccessPermission.MANAGE_SUPPORT,
                    AccessPermission.MANAGE_ACCESS
            );
            case "MEMBER" -> EnumSet.of(
                    AccessPermission.VIEW_HOME,
                    AccessPermission.CONTROL_DEVICES,
                    AccessPermission.MANAGE_DEVICES,
                    AccessPermission.MANAGE_ROUTINES,
                    AccessPermission.VIEW_ENERGY,
                    AccessPermission.VIEW_REPORTS,
                    AccessPermission.MANAGE_ALERTS,
                    AccessPermission.MANAGE_SUPPORT
            );
            default -> EnumSet.of(
                    AccessPermission.VIEW_HOME,
                    AccessPermission.CONTROL_DEVICES
            );
        };
    }

    public boolean hasPermission(AccessProfile profile, AccessPermission permission) {
        return resolvePermissions(profile).contains(permission);
    }
}
