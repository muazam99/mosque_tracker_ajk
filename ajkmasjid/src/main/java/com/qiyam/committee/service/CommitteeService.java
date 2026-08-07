package com.qiyam.committee.service;

import com.qiyam.committee.dto.CommitteeRequest;
import com.qiyam.committee.dto.MemberRequest;
import com.qiyam.committee.dto.RoleMemberRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.dto.PagedResponse;
import com.qiyam.shared.exception.ConflictException;
import com.qiyam.shared.exception.ResourceNotFoundException;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class CommitteeService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    // ─── Committees (committee_roles) ─────────────────────────
    // NOTE: committee_roles is a global, organization-wide role catalog — it is
    // intentionally NOT mosque-scoped. Mosque scoping lives on committee_role_member.

    public PagedResponse<Map<String, Object>> getAllCommittees(int limit, int offset, int page) {
        accessControlService.requirePermission(null, Permission.MEMBERS_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        var result = supabaseClient.getAllPaged("committee_roles", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getCommitteeById(String id) {
        accessControlService.requirePermission(null, Permission.MEMBERS_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("committee_roles", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createCommittee(CommitteeRequest request) {
        accessControlService.requirePermission(null, Permission.MEMBERS_WRITE);
        var body = committeeToMap(request);
        var result = supabaseClient.post("committee_roles", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateCommittee(String id, CommitteeRequest request) {
        accessControlService.requirePermission(null, Permission.MEMBERS_WRITE);
        var body = committeeToMap(request);
        var result = supabaseClient.patch("committee_roles", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteCommittee(String id) {
        accessControlService.requirePermission(null, Permission.MEMBERS_DELETE);
        supabaseClient.delete("committee_roles", "id", String.valueOf(id));
    }

    // ─── Members (mosque_committees) ─────────────────────────

    public List<Map<String, Object>> getCommitteeMembers(String committeeId) {
        accessControlService.requirePermission(null, Permission.MEMBERS_READ);
        var params = new HashMap<String, String>();
        params.put("committee_role_id", "eq." + committeeId);
        params.put("order", "created_at.desc");
        var rows = (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosque_committees", params, Map.class);

        var allowedMosqueIds = accessControlService.getMosqueIds(null);
        if (allowedMosqueIds == null) return rows; // SUPER_ADMIN — unrestricted
        return rows.stream()
                .filter(row -> allowedMosqueIds.contains(toMosqueId(row.get("mosque_id"))))
                .toList();
    }

    public Map<String, Object> addCommitteeMember(String committeeId, MemberRequest request) {
        accessControlService.requirePermission(null, Permission.MEMBERS_WRITE);
        // Look up the committee to get its mosque_id
        var committee = getCommitteeById(committeeId)
                .orElseThrow(() -> new RuntimeException("Committee not found: " + committeeId));
        var body = memberToMap(request);
        body.put("committee_role_id", committeeId);
        body.put("mosque_id", committee.get("mosque_id"));
        var result = supabaseClient.post("mosque_committees", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateCommitteeMember(String committeeId, String memberId, MemberRequest request) {
        accessControlService.requirePermission(null, Permission.MEMBERS_WRITE);
        var body = memberToMap(request);
        body.put("committee_role_id", committeeId);
        var result = supabaseClient.patch("mosque_committees", "id", String.valueOf(memberId), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void removeCommitteeMember(String committeeId, String memberId) {
        accessControlService.requirePermission(null, Permission.MEMBERS_DELETE);
        supabaseClient.delete("mosque_committees", "id", String.valueOf(memberId));
    }

    // ─── Role Members (committee_role_member) ─────────────────
    // Minimal user x committee_role x mosque assignment table — distinct from
    // the richer mosque_committees ("members") table above, which additionally
    // tracks appointment scheduling/status. This is a separate concept: a plain
    // "who holds this global role at which mosque" assignment.

    private Map<String, Object> requireCommitteeRole(String committeeId) {
        var found = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("committee_roles", "id", committeeId, Map.class);
        return found.orElseThrow(() -> new ResourceNotFoundException("Committee role", committeeId));
    }

    public List<Map<String, Object>> getRoleMembers(String committeeId) {
        accessControlService.requirePermission(null, Permission.MEMBERS_READ);
        requireCommitteeRole(committeeId);

        var params = new HashMap<String, String>();
        params.put("committee_role_id", "eq." + committeeId);
        params.put("order", "created_at.desc");
        var rows = (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("committee_role_member", params, Map.class);

        var allowedMosqueIds = accessControlService.getMosqueIds(null);
        if (allowedMosqueIds == null) return rows; // SUPER_ADMIN — unrestricted
        return rows.stream()
                .filter(row -> allowedMosqueIds.contains(toMosqueId(row.get("mosque_id"))))
                .toList();
    }

    public Map<String, Object> addRoleMember(String committeeId, RoleMemberRequest request) {
        accessControlService.requirePermission(null, Permission.MEMBERS_WRITE);
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getMosqueId() == null) {
            throw new IllegalArgumentException("mosqueId is required");
        }
        accessControlService.requirePermissionAndMosque(null, Permission.MEMBERS_WRITE, request.getMosqueId().intValue());

        requireCommitteeRole(committeeId);

        var user = supabaseClient.getOne("users", "id", request.getUserId(), Map.class);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User", request.getUserId());
        }
        var mosque = supabaseClient.getOne("mosques", "id", String.valueOf(request.getMosqueId()), Map.class);
        if (mosque.isEmpty()) {
            throw new ResourceNotFoundException("Mosque", request.getMosqueId());
        }

        var dupParams = new HashMap<String, String>();
        dupParams.put("committee_role_id", "eq." + committeeId);
        dupParams.put("user_id", "eq." + request.getUserId());
        dupParams.put("mosque_id", "eq." + request.getMosqueId());
        var existing = supabaseClient.getAll("committee_role_member", dupParams, Map.class);
        if (existing != null && !existing.isEmpty()) {
            throw new ConflictException("User " + request.getUserId()
                    + " is already assigned to committee role " + committeeId + " at mosque " + request.getMosqueId());
        }

        var body = new HashMap<String, Object>();
        body.put("committee_role_id", committeeId);
        body.put("user_id", request.getUserId());
        body.put("mosque_id", request.getMosqueId());
        var created = (Map<String, Object>) supabaseClient.postReturning("committee_role_member", body, Map.class);
        return created != null ? created : body;
    }

    public void removeRoleMember(String committeeId, String memberId) {
        accessControlService.requirePermission(null, Permission.MEMBERS_DELETE);
        requireCommitteeRole(committeeId);

        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("committee_role_member", "id", memberId, Map.class);
        var member = row.orElseThrow(() -> new ResourceNotFoundException("Committee role member", memberId));
        if (!committeeId.equals(String.valueOf(member.get("committee_role_id")))) {
            throw new ResourceNotFoundException("Committee role member", memberId);
        }

        var mosqueId = toMosqueId(member.get("mosque_id"));
        accessControlService.requirePermissionAndMosque(null, Permission.MEMBERS_DELETE, mosqueId);

        supabaseClient.delete("committee_role_member", "id", memberId);
    }

    private Integer toMosqueId(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    // ─── Mappers ─────────────────────────────────────────────

    private Map<String, Object> committeeToMap(CommitteeRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "role_name", r.getRoleName());
        putIfNotNull(map, "description", r.getDescription());
        putIfNotNull(map, "hierarchy_level", r.getHierarchyLevel());
        putIfNotNull(map, "is_system_role", r.getIsSystemRole());
        return map;
    }

    private Map<String, Object> memberToMap(MemberRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "user_id", r.getUserId());
        putIfNotNull(map, "committee_role_id", r.getCommitteeRoleId());
        putIfNotNull(map, "appointment_start", r.getAppointmentStart());
        putIfNotNull(map, "appointment_end", r.getAppointmentEnd());
        putIfNotNull(map, "status", r.getStatus());
        putIfNotNull(map, "appointed_by", r.getAppointedBy());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
