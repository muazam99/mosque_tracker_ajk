package com.qiyam.mosque.service;

import com.qiyam.mosque.dto.MosqueRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class MosqueService {
    private final SupabaseClient supabaseClient;

    public List<Map<String, Object>> getAllMosques(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public Optional<Map<String, Object>> getMosqueById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("mosques", "id", String.valueOf(id), Map.class);
    }

    public List<Map<String, Object>> getMosquesByCountry(Long countryId, int limit) {
        var params = new HashMap<String, String>();
        params.put("country_id", "eq." + countryId);
        params.put("limit", String.valueOf(limit));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public List<Map<String, Object>> getMosquesByState(Long stateId, int limit) {
        var params = new HashMap<String, String>();
        params.put("state_id", "eq." + stateId);
        params.put("limit", String.valueOf(limit));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public List<Map<String, Object>> searchMosques(String query, int limit) {
        var params = new HashMap<String, String>();
        params.put("name", "ilike." + query + "*");
        params.put("limit", String.valueOf(limit));
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosques", params, Map.class);
    }

    public Map<String, Object> createMosque(MosqueRequest request) {
        var body = requestToMap(request);
        var result = supabaseClient.post("mosques", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateMosque(Long id, MosqueRequest request) {
        var body = requestToMap(request);
        var result = supabaseClient.patch("mosques", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteMosque(Long id) {
        supabaseClient.delete("mosques", "id", String.valueOf(id));
    }

    private Map<String, Object> requestToMap(MosqueRequest request) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "name", request.getName());
        putIfNotNull(map, "thumbnail_url", request.getThumbnailUrl());
        putIfNotNull(map, "image_urls", request.getImageUrls());
        putIfNotNull(map, "description", request.getDescription());
        putIfNotNull(map, "google_maps_embedded", request.getGoogleMapsEmbedded());
        putIfNotNull(map, "google_maps_url", request.getGoogleMapsUrl());
        putIfNotNull(map, "latitude", request.getLatitude());
        putIfNotNull(map, "longitude", request.getLongitude());
        putIfNotNull(map, "address", request.getAddress());
        putIfNotNull(map, "reviews_per_rating", request.getReviewsPerRating());
        putIfNotNull(map, "country_id", request.getCountryId());
        putIfNotNull(map, "state_id", request.getStateId());
        putIfNotNull(map, "district_id", request.getDistrictId());
        putIfNotNull(map, "website_url", request.getWebsiteUrl());
        putIfNotNull(map, "phone", request.getPhone());
        putIfNotNull(map, "email", request.getEmail());
        putIfNotNull(map, "category", request.getCategory());
        putIfNotNull(map, "status", request.getStatus());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
