package com.qiyam.activity.service;

import com.qiyam.activity.dto.ActivityRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class ActivityService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    private void applyMosqueScope(Map<String, String> params, Set<Integer> scope) {
        if (scope == null) return; // unrestricted (SUPER_ADMIN)
        if (scope.size() == 1) {
            params.put("mosque_id", "eq." + scope.iterator().next());
        } else {
            params.put("mosque_id", "in.(" + scope.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")");
        }
    }

    /** Fetches a row by id and, if found, verifies the caller has access to its mosque_id. */
    private void verifyMosqueOwnership(String table, String id) {
        var row = supabaseClient.getOne(table, "id", id, Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, ((Map<?, ?>) r).get("mosque_id")));
    }

    public List<Map<String, Object>> getAll(int limit, int offset, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.ACTIVITIES_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return List.of();
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "starts_at.desc");
        applyMosqueScope(params, scope);
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosque_activities", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("mosque_activities", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> create(ActivityRequest request) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
        var body = toMap(request);
        var result = supabaseClient.post("mosque_activities", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, ActivityRequest request) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_WRITE);
        verifyMosqueOwnership("mosque_activities", String.valueOf(id));
        var body = toMap(request);
        var result = supabaseClient.patch("mosque_activities", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_DELETE);
        verifyMosqueOwnership("mosque_activities", String.valueOf(id));
        supabaseClient.delete("mosque_activities", "id", String.valueOf(id));
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_WRITE);
        verifyMosqueOwnership("mosque_activities", String.valueOf(id));
        return supabaseClient.patch("mosque_activities", "id", String.valueOf(id), Map.of("activity_status", status), Map.class);
    }

    // ─── Registration ───────── use user_check_ins table ────

    public Map<String, Object> registerParticipant(Long activityId, Long userId) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_WRITE);
        verifyMosqueOwnership("mosque_activities", String.valueOf(activityId));
        var body = Map.of(
            "activity_id", activityId,
            "user_id", userId,
            "mosque_id", 0
        );
        var result = supabaseClient.post("user_check_ins", body, Map.class);
        return result != null ? result : Map.of();
    }

    public void cancelRegistration(Long activityId, Long userId) {
        accessControlService.requirePermission(null, Permission.ACTIVITIES_DELETE);
        verifyMosqueOwnership("mosque_activities", String.valueOf(activityId));
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
