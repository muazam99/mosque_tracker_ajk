package com.qiyam.document.controller;

import com.qiyam.document.dto.DocumentRequest;
import com.qiyam.document.service.DocumentService;
import com.qiyam.shared.dto.PagedResponse;
import com.qiyam.shared.util.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Manage documents including upload, download, and CRUD operations")
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "Get all documents", description = "Returns a paginated list of all documents")
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
        return ResponseEntity.ok(documentService.getAll(effectiveLimit, effectiveOffset, resolvedPage, mosqueId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Returns a single document by its unique identifier")
    public ResponseEntity<Map<String, Object>> getById(@Parameter(description = "Document ID") @PathVariable Long id) {
        return documentService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document", description = "Uploads a new document's file to R2 and records its metadata")
    public ResponseEntity<Map<String, Object>> upload(
            @Parameter(description = "Mosque ID this document belongs to") @RequestParam Long mosqueId,
            @Parameter(description = "Display name for the document") @RequestParam String name,
            @Parameter(description = "Document category") @RequestParam(required = false) String category,
            @Parameter(description = "The file to upload") @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(mosqueId, name, category, file));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document", description = "Updates an existing document by ID")
    public ResponseEntity<Map<String, Object>> update(@Parameter(description = "Document ID") @PathVariable Long id, @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @DeleteMapping("/{id}")
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
