package com.qiyam.announcement.service;

import com.qiyam.announcement.dto.AnnouncementRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final SupabaseClient supabaseClient;

    public List<Map<String, Object>> getAll(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("announcements", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("announcements", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> create(AnnouncementRequest request) {
        var body = toMap(request);
        var result = supabaseClient.post("announcements", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, AnnouncementRequest request) {
        var body = toMap(request);
        var result = supabaseClient.patch("announcements", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
        supabaseClient.delete("announcements", "id", String.valueOf(id));
    }

    public Map<String, Object> publish(Long id) {
        return supabaseClient.patch("announcements", "id", String.valueOf(id), Map.of("status", "published"), Map.class);
    }

    public Map<String, Object> unpublish(Long id) {
        return supabaseClient.patch("announcements", "id", String.valueOf(id), Map.of("status", "draft"), Map.class);
    }

    private Map<String, Object> toMap(AnnouncementRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "title", r.getTitle());
        putIfNotNull(map, "content", r.getContent());
        putIfNotNull(map, "status", r.getStatus());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
