package com.qiyam.mosque.controller;

import com.qiyam.mosque.dto.MosqueRequest;
import com.qiyam.mosque.service.MosqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mosques")
@RequiredArgsConstructor
@Tag(name = "Mosques", description = "Manage mosques including CRUD, search, and location-based queries")
public class MosqueController {
    private final MosqueService mosqueService;

    @GetMapping
    @Operation(summary = "Get all mosques", description = "Returns a paginated list of all mosques")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(mosqueService.getAllMosques(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get mosque by ID", description = "Returns a single mosque by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Mosque ID") @PathVariable Long id) {
        return mosqueService.getMosqueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/country/{countryId}")
    @Operation(summary = "Get mosques by country", description = "Returns mosques filtered by country ID")
    public ResponseEntity<List<Map<String, Object>>> getByCountry(
            @Parameter(description = "Country ID") @PathVariable Long countryId,
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mosqueService.getMosquesByCountry(countryId, limit));
    }

    @GetMapping("/state/{stateId}")
    @Operation(summary = "Get mosques by state", description = "Returns mosques filtered by state ID")
    public ResponseEntity<List<Map<String, Object>>> getByState(
            @Parameter(description = "State ID") @PathVariable Long stateId,
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mosqueService.getMosquesByState(stateId, limit));
    }

    @GetMapping("/search")
    @Operation(summary = "Search mosques", description = "Search mosques by name, address, or other criteria")
    public ResponseEntity<List<Map<String, Object>>> search(
            @Parameter(description = "Search query string") @RequestParam String query,
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(mosqueService.searchMosques(query, limit));
    }

    @PostMapping
    @Operation(summary = "Create a mosque", description = "Creates a new mosque record")
    public ResponseEntity<Map<String, Object>> create(@RequestBody MosqueRequest request) {
        var created = mosqueService.createMosque(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a mosque", description = "Updates an existing mosque record by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Mosque ID") @PathVariable Long id, @RequestBody MosqueRequest request) {
        var updated = mosqueService.updateMosque(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a mosque", description = "Deletes a mosque record by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Mosque ID") @PathVariable Long id) {
        mosqueService.deleteMosque(id);
        return ResponseEntity.noContent().build();
    }
}
