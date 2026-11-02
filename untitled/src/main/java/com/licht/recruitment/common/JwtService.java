package com.licht.recruitment.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * High-security JWT Service utilizing HMAC-SHA256 cryptographic signatures.
 * Prevents token tampering, identity spoofing, and privilege escalation without
 * relying on third-party dependencies.
 */
@Service
public class JwtService {

    private final String secretKey;
    private final long tokenValidityMs;

    public JwtService(
            @Value("${app.security.jwt.secret:}") String secretKey,
            @Value("${app.security.jwt.expiration-ms:86400000}") long tokenValidityMs) { // 24 hours
        // Fail fast: a missing or short secret would let anyone forge tokens
        if (secretKey == null || secretKey.trim().length() < 32) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set to at least 32 characters.");
        }
        this.secretKey = secretKey;
        this.tokenValidityMs = tokenValidityMs;
    }

    public static class TokenClaims {
        private final Long userId;
        private final String subject;
        private final String role;
        private final long expirationTime;

        public TokenClaims(Long userId, String subject, String role, long expirationTime) {
            this.userId = userId;
            this.subject = subject;
            this.role = role;
            this.expirationTime = expirationTime;
        }

        public Long getUserId() { return userId; }
        public String getSubject() { return subject; }
        public String getRole() { return role; }
        public long getExpirationTime() { return expirationTime; }
    }

    /**
     * Generates a signed token with tamper-proof HMAC-SHA256 signature.
     */
    public String generateToken(String subject, String role, Long userId) {
        return generateToken(subject, role, userId, tokenValidityMs);
    }

    /**
     * Generates a signed token with a custom lifetime (e.g. short-lived, single-purpose tokens).
     */
    public String generateToken(String subject, String role, Long userId, long validityMs) {
        long exp = System.currentTimeMillis() + validityMs;
        String payload = userId + ":" + (subject != null ? subject : "") + ":" + role + ":" + exp;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return "licht_v2_" + encodedPayload + "." + signature;
    }

    /**
     * Validates and parses the bearer token.
     * Returns TokenClaims if signature and expiry are valid; null otherwise.
     */
    public TokenClaims validateToken(String rawAuthHeader) {
        if (rawAuthHeader == null || rawAuthHeader.trim().isEmpty()) {
            return null;
        }

        String token = rawAuthHeader.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        if (!token.startsWith("licht_v2_")) {
            return null;
        }

        String tokenBody = token.substring("licht_v2_".length());
        int dotIndex = tokenBody.lastIndexOf('.');
        if (dotIndex <= 0) {
            return null;
        }

        String encodedPayload = tokenBody.substring(0, dotIndex);
        String providedSignature = tokenBody.substring(dotIndex + 1);

        String expectedSignature = sign(encodedPayload);
        // Constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = payload.split(":", 4);
            if (parts.length < 4) {
                return null;
            }

            Long userId = parts[0].isEmpty() ? null : Long.parseLong(parts[0]);
            String subject = parts[1];
            String role = parts[2];
            long exp = Long.parseLong(parts[3]);

            if (System.currentTimeMillis() > exp) {
                return null; // Expired
            }

            return new TokenClaims(userId, subject, role, exp);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC signature", e);
        }
    }
}
