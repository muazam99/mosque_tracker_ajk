package com.qiyam.activity.service;

import com.qiyam.activity.dto.ActivityRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class ActivityService {
    private final SupabaseClient supabaseClient;

    public List<Map<String, Object>> getAll(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "starts_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosque_activities", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("mosque_activities", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> create(ActivityRequest request) {
        var body = toMap(request);
        var result = supabaseClient.post("mosque_activities", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, ActivityRequest request) {
        var body = toMap(request);
        var result = supabaseClient.patch("mosque_activities", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
        supabaseClient.delete("mosque_activities", "id", String.valueOf(id));
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        return supabaseClient.patch("mosque_activities", "id", String.valueOf(id), Map.of("activity_status", status), Map.class);
    }

    // ─── Registration ───────── use user_check_ins table ────

    public Map<String, Object> registerParticipant(Long activityId, Long userId) {
        var body = Map.of(
            "activity_id", activityId,
            "user_id", userId,
            "mosque_id", 0 // will be resolved or overridden
        );
        var result = supabaseClient.post("user_check_ins", body, Map.class);
        return result != null ? result : Map.of();
    }

    public void cancelRegistration(Long activityId, Long userId) {
        supabaseClient.delete("user_check_ins", "activity_id", String.valueOf(activityId));
    }

    private Map<String, Object> toMap(ActivityRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "title", r.getTitle());
        putIfNotNull(map, "description", r.getDescription());
        putIfNotNull(map, "status", r.getStatus());
        putIfNotNull(map, "activity_status", r.getActivityStatus());
        putIfNotNull(map, "starts_at", r.getStartsAt());
        putIfNotNull(map, "ends_at", r.getEndsAt());
        putIfNotNull(map, "is_recurring", r.getIsRecurring());
        putIfNotNull(map, "recurrence_type", r.getRecurrenceType());
        putIfNotNull(map, "recurrence_interval", r.getRecurrenceInterval());
        putIfNotNull(map, "recurrence_by_weekday", r.getRecurrenceByWeekday());
        putIfNotNull(map, "recurrence_until", r.getRecurrenceUntil());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
