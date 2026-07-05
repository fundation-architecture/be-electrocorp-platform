package com.electrocorp.electrocorpplatform.acceptance;

import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;

@Component
public class TestContext {
    public String jwtToken;
    public String lastAuthEmail;
    public HttpResponse<String> lastResponse;
}
