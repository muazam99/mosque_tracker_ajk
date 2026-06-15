package com.qiyam.document.service;

import com.qiyam.document.dto.DocumentRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final SupabaseClient supabaseClient;

    public List<Map<String, Object>> getAll(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("mosque_documents", params, Map.class);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("mosque_documents", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> upload(DocumentRequest request) {
        var body = toMap(request);
        var result = supabaseClient.post("mosque_documents", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> update(Long id, DocumentRequest request) {
        var body = toMap(request);
        var result = supabaseClient.patch("mosque_documents", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void delete(Long id) {
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
