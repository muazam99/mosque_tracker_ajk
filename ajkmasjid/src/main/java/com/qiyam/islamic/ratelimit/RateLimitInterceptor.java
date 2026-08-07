package com.qiyam.islamic.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qiyam.islamic.dto.PublicApiErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Per-client-IP rate limiting for the unauthenticated public Islamic API, so it can't be used
 * to hammer the upstream providers (Aladhan / alquran.cloud / hadith dataset) or this server.
 * In-memory token buckets (Caffeine-evicted after 10 minutes of inactivity) — fine for a single
 * instance; move to a shared store (e.g. Redis) if this app is ever scaled horizontally.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Cache<String, Bucket> buckets;
    private final int requestsPerMinute;

    public RateLimitInterceptor(@Value("${app.rate-limit.public-requests-per-minute:60}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(50_000)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var bucket = buckets.get(clientKey(request), key -> newBucket());
        if (bucket.tryConsume(1)) {
            return true;
        }
        log.warn("Rate limit exceeded for {} on {}", clientKey(request), request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                PublicApiErrorResponse.of("Rate limit exceeded. Please try again later.")));
        return false;
    }

    private Bucket newBucket() {
        var limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
