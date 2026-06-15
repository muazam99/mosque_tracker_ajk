package com.qiyam.committee.service;

import com.qiyam.committee.dto.CommitteeRequest;
import com.qiyam.committee.dto.MemberRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class CommitteeService {
    private final SupabaseClient supabaseClient;

    // ─── Committees (committee_roles) ─────────────────────────

    public List<Map<String, Object>> getAllCommittees(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("committee_roles", params, Map.class);
    }

    public Optional<Map<String, Object>> getCommitteeById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("committee_roles", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createCommittee(CommitteeRequest request) {
        var body = committeeToMap(request);
        var result = supabaseClient.post("committee_roles", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateCommittee(Long id, CommitteeRequest request) {
        var body = committeeToMap(request);
        var result = supabaseClient.patch("committee_roles", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteCommittee(Long id) {
        supabaseClient.delete("committee_roles", "id", String.valueOf(id));
    }

    // ─── Members (mosque_committees) ─────────────────────────

    public List<Map<String, Object>> getCommitteeMembers(Long committeeId) {
        var params = new HashMap<String, String>();
        params.put("committee_role_id", "eq." + committeeId);
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosque_committees", params, Map.class);
    }

    public Map<String, Object> addCommitteeMember(Long committeeId, MemberRequest request) {
        request.setCommitteeRoleId(committeeId);
        var body = memberToMap(request);
        var result = supabaseClient.post("mosque_committees", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateCommitteeMember(Long committeeId, Long memberId, MemberRequest request) {
        request.setCommitteeRoleId(committeeId);
        var body = memberToMap(request);
        var result = supabaseClient.patch("mosque_committees", "id", String.valueOf(memberId), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void removeCommitteeMember(Long committeeId, Long memberId) {
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
