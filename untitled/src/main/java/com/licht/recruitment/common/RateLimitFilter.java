package com.licht.recruitment.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enterprise Rate Limiter and Abuse Protection Filter.
 * Mitigates credential stuffing, brute-force dictionary attacks, spam, and DoS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_LIMIT_PER_MINUTE = 15;
    private static final int ENQUIRY_LIMIT_PER_MINUTE = 20;
    private static final int GENERAL_LIMIT_PER_MINUTE = 120;

    private static class WindowBucket {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        WindowBucket(long start) {
            this.windowStart = start;
        }
    }

    private final Map<String, WindowBucket> requestBuckets = new ConcurrentHashMap<>();

    @Value("${app.security.trust-proxy:false}")
    private boolean trustProxy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Allow CORS pre-flight requests unconditionally
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        int maxLimit = GENERAL_LIMIT_PER_MINUTE;
        String category = "general";

        if (path.startsWith("/api/auth/") || path.equals("/api/employer/login") || path.equals("/api/employer/register")) {
            maxLimit = AUTH_LIMIT_PER_MINUTE;
            category = "auth";
        } else if (path.startsWith("/api/enquiries/")) {
            maxLimit = ENQUIRY_LIMIT_PER_MINUTE;
            category = "enquiry";
        }

        String bucketKey = clientIp + ":" + category;
        long now = System.currentTimeMillis();
        long currentMinuteWindow = now - (now % 60000);

        WindowBucket bucket = requestBuckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || existing.windowStart < currentMinuteWindow) {
                WindowBucket newBucket = new WindowBucket(currentMinuteWindow);
                newBucket.count.incrementAndGet();
                return newBucket;
            }
            existing.count.incrementAndGet();
            return existing;
        });

        // Cleanup stale keys periodically if map grows large
        if (requestBuckets.size() > 5000) {
            requestBuckets.entrySet().removeIf(entry -> entry.getValue().windowStart < currentMinuteWindow - 60000);
        }

        if (bucket.count.get() > maxLimit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Rate limit exceeded. Too many requests. Please wait 60 seconds.\",\"data\":null}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For is client-controlled unless a trusted proxy sets it. Behind one proxy,
        // the right-most entry is the address the proxy actually saw.
        if (trustProxy) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isEmpty()) {
                String[] hops = xf.split(",");
                return hops[hops.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
