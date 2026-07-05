package com.electrocorp.electrocorpplatform.iam.application.queryservices;

import com.electrocorp.electrocorpplatform.iam.application.results.AccessProfileDetails;

import java.util.List;

public interface AccessProfileQueryService {
    List<AccessProfileDetails> getAccessProfiles();
}
