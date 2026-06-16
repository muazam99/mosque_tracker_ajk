package com.qiyam.activity.controller;

import com.qiyam.activity.dto.ActivityRequest;
import com.qiyam.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Manage activities including CRUD, status updates, and participant registration")
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Get all activities", description = "Returns a paginated list of all activities")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(activityService.getAll(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Returns a single activity by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Activity ID") @PathVariable Long id) {
        return activityService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create an activity", description = "Creates a new activity")
    public ResponseEntity<Map<String, Object>> create(@RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an activity", description = "Updates an existing activity by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Activity ID") @PathVariable Long id, @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an activity", description = "Deletes an activity by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Activity ID") @PathVariable Long id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update activity status", description = "Updates the status of an activity")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @Parameter(description = "Activity ID") @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(activityService.updateStatus(id, body.get("status")));
    }

    @PostMapping("/{id}/register")
    @Operation(summary = "Register for activity", description = "Registers a user as a participant for an activity")
    public ResponseEntity<Map<String, Object>> register(
            @Parameter(description = "Activity ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        var userId = ((Number) body.get("user_id")).longValue();
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.registerParticipant(id, userId));
    }

    @DeleteMapping("/{id}/register")
    @Operation(summary = "Cancel registration", description = "Cancels a user's registration for an activity")
    public ResponseEntity<Void> cancelRegistration(
            @Parameter(description = "Activity ID") @PathVariable Long id,
            @Parameter(description = "User ID to cancel registration for") @RequestParam Long userId) {
        activityService.cancelRegistration(id, userId);
        return ResponseEntity.noContent().build();
    }
}
