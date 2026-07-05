package com.electrocorp.electrocorpplatform.iam.infrastructure.security;

import com.electrocorp.electrocorpplatform.iam.application.security.TokenService;
import com.electrocorp.electrocorpplatform.iam.domain.model.aggregates.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtTokenService implements TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"(\\d+)\"");
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final String secret;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${electrocorp.security.jwt.secret:electrocorp-development-jwt-secret-change-me}") String secret,
            @Value("${electrocorp.security.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String generateToken(User user) {
        try {
            String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

            long issuedAt = Instant.now().getEpochSecond();
            long expiresAt = Instant.now().plusSeconds(expirationSeconds).getEpochSecond();
            String payload = "{"
                    + "\"sub\":\"" + user.getId() + "\","
                    + "\"email\":\"" + escapeJson(user.getEmail()) + "\","
                    + "\"iat\":" + issuedAt + ","
                    + "\"exp\":" + expiresAt
                    + "}";
            String body = encode(payload);
            String unsignedToken = header + "." + body;
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate authentication token.", exception);
        }
    }

    @Override
    public Optional<Long> validateAndGetUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return Optional.empty();
            }

            String payload = new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            Matcher subjectMatcher = SUBJECT_PATTERN.matcher(payload);
            Matcher expirationMatcher = EXPIRATION_PATTERN.matcher(payload);

            if (!subjectMatcher.find() || !expirationMatcher.find()) {
                return Optional.empty();
            }

            long expiration = Long.parseLong(expirationMatcher.group(1));
            if (expiration < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            return Optional.of(Long.parseLong(subjectMatcher.group(1)));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String encode(String value) {
        return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestHelper.constantTimeEquals(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
