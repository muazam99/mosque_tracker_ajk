package com.qiyam.shared.client;

import com.qiyam.shared.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for a double-URL-encoding bug: SupabaseClient previously handed an
 * already percent-encoded URL *String* to RestTemplate, which re-encodes String URLs as
 * a URI template — turning e.g. "%28" into "%2528" on the wire. PostgREST would then see
 * the literal text "%28" instead of "(" and reject the filter with a 400
 * ("failed to parse logic tree"). Passing a pre-built java.net.URI instead skips that
 * second encoding pass. This only ever surfaced once a filter value containing reserved
 * characters (parens/commas, as in an "or=(...)" filter) was introduced.
 */
@ExtendWith(MockitoExtension.class)
class SupabaseClientTest {

    @Mock
    private RestTemplate restTemplate;

    private SupabaseClient supabaseClient;

    @BeforeEach
    void setUp() {
        var supabase = new AppProperties.Supabase("https://example.supabase.co", "anon-key", "service-role-key");
        var appProperties = new AppProperties(null, null, null, supabase, null, null, null, null);
        supabaseClient = new SupabaseClient(restTemplate, appProperties);
    }

    @Test
    void getAll_withOrFilterContainingParensAndCommas_sendsRequestAsUriNotString() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        var params = new LinkedHashMap<String, String>();
        params.put("limit", "20");
        params.put("or", "(username.ilike.*af*,fullname.ilike.*af*,email.ilike.*af*)");

        supabaseClient.getAll("users", params, Map.class);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class));

        var uri = uriCaptor.getValue();
        // Decoded query must round-trip to exactly one structural "(" / "," / ")" — not
        // the double-encoded literal text "%28"/"%2C"/"%29" that PostgREST rejected.
        assertThat(uri.getQuery()).contains("or=(username.ilike.*af*,fullname.ilike.*af*,email.ilike.*af*)");
        // Raw (still-encoded) form must contain a single encoding pass: one "%28", not "%2528".
        assertThat(uri.getRawQuery()).contains("%28username.ilike").doesNotContain("%2528").doesNotContain("%25");
    }
}
