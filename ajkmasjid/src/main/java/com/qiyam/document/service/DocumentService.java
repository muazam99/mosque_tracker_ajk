package com.qiyam.document.service;

import com.qiyam.document.dto.DocumentRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.dto.PagedResponse;
import com.qiyam.shared.exception.StorageException;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import com.qiyam.shared.storage.R2StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;
    private final R2StorageClient r2StorageClient;

    private void applyMosqueScope(Map<String, String> params, Set<Integer> scope) {
        if (scope == null) return; // unrestricted (SUPER_ADMIN)
        if (scope.size() == 1) {
            params.put("mosque_id", "eq." + scope.iterator().next());
        } else {
            params.put("mosque_id", "in.(" + scope.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")");
        }
    }

    /** Fetches a row by id and, if found, verifies the caller has access to its mosque_id. */
    private void verifyMosqueOwnership(String table, String id) {
        var row = supabaseClient.getOne(table, "id", id, Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, ((Map<?, ?>) r).get("mosque_id")));
    }

    public PagedResponse<Map<String, Object>> getAll(int limit, int offset, int page, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.DOCUMENTS_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return PagedResponse.empty(page, limit);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        applyMosqueScope(params, scope);
        var result = supabaseClient.getAllPaged("mosque_documents", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        accessControlService.requirePermission(null, Permission.DOCUMENTS_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("mosque_documents", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    /**
     * Uploads {@code file}'s bytes to R2 and records the resulting object URL as a new
     * {@code mosque_documents} row. {@code uploaded_by} is intentionally not set here — the
     * column is a {@code Long} but the authenticated user's id is a UUID (see
     * {@link com.qiyam.shared.security.UserPrincipal#userId()}), so there's no correct value
     * to derive from the caller without a schema change.
     */
    public Map<String, Object> upload(Long mosqueId, String name, String category, MultipartFile file) {
        accessControlService.requirePermission(null, Permission.DOCUMENTS_WRITE);
        if (mosqueId != null) {
            accessControlService.requireRowMosqueAccess(null, mosqueId);
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file: " + e.getMessage(), e);
        }
        var fileUrl = r2StorageClient.upload(content, file.getOriginalFilename(), file.getContentType(), category);

        var body = new HashMap<String, Object>();
        putIfNotNull(body, "mosque_id", mosqueId);
        putIfNotNull(body, "name", name);
        putIfNotNull(body, "file_url", fileUrl);
        putIfNotNull(body, "file_type", file.getContentType());
        putIfNotNull(body, "file_size", file.getSize());
        putIfNotNull(body, "category", category);
        var result = supabaseClient.post("mosque_documents", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, DocumentRequest request) {
        accessControlService.requirePermission(null, Permission.DOCUMENTS_WRITE);
        verifyMosqueOwnership("mosque_documents", String.valueOf(id));
        var body = toMap(request);
        var result = supabaseClient.patch("mosque_documents", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
        accessControlService.requirePermission(null, Permission.DOCUMENTS_DELETE);
        verifyMosqueOwnership("mosque_documents", String.valueOf(id));
        supabaseClient.delete("mosque_documents", "id", String.valueOf(id));
    }

    private Map<String, Object> toMap(DocumentRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "name", r.getName());
        putIfNotNull(map, "file_url", r.getFileUrl());
        putIfNotNull(map, "file_type", r.getFileType());
        putIfNotNull(map, "file_size", r.getFileSize());
        putIfNotNull(map, "category", r.getCategory());
        putIfNotNull(map, "uploaded_by", r.getUploadedBy());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
