package com.qiyam.meeting.service;

import com.qiyam.meeting.dto.MeetingRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class MeetingService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    public List<Map<String, Object>> getAll(int limit, int offset) {
        accessControlService.requirePermission(null, Permission.MEETINGS_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "scheduled_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("meetings", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        accessControlService.requirePermission(null, Permission.MEETINGS_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("meetings", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> create(MeetingRequest request) {
        accessControlService.requirePermission(null, Permission.MEETINGS_WRITE);
        var body = toMap(request);
        var result = supabaseClient.post("meetings", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, MeetingRequest request) {
        accessControlService.requirePermission(null, Permission.MEETINGS_WRITE);
        var body = toMap(request);
        var result = supabaseClient.patch("meetings", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
        accessControlService.requirePermission(null, Permission.MEETINGS_DELETE);
        supabaseClient.delete("meetings", "id", String.valueOf(id));
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        accessControlService.requirePermission(null, Permission.MEETINGS_WRITE);
        return supabaseClient.patch("meetings", "id", String.valueOf(id), Map.of("status", status), Map.class);
    }

    public Map<String, Object> updateMinutes(Long id, String minutes) {
        accessControlService.requirePermission(null, Permission.MEETINGS_WRITE);
        return supabaseClient.patch("meetings", "id", String.valueOf(id), Map.of("minutes", minutes), Map.class);
    }

    public Map<String, Object> updateAttendance(Long meetingId, Long attendeeId, String status) {
        accessControlService.requirePermission(null, Permission.MEETINGS_WRITE);
        return supabaseClient.patch(
            "meeting_attendees",
            "id", String.valueOf(attendeeId),
            Map.of("attendance_status", status),
            Map.class
        );
    }

    private Map<String, Object> toMap(MeetingRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "committee_id", r.getCommitteeId());
        putIfNotNull(map, "title", r.getTitle());
        putIfNotNull(map, "agenda", r.getAgenda());
        putIfNotNull(map, "location", r.getLocation());
        putIfNotNull(map, "scheduled_at", r.getScheduledAt());
        putIfNotNull(map, "status", r.getStatus());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
