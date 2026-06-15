package com.qiyam.mosque.controller;

import com.qiyam.mosque.dto.MosqueRequest;
import com.qiyam.mosque.service.MosqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mosques")
@RequiredArgsConstructor
public class MosqueController {
    private final MosqueService mosqueService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(mosqueService.getAllMosques(limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return mosqueService.getMosqueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<List<Map<String, Object>>> getByCountry(
            @PathVariable Long countryId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mosqueService.getMosquesByCountry(countryId, limit));
    }

    @GetMapping("/state/{stateId}")
    public ResponseEntity<List<Map<String, Object>>> getByState(
            @PathVariable Long stateId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mosqueService.getMosquesByState(stateId, limit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mosqueService.searchMosques(query, limit));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody MosqueRequest request) {
        var created = mosqueService.createMosque(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody MosqueRequest request) {
        var updated = mosqueService.updateMosque(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mosqueService.deleteMosque(id);
        return ResponseEntity.noContent().build();
    }
}
