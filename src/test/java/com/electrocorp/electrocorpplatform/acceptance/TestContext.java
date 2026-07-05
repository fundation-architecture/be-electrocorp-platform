package com.electrocorp.electrocorpplatform.acceptance;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class TestContext {
    public String jwtToken;
    public ResponseEntity<String> lastResponse;
}
