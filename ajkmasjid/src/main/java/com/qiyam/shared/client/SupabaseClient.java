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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private String authUrl() {
        return appProperties.supabase().url() + "/auth/v1";
    }

    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.set("apikey", appProperties.supabase().serviceRoleKey());
        headers.set("Authorization", "Bearer " + appProperties.supabase().serviceRoleKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ─── Generic CRUD ────────────────────────────────────────────────

    public <T> List<T> getAll(String table, Map<String, String> params, Class<T> clazz) {
        var url = buildUrl(table, params);
        try {
            var entity = new HttpEntity<>(headers());
            log.debug("GET {}", url);
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<T>>() {});
            var bodySize = response.getBody() != null ? response.getBody().size() : 0;
            log.info("GET {} -> HTTP {} returned {} rows", url, response.getStatusCode().value(), bodySize);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GET {} -> HTTP 404 (NotFound) body={}", url, e.getResponseBodyAsString());
            return List.of();
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase GET error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to fetch " + table);
        }
    }

    public <T> Optional<T> getOne(String table, String column, String value, Class<T> clazz) {
        try {
            // Build URL directly to avoid encoding the "eq." operator prefix
            var url = baseUrl() + "/" + encodeTable(table) + "?"
                    + encodeColumn(column) + "=eq." + encodeValue(value);
            log.info("getOne URL: {}", url);
            var entity = new HttpEntity<>(headers());
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<T>>() {});
            var results = response.getBody() != null ? response.getBody() : List.of();
            log.info("getOne: returned {} results", results.size());
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (SupabaseException e) {
            throw e;
        }
    }

    public <T> T post(String table, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + encodeTable(table);
            var entity = new HttpEntity<>(body, headers());
            log.debug("POST {}", url);
            return restTemplate.postForObject(url, entity, clazz);
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase POST error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to create record");
        }
    }

    public <T> T patch(String table, String column, String value, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + encodeTable(table) + "?" + encodeColumn(column) + "=eq." + encodeValue(value);
            var entity = new HttpEntity<>(body, headers());
            log.debug("PATCH {}", url);
            var response = restTemplate.exchange(url, HttpMethod.PATCH, entity, clazz);
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase PATCH error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to update record");
        }
    }

    public void delete(String table, String column, String value) {
        try {
            var url = baseUrl() + "/" + encodeTable(table) + "?" + encodeColumn(column) + "=eq." + encodeValue(value);
            var entity = new HttpEntity<>(headers());
            log.debug("DELETE {}", url);
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase DELETE error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to delete record");
        }
    }

    // ─── Auth - Google OAuth (verify access_token from frontend) ─────

    @SuppressWarnings("unchecked")
    public Map<String, Object> authenticateWithGoogle(String accessToken) {
        var url = authUrl() + "/user";
        var headers = new HttpHeaders();
        headers.set("apikey", appProperties.supabase().anonKey());
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var entity = new HttpEntity<>(headers);

        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Google authentication failed");
        } catch (Exception e) {
            log.error("Supabase Google auth failed: {}", e.getMessage());
            throw new RuntimeException("Google authentication failed");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private String buildUrl(String table, Map<String, String> params) {
        var sb = new StringBuilder(baseUrl() + "/" + encodeTable(table));
        if (params != null && !params.isEmpty()) {
            sb.append("?");
            params.forEach((k, v) -> {
                if (sb.charAt(sb.length() - 1) != '?') sb.append("&");
                sb.append(encodeColumn(k)).append("=").append(encodeValue(v));
            });
        }
        return sb.toString();
    }

    private String encodeTable(String table) {
        return URLEncoder.encode(table, StandardCharsets.UTF_8);
    }

    private String encodeColumn(String column) {
        return URLEncoder.encode(column, StandardCharsets.UTF_8);
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
