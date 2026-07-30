package com.qiyam.committee.service;

import com.qiyam.committee.dto.CommitteeRequest;
import com.qiyam.committee.dto.MemberRequest;
import com.qiyam.shared.client.SupabaseClient;
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

    private void applyMosqueFilter(Map<String, String> params, Integer mosqueId) {
        if (mosqueId != null) params.put("mosque_id", "eq." + mosqueId);
    }

    public List<Map<String, Object>> getAllCommittees(int limit, int offset, Integer mosqueId) {
        accessControlService.requirePermission(null, Permission.MEMBERS_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        applyMosqueFilter(params, mosqueId);
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("committee_roles", params, Map.class);
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
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosque_committees", params, Map.class);
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

    // ─── Mappers ─────────────────────────────────────────────

    private Map<String, Object> committeeToMap(CommitteeRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
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
