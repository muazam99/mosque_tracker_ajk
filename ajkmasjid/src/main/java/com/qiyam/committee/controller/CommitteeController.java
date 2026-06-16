package com.qiyam.committee.controller;

import com.qiyam.committee.dto.CommitteeRequest;
import com.qiyam.committee.dto.MemberRequest;
import com.qiyam.committee.service.CommitteeService;
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
@RequestMapping("/committees")
@RequiredArgsConstructor
@Tag(name = "Committees", description = "Manage committees and their members including CRUD operations")
public class CommitteeController {
    private final CommitteeService committeeService;

    @GetMapping
    @Operation(summary = "Get all committees", description = "Returns a paginated list of all committees")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(committeeService.getAllCommittees(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get committee by ID", description = "Returns a single committee by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Committee ID") @PathVariable Long id) {
        return committeeService.getCommitteeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a committee", description = "Creates a new committee")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CommitteeRequest request) {
        var created = committeeService.createCommittee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a committee", description = "Updates an existing committee by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Committee ID") @PathVariable Long id, @RequestBody CommitteeRequest request) {
        var updated = committeeService.updateCommittee(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a committee", description = "Deletes a committee by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Committee ID") @PathVariable Long id) {
        committeeService.deleteCommittee(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Members ─────────────────────────────────────────────

    @GetMapping("/{id}/members")
    @Operation(summary = "Get committee members", description = "Returns all members of a specific committee")
    public ResponseEntity<List<Map<String, Object>>> getMembers(@Parameter(description = "Committee ID") @PathVariable Long id) {
        return ResponseEntity.ok(committeeService.getCommitteeMembers(id));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to committee", description = "Adds a new member to a specific committee")
    public ResponseEntity<Map<String, Object>> addMember(
            @Parameter(description = "Committee ID") @PathVariable Long id, @RequestBody MemberRequest request) {
        var created = committeeService.addCommitteeMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/members/{memberId}")
    @Operation(summary = "Update committee member", description = "Updates the details of a committee member")
    public ResponseEntity<Map<String, Object>> updateMember(
            @Parameter(description = "Committee ID") @PathVariable Long id,
            @Parameter(description = "Member ID") @PathVariable Long memberId,
            @RequestBody MemberRequest request) {
        var updated = committeeService.updateCommitteeMember(id, memberId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove committee member", description = "Removes a member from a specific committee")
    public ResponseEntity<Void> removeMember(@Parameter(description = "Committee ID") @PathVariable Long id, @Parameter(description = "Member ID") @PathVariable Long memberId) {
        committeeService.removeCommitteeMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
