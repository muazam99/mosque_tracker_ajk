package com.qiyam.committee.controller;

import com.qiyam.committee.dto.CommitteeRequest;
import com.qiyam.committee.dto.MemberRequest;
import com.qiyam.committee.service.CommitteeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/committees")
@RequiredArgsConstructor
public class CommitteeController {
    private final CommitteeService committeeService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(committeeService.getAllCommittees(limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return committeeService.getCommitteeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CommitteeRequest request) {
        var created = committeeService.createCommittee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody CommitteeRequest request) {
        var updated = committeeService.updateCommittee(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        committeeService.deleteCommittee(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Members ─────────────────────────────────────────────

    @GetMapping("/{id}/members")
    public ResponseEntity<List<Map<String, Object>>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(committeeService.getCommitteeMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable Long id, @RequestBody MemberRequest request) {
        var created = committeeService.addCommitteeMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/members/{memberId}")
    public ResponseEntity<Map<String, Object>> updateMember(
            @PathVariable Long id, @PathVariable Long memberId, @RequestBody MemberRequest request) {
        var updated = committeeService.updateCommitteeMember(id, memberId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        committeeService.removeCommitteeMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
