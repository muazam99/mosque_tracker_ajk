package com.qiyam.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Verifies Google ID tokens directly using Google's tokeninfo endpoint.
 * No Firebase or Supabase needed — just a Google OAuth Web Client ID.
 */
@Slf4j
@Component
public class GoogleAuthClient {

    private final RestTemplate restTemplate;
    private final String googleWebClientId;

    public GoogleAuthClient(RestTemplate restTemplate, com.qiyam.shared.config.AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.googleWebClientId = appProperties.google().webClientId();
    }

    /**
     * Verifies a Google ID token and returns the user payload.
     * Calls Google's tokeninfo endpoint to validate the token,
     * then checks that the audience matches our client ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyIdToken(String idToken) {
        var url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                var body = response.getBody();
                var aud = (String) body.get("aud"); // audience should match our client ID
                var azp = (String) body.get("azp"); // authorized party

                // Verify the token is intended for our application
                if (googleWebClientId != null && !googleWebClientId.isBlank()) {
                    var matchesAud = googleWebClientId.equals(aud);
                    var matchesAzp = googleWebClientId.equals(azp);
                    if (!matchesAud && !matchesAzp) {
                        log.warn("Google token client ID mismatch: aud={}, azp={}, expected={}",
                                aud, azp, googleWebClientId);
                        throw new SecurityException("Invalid Google token: client ID mismatch");
                    }
                }

                log.info("Google token verified for email: {}", body.get("email"));
                return body;
            }
            throw new SecurityException("Google token verification failed");
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification error: {}", e.getMessage());
            throw new SecurityException("Google authentication failed");
        }
    }
}
