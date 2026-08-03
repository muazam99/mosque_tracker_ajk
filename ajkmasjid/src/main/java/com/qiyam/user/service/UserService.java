package com.qiyam.user.service;

import com.qiyam.user.dto.UserRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.dto.PagedResponse;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class UserService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    /**
     * @param search   optional case-insensitive substring match against username, fullname, or email
     * @param mosqueId optional — restricts to users with a mosque_committees membership at this mosque.
     *                 When omitted, results are NOT mosque-scoped (global search across all users the
     *                 caller is permitted to see).
     */
    public PagedResponse<Map<String, Object>> getAll(int limit, int offset, int page, String search, Long mosqueId) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_READ);

        List<String> mosqueUserIds = null;
        if (mosqueId != null) {
            var memberParams = new HashMap<String, String>();
            memberParams.put("mosque_id", "eq." + mosqueId);
            var memberships = supabaseClient.getAll("mosque_committees", memberParams, Map.class);
            mosqueUserIds = memberships.stream()
                    .map(m -> String.valueOf(m.get("user_id")))
                    .distinct()
                    .toList();
            if (mosqueUserIds.isEmpty()) {
                return PagedResponse.empty(page, limit);
            }
        }

        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        if (search != null && !search.isBlank()) {
            var escaped = escapeFilterValue(search.trim());
            params.put("or", "(username.ilike.*" + escaped + "*,fullname.ilike.*" + escaped + "*,email.ilike.*" + escaped + "*)");
        }
        if (mosqueUserIds != null) {
            params.put("id", "in.(" + String.join(",", mosqueUserIds) + ")");
        }
        var result = supabaseClient.getAllPaged("users", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    /** Escapes characters that are structurally significant in a PostgREST filter value (comma, parens, backslash). */
    private String escapeFilterValue(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    public Optional<Map<String, Object>> getById(String id) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("users", "id", id, Map.class);
    }

    public Map<String, Object> create(UserRequest request) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_WRITE);
        var body = toMap(request);
        var result = supabaseClient.post("users", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(String id, UserRequest request) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_WRITE);
        var body = toMap(request);
        var result = supabaseClient.patch("users", "id", id, body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(String id) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_DELETE);
        supabaseClient.delete("users", "id", id);
    }

    public Map<String, Object> changeRole(String id, String role) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_WRITE);
        // Enforce hierarchical role management
        var targetRole = Role.fromString(role);
        accessControlService.requireCanManageUser(null, targetRole);
        return supabaseClient.patch("users", "id", id, Map.of("role", role), Map.class);
    }

    public Map<String, Object> resetPassword(String id) {
        accessControlService.requirePermission(null, com.qiyam.shared.security.Permission.USERS_WRITE);
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
