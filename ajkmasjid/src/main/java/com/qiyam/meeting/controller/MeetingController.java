package com.qiyam.meeting.controller;

import com.qiyam.meeting.dto.MeetingRequest;
import com.qiyam.meeting.service.MeetingService;
import com.qiyam.shared.dto.PagedResponse;
import com.qiyam.shared.util.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Manage meetings including CRUD, status updates, minutes, and attendance tracking")
public class MeetingController {
    private final MeetingService meetingService;

    @GetMapping
    @Operation(summary = "Get all meetings", description = "Returns a paginated list of all meetings")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "1-indexed page number; takes precedence over 'offset' when given")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Alias for 'limit'")
            @RequestParam(name = "per_page", required = false) Integer perPage,
            @Parameter(description = "Filter by mosque ID")
            @RequestParam(required = false) Integer mosqueId) {
        var effectiveLimit = Pagination.resolveLimit(perPage, limit);
        var effectiveOffset = Pagination.resolveOffset(page, effectiveLimit, offset);
        var resolvedPage = Pagination.resolvePage(page, effectiveOffset, effectiveLimit);
        return ResponseEntity.ok(meetingService.getAll(effectiveLimit, effectiveOffset, resolvedPage, mosqueId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meeting by ID", description = "Returns a single meeting by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Meeting ID") @PathVariable Long id) {
        return meetingService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a meeting", description = "Creates a new meeting")
    public ResponseEntity<Map<String, Object>> create(@RequestBody MeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a meeting", description = "Updates an existing meeting by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Meeting ID") @PathVariable Long id, @RequestBody MeetingRequest request) {
        return ResponseEntity.ok(meetingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a meeting", description = "Deletes a meeting by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Meeting ID") @PathVariable Long id) {
        meetingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update meeting status", description = "Updates the status of a meeting")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @Parameter(description = "Meeting ID") @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(meetingService.updateStatus(id, body.get("status")));
    }

    @PatchMapping("/{id}/minutes")
    @Operation(summary = "Update meeting minutes", description = "Updates the minutes/notes of a meeting")
    public ResponseEntity<Map<String, Object>> updateMinutes(
            @Parameter(description = "Meeting ID") @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(meetingService.updateMinutes(id, body.get("minutes")));
    }

    @PatchMapping("/{id}/attendees/{attendeeId}")
    @Operation(summary = "Update attendance", description = "Updates the attendance status of a meeting attendee")
    public ResponseEntity<Map<String, Object>> updateAttendance(
            @Parameter(description = "Meeting ID") @PathVariable Long id,
            @Parameter(description = "Attendee ID") @PathVariable Long attendeeId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(meetingService.updateAttendance(id, attendeeId, body.get("status")));
    }
}
