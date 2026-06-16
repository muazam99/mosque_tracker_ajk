package com.qiyam.shared.client;

import com.qiyam.shared.config.AppProperties;
import com.qiyam.shared.exception.SupabaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupabaseClient {
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    private String baseUrl() {
        return appProperties.supabase().url() + "/rest/v1";
    }

    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.set("apikey", appProperties.supabase().serviceRoleKey());
        headers.set("Authorization", "Bearer " + appProperties.supabase().serviceRoleKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ─── Generic CRUD ────────────────────────────────────────────────

    public <T> List<T> getAll(String table, Map<String, String> params, Class<T> clazz) {
        try {
            var url = buildUrl(table, params);
            var entity = new HttpEntity<>(headers());
            log.debug("GET {}", url);
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<T>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase GET error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to fetch " + table + ": " + e.getMessage());
        }
    }

    public <T> Optional<T> getOne(String table, String column, String value, Class<T> clazz) {
        try {
            var params = Map.of(column, "eq." + value);
            var results = getAll(table, params, clazz);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SupabaseException e) {
            throw e;
        }
    }

    public <T> T post(String table, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + table;
            var entity = new HttpEntity<>(body, headers());
            log.debug("POST {}", url);
            return restTemplate.postForObject(url, entity, clazz);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase POST error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to create " + table + ": " + e.getMessage());
        }
    }

    public <T> T patch(String table, String column, String value, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + table + "?" + column + "=eq." + value;
            var entity = new HttpEntity<>(body, headers());
            log.debug("PATCH {}", url);
            var response = restTemplate.exchange(url, HttpMethod.PATCH, entity, clazz);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase PATCH error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to update " + table + ": " + e.getMessage());
        }
    }

    public void delete(String table, String column, String value) {
        try {
            var url = baseUrl() + "/" + table + "?" + column + "=eq." + value;
            var entity = new HttpEntity<>(headers());
            log.debug("DELETE {}", url);
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase DELETE error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to delete from " + table + ": " + e.getMessage());
        }
    }

    // ─── Auth ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> authenticate(String email, String password) {
        var url = appProperties.supabase().url() + "/auth/v1/token?grant_type=password";
        var headers = new HttpHeaders();
        headers.set("apikey", appProperties.supabase().anonKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        var body = Map.of("email", email, "password", password);
        var entity = new HttpEntity<>(body, headers);

        try {
            var response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                var tokenBody = response.getBody();
                return (Map<String, Object>) tokenBody.get("user");
            }
            throw new RuntimeException("Supabase auth failed: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Supabase authentication failed", e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private String buildUrl(String table, Map<String, String> params) {
        var sb = new StringBuilder(baseUrl() + "/" + table);
        if (params != null && !params.isEmpty()) {
            sb.append("?");
            params.forEach((k, v) -> {
                if (sb.charAt(sb.length() - 1) != '?') sb.append("&");
                sb.append(k).append("=").append(v);
            });
        }
        return sb.toString();
    }
}
