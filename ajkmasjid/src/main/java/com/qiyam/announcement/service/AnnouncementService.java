package com.qiyam.announcement.service;

import com.qiyam.announcement.dto.AnnouncementRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class AnnouncementService {
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
        var scope = accessControlService.resolveMosqueScope(null, Permission.ANNOUNCEMENTS_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return List.of();
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        applyMosqueScope(params, scope);
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("announcements", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        accessControlService.requirePermission(null, Permission.ANNOUNCEMENTS_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("announcements", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> create(AnnouncementRequest request) {
        accessControlService.requirePermission(null, Permission.ANNOUNCEMENTS_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
        var body = toMap(request);
        var result = supabaseClient.post("announcements", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, AnnouncementRequest request) {
        accessControlService.requirePermission(null, Permission.ANNOUNCEMENTS_WRITE);
        verifyMosqueOwnership("announcements", String.valueOf(id));
        var body = toMap(request);
        var result = supabaseClient.patch("announcements", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
        accessControlService.requirePermission(null, Permission.ANNOUNCEMENTS_DELETE);
        verifyMosqueOwnership("announcements", String.valueOf(id));
        supabaseClient.delete("announcements", "id", String.valueOf(id));
    }

    public Map<String, Object> publish(Long id) {
        accessControlService.requirePermission(null, Permission.ANNOUNCEMENTS_WRITE);
        verifyMosqueOwnership("announcements", String.valueOf(id));
        return supabaseClient.patch("announcements", "id", String.valueOf(id), Map.of("status", "published"), Map.class);
    }

    public Map<String, Object> unpublish(Long id) {
        accessControlService.requirePermission(null, Permission.ANNOUNCEMENTS_WRITE);
        verifyMosqueOwnership("announcements", String.valueOf(id));
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
