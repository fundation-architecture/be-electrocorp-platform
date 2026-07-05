package com.electrocorp.electrocorpplatform.iam.interfaces.rest.controllers;

import com.electrocorp.electrocorpplatform.iam.application.queryservices.AccessProfileQueryService;
import com.electrocorp.electrocorpplatform.iam.application.security.AccessAuthorizationService;
import com.electrocorp.electrocorpplatform.iam.domain.model.AccessPermission;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.resources.AccessProfileResource;
import com.electrocorp.electrocorpplatform.iam.interfaces.rest.transform.AccessProfileResourceFromResultAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/access-profiles")
@RequiredArgsConstructor
public class AccessProfileController {

    private final AccessProfileQueryService queryService;
    private final AccessAuthorizationService accessAuthorizationService;

    @GetMapping
    public List<AccessProfileResource> getAccessProfiles() {
        accessAuthorizationService.requirePermission(AccessPermission.MANAGE_ACCESS);

        return queryService.getAccessProfiles()
                .stream()
                .map(AccessProfileResourceFromResultAssembler::toResourceFromResult)
                .toList();
    }
}
