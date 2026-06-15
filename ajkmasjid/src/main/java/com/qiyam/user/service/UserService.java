package com.qiyam.user.service;

import com.qiyam.user.dto.UserRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class UserService {
    private final SupabaseClient supabaseClient;

    public List<Map<String, Object>> getAll(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("users", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(String id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("users", "id", id, Map.class);
    }

    public Map<String, Object> create(UserRequest request) {
        var body = toMap(request);
        var result = supabaseClient.post("users", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(String id, UserRequest request) {
        var body = toMap(request);
        var result = supabaseClient.patch("users", "id", id, body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(String id) {
        supabaseClient.delete("users", "id", id);
    }

    public Map<String, Object> changeRole(String id, String role) {
        return supabaseClient.patch("users", "id", id, Map.of("role", role), Map.class);
    }

    public Map<String, Object> resetPassword(String id) {
        return supabaseClient.patch("users", "id", id, Map.of("subscription_status", "reset-requested"), Map.class);
    }

    private Map<String, Object> toMap(UserRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "email", r.getEmail());
        putIfNotNull(map, "username", r.getUsername());
        putIfNotNull(map, "fullname", r.getFullname());
        putIfNotNull(map, "phone", r.getPhone());
        putIfNotNull(map, "role", r.getRole());
        putIfNotNull(map, "image_path", r.getImagePath());
        putIfNotNull(map, "status", r.getStatus());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
