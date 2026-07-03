package com.qiyam.document.controller;

import com.qiyam.document.dto.DocumentRequest;
import com.qiyam.document.service.DocumentService;
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
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Manage documents including upload, download, and CRUD operations")
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "Get all documents", description = "Returns a paginated list of all documents")
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(documentService.getAll(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Returns a single document by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Document ID") @PathVariable Long id) {
        return documentService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a document", description = "Uploads a new document to the system")
    public ResponseEntity<Map<String, Object>> upload(@RequestBody DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document", description = "Updates an existing document by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Document ID") @PathVariable Long id, @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a document", description = "Deletes a document by ID")
    public ResponseEntity<Void> delete(@Parameter(description = "Document ID") @PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a document", description = "Returns download information for a specific document")
    public ResponseEntity<Map<String, Object>> download(@Parameter(description = "Document ID") @PathVariable Long id) {
        return documentService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
