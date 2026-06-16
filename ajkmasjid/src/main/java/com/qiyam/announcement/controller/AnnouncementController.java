package com.qiyam.announcement.controller;

import com.qiyam.announcement.dto.AnnouncementRequest;
import com.qiyam.announcement.service.AnnouncementService;
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
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Manage announcements including CRUD, publishing, and unpublishing")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @GetMapping
    @Operation(summary = "Get all announcements", description = "Returns a paginated list of all announcements")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(announcementService.getAll(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get announcement by ID", description = "Returns a single announcement by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Announcement ID") @PathVariable Long id) {
        return announcementService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create an announcement", description = "Creates a new announcement")
    public ResponseEntity<Map<String, Object>> create(@RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an announcement", description = "Updates an existing announcement by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Announcement ID") @PathVariable Long id, @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an announcement", description = "Deletes an announcement by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Announcement ID") @PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish an announcement", description = "Publishes an announcement, making it visible to users")
    public ResponseEntity<Map<String, Object>> publish(@Parameter(description = "Announcement ID") @PathVariable Long id) {
        return ResponseEntity.ok(announcementService.publish(id));
    }

    @PatchMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish an announcement", description = "Unpublishes an announcement, hiding it from users")
    public ResponseEntity<Map<String, Object>> unpublish(@Parameter(description = "Announcement ID") @PathVariable Long id) {
        return ResponseEntity.ok(announcementService.unpublish(id));
    }
}
