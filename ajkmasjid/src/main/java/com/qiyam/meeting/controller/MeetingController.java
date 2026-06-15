package com.qiyam.meeting.controller;

import com.qiyam.meeting.dto.MeetingRequest;
import com.qiyam.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(meetingService.getAll(limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return meetingService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody MeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody MeetingRequest request) {
        return ResponseEntity.ok(meetingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        meetingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(meetingService.updateStatus(id, body.get("status")));
    }

    @PatchMapping("/{id}/minutes")
    public ResponseEntity<Map<String, Object>> updateMinutes(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(meetingService.updateMinutes(id, body.get("minutes")));
    }

    @PatchMapping("/{id}/attendees/{attendeeId}")
    public ResponseEntity<Map<String, Object>> updateAttendance(
            @PathVariable Long id, @PathVariable Long attendeeId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(meetingService.updateAttendance(id, attendeeId, body.get("status")));
    }
}
