package com.qiyam.mosque.service;

import com.qiyam.mosque.dto.MosqueRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class MosqueService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    public List<Map<String, Object>> getAllMosques(int limit, int offset) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "name.asc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public Optional<Map<String, Object>> getMosqueById(Long id) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("mosques", "id", String.valueOf(id), Map.class);
    }

    public List<Map<String, Object>> getMosquesByCountry(Long countryId, int limit) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_READ);
        var params = new HashMap<String, String>();
        params.put("country_id", "eq." + countryId);
        params.put("limit", String.valueOf(limit));
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public List<Map<String, Object>> getMosquesByState(Long stateId, int limit) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_READ);
        var params = new HashMap<String, String>();
        params.put("state_id", "eq." + stateId);
        params.put("limit", String.valueOf(limit));
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public List<Map<String, Object>> searchMosques(String query, int limit) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_READ);
        var params = new HashMap<String, String>();
        params.put("name", "ilike.*" + query + "*");
        params.put("limit", String.valueOf(limit));
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public Map<String, Object> createMosque(MosqueRequest request) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_WRITE);
        var body = toMap(request);
        var result = supabaseClient.post("mosques", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateMosque(Long id, MosqueRequest request) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_WRITE);
        var body = toMap(request);
        var result = supabaseClient.patch("mosques", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteMosque(Long id) {
        accessControlService.requirePermission(null, Permission.MOSQUE_SETTINGS_WRITE);
        supabaseClient.delete("mosques", "id", String.valueOf(id));
    }

    private Map<String, Object> toMap(MosqueRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "name", r.getName());
        putIfNotNull(map, "address", r.getAddress());
        putIfNotNull(map, "phone", r.getPhone());
        putIfNotNull(map, "email", r.getEmail());
        putIfNotNull(map, "description", r.getDescription());
        putIfNotNull(map, "thumbnail_url", r.getThumbnailUrl());
        putIfNotNull(map, "image_urls", r.getImageUrls());
        putIfNotNull(map, "google_maps_embedded", r.getGoogleMapsEmbedded());
        putIfNotNull(map, "google_maps_url", r.getGoogleMapsUrl());
        putIfNotNull(map, "latitude", r.getLatitude());
        putIfNotNull(map, "longitude", r.getLongitude());
        putIfNotNull(map, "country_id", r.getCountryId());
        putIfNotNull(map, "state_id", r.getStateId());
        putIfNotNull(map, "district_id", r.getDistrictId());
        putIfNotNull(map, "website_url", r.getWebsiteUrl());
        putIfNotNull(map, "category", r.getCategory());
        putIfNotNull(map, "status", r.getStatus());
        putIfNotNull(map, "reviews_per_rating", r.getReviewsPerRating());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
