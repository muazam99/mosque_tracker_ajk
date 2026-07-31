package com.qiyam.committee.controller;

import com.qiyam.committee.dto.CommitteeRequest;
import com.qiyam.committee.dto.MemberRequest;
import com.qiyam.committee.dto.RoleMemberRequest;
import com.qiyam.committee.service.CommitteeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Get all committees", description = "Returns a paginated list of all committees (a global, organization-wide role catalog — not mosque-scoped)")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(committeeService.getAllCommittees(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get committee by ID", description = "Returns a single committee by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Committee ID") @PathVariable String id) {
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
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Committee ID") @PathVariable String id, @RequestBody CommitteeRequest request) {
        var updated = committeeService.updateCommittee(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a committee", description = "Deletes a committee by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Committee ID") @PathVariable String id) {
        committeeService.deleteCommittee(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Members ─────────────────────────────────────────────

    @GetMapping("/{id}/members")
    @Operation(summary = "Get committee members", description = "Returns all members of a specific committee")
    public ResponseEntity<List<Map<String, Object>>> getMembers(@Parameter(description = "Committee ID") @PathVariable String id) {
        return ResponseEntity.ok(committeeService.getCommitteeMembers(id));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to committee", description = "Adds a new member to a specific committee")
    public ResponseEntity<Map<String, Object>> addMember(
            @Parameter(description = "Committee ID") @PathVariable String id, @RequestBody MemberRequest request) {
        var created = committeeService.addCommitteeMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/members/{memberId}")
    @Operation(summary = "Update committee member", description = "Updates the details of a committee member")
    public ResponseEntity<Map<String, Object>> updateMember(
            @Parameter(description = "Committee ID") @PathVariable String id,
            @Parameter(description = "Member ID") @PathVariable String memberId,
            @RequestBody MemberRequest request) {
        var updated = committeeService.updateCommitteeMember(id, memberId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove committee member", description = "Removes a member from a specific committee")
    public ResponseEntity<Void> removeMember(@Parameter(description = "Committee ID") @PathVariable String id, @Parameter(description = "Member ID") @PathVariable String memberId) {
        committeeService.removeCommitteeMember(id, memberId);
        return ResponseEntity.noContent().build();
    }

    // ─── Role Members ────────────────────────────────────────

    @GetMapping("/{id}/role-members")
    @Operation(summary = "Get committee role members", description = "Returns all users assigned to a committee role, across the mosques the caller has access to")
    public ResponseEntity<List<Map<String, Object>>> getRoleMembers(@Parameter(description = "Committee role ID") @PathVariable String id) {
        return ResponseEntity.ok(committeeService.getRoleMembers(id));
    }

    @PostMapping("/{id}/role-members")
    @Operation(summary = "Assign a user to a committee role", description = "Assigns a user to a committee role at a specific mosque")
    public ResponseEntity<Map<String, Object>> addRoleMember(
            @Parameter(description = "Committee role ID") @PathVariable String id, @RequestBody RoleMemberRequest request) {
        var created = committeeService.addRoleMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}/role-members/{memberId}")
    @Operation(summary = "Remove a committee role member", description = "Removes a user's assignment to a committee role")
    public ResponseEntity<Void> removeRoleMember(
            @Parameter(description = "Committee role ID") @PathVariable String id,
            @Parameter(description = "Role member ID") @PathVariable String memberId) {
        committeeService.removeRoleMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
