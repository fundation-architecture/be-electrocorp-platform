package com.electrocorp.electrocorpplatform.iam.application.services;

import com.electrocorp.electrocorpplatform.iam.application.queryservices.AccessProfileQueryService;
import com.electrocorp.electrocorpplatform.iam.application.results.AccessProfileDetails;
import com.electrocorp.electrocorpplatform.iam.domain.repositories.AccessProfileRepository;
import com.electrocorp.electrocorpplatform.iam.domain.services.AccessProfilePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessProfileApplicationService implements AccessProfileQueryService {

    private final AccessProfileRepository accessProfileRepository;
    private final AccessProfilePolicyService accessProfilePolicyService;

    @Override
    @Transactional(readOnly = true)
    public List<AccessProfileDetails> getAccessProfiles() {
        return accessProfileRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(profile -> profileOrder(profile.getName())))
                .map(profile -> new AccessProfileDetails(
                        profile,
                        accessProfilePolicyService.resolvePermissions(profile)
                ))
                .toList();
    }

    private int profileOrder(String profileName) {
        String normalized = profileName == null ? "" : profileName.trim().toUpperCase();

        return switch (normalized) {
            case "OWNER" -> 1;
            case "ADMIN" -> 2;
            case "MEMBER" -> 3;
            case "GUEST" -> 4;
            default -> 99;
        };
    }
}
