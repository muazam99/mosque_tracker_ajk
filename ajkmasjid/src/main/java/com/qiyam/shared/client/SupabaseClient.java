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

import java.net.URI;
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
        return headers;
    }

    private HttpHeaders headersWithRepresentation() {
        var headers = headers();
        headers.set("Prefer", "return=representation");
        return headers;
    }

    // ─── Generic CRUD ────────────────────────────────────────────────

    public <T> List<T> getAll(String table, Map<String, String> params, Class<T> clazz) {
        var url = buildUrl(table, params);
        try {
            var entity = new HttpEntity<>(headers());
            log.debug("GET {}", url);
            var response = restTemplate.exchange(
                    toUri(url), HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<T>>() {});
            var bodySize = response.getBody() != null ? response.getBody().size() : 0;
            log.info("GET {} -> HTTP {} returned {} rows", url, response.getStatusCode().value(), bodySize);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GET {} -> HTTP 404 (NotFound)", url);
            return List.of();
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase GET error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to fetch " + table);
        }
    }

    /** A page of rows plus the total number of rows matching the filter (ignoring limit/offset). */
    public record PageResult<T>(List<T> data, long total) {}

    /**
     * Like {@link #getAll}, but also asks PostgREST for an exact total count of rows matching
     * the filter (via {@code Prefer: count=exact}), read back off the {@code Content-Range}
     * response header. Use this for any endpoint that needs to report a total/page count to
     * the caller — {@link #getAll} alone only ever tells you how many rows came back in this page.
     */
    public <T> PageResult<T> getAllPaged(String table, Map<String, String> params, Class<T> clazz) {
        var url = buildUrl(table, params);
        try {
            var headers = headers();
            headers.set("Prefer", "count=exact");
            var entity = new HttpEntity<>(headers);
            log.debug("GET (paged) {}", url);
            var response = restTemplate.exchange(
                    toUri(url), HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<T>>() {});
            var rows = response.getBody() != null ? response.getBody() : List.<T>of();
            var total = parseTotalCount(response.getHeaders().getFirst("Content-Range"), rows.size());
            log.info("GET {} -> HTTP {} returned {} of {} total rows", url, response.getStatusCode().value(), rows.size(), total);
            return new PageResult<>(rows, total);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GET {} -> HTTP 404 (NotFound)", url);
            return new PageResult<>(List.of(), 0);
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase GET error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to fetch " + table);
        }
    }

    /** Parses PostgREST's {@code Content-Range: <start>-<end>/<total>} header (total may be "*" if unknown). */
    private long parseTotalCount(String contentRange, int fallback) {
        if (contentRange == null) return fallback;
        var slash = contentRange.lastIndexOf('/');
        if (slash < 0 || slash == contentRange.length() - 1) return fallback;
        var totalStr = contentRange.substring(slash + 1);
        if ("*".equals(totalStr)) return fallback;
        try {
            return Long.parseLong(totalStr);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOne(String table, String column, String value, Class<T> clazz) {
        try {
            // Pre-encoded query string handed to toUri() as-is — see toUri() javadoc for why
            // RestTemplate must not re-encode it.
            var url = baseUrl() + "/" + encodeTable(table) + "?"
                    + encodeColumn(column) + "=eq." + encodeValue(value) + "&limit=1";
            var entity = new HttpEntity<>(headers());
            var response = restTemplate.exchange(
                    toUri(url), HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            var rows = response.getBody();
            if (rows == null || rows.isEmpty()) {
                log.debug("getOne: no row found for {}={} in {}", column, value, table);
                return Optional.empty();
            }
            return Optional.of((T) rows.get(0));
        } catch (SupabaseException e) {
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed in getOne: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase getOne error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to fetch from " + table);
        }
    }

    public <T> T post(String table, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + encodeTable(table);
            // Use minimal return to avoid deserialization errors with array responses
            var h = headers();
            h.set("Prefer", "return=minimal");
            var entity = new HttpEntity<>(body, h);
            log.debug("POST {}", url);
            restTemplate.postForObject(toUri(url), entity, clazz);
            // Record created successfully — return empty map since we used return=minimal
            return clazz == Map.class ? (T) Map.of() : null;
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase POST error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to create record: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Like {@link #post}, but returns the inserted row instead of an empty body
     * (uses "Prefer: return=representation" — PostgREST responds with a JSON array).
     */
    @SuppressWarnings("unchecked")
    public <T> T postReturning(String table, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + encodeTable(table);
            var entity = new HttpEntity<>(body, headersWithRepresentation());
            log.debug("POST (returning) {}", url);
            var response = restTemplate.exchange(
                    toUri(url), HttpMethod.POST, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            var rows = response.getBody();
            return rows != null && !rows.isEmpty() ? (T) rows.get(0) : null;
        } catch (ResourceAccessException e) {
            log.error("Supabase connection failed: {}", e.getMessage());
            throw new SupabaseException(503, "Cannot reach Supabase at " + baseUrl() + " – " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Supabase POST error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SupabaseException(e.getStatusCode().value(), "Failed to create record: " + e.getResponseBodyAsString());
        }
    }

    public <T> T patch(String table, String column, String value, Object body, Class<T> clazz) {
        try {
            var url = baseUrl() + "/" + encodeTable(table) + "?" + encodeColumn(column) + "=eq." + encodeValue(value);
            var entity = new HttpEntity<>(body, headers());
            log.debug("PATCH {}", url);
            var response = restTemplate.exchange(toUri(url), HttpMethod.PATCH, entity, clazz);
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
            restTemplate.exchange(toUri(url), HttpMethod.DELETE, entity, Void.class);
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

    /**
     * Converts an already percent-encoded URL string into a {@link URI} before handing it
     * to RestTemplate. RestTemplate's {@code exchange(String, ...)}/{@code postForObject(String, ...)}
     * overloads treat the string as a URI *template* and re-encode it — since {@link #buildUrl}
     * and friends already percent-encode column/value content via {@link #encodeValue}, that
     * second pass double-encodes any reserved character (e.g. "(" becomes "%2528" on the wire
     * instead of "%28"), which PostgREST then fails to parse. Passing a pre-built {@link URI}
     * instead skips that re-encoding entirely.
     */
    private URI toUri(String url) {
        return URI.create(url);
    }

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
