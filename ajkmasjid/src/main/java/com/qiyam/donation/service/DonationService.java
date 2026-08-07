package com.qiyam.donation.service;

import com.qiyam.donation.dto.CampaignRequest;
import com.qiyam.donation.dto.DonationRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.dto.PagedResponse;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class DonationService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    private void applyMosqueScope(Map<String, String> params, Set<Integer> scope) {
        if (scope == null) return; // unrestricted (SUPER_ADMIN)
        if (scope.size() == 1) {
            params.put("mosque_id", "eq." + scope.iterator().next());
        } else {
            params.put("mosque_id", "in.(" + scope.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")");
        }
    }

    // ─── Donations ─────────────────────────────────────────

    public PagedResponse<Map<String, Object>> getAllDonations(int limit, int offset, int page, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.DONATIONS_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return PagedResponse.empty(page, limit);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "date.desc");
        applyMosqueScope(params, scope);
        var result = supabaseClient.getAllPaged("donations", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getDonationById(Long id) {
        accessControlService.requirePermission(null, Permission.DONATIONS_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("donations", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> createDonation(DonationRequest request) {
        accessControlService.requirePermission(null, Permission.DONATIONS_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
        var body = donationToMap(request);
        var result = supabaseClient.post("donations", body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteDonation(Long id) {
        accessControlService.requirePermission(null, Permission.DONATIONS_DELETE);
        verifyMosqueOwnership("donations", String.valueOf(id));
        supabaseClient.delete("donations", "id", String.valueOf(id));
    }

    /** Fetches a row by id and, if found, verifies the caller has access to its mosque_id. */
    private void verifyMosqueOwnership(String table, String id) {
        var row = supabaseClient.getOne(table, "id", id, Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, ((Map<?, ?>) r).get("mosque_id")));
    }

    // ─── Campaigns ─────────────────────────────────────────

    public PagedResponse<Map<String, Object>> getAllCampaigns(int limit, int offset, int page, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.DONATIONS_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return PagedResponse.empty(page, limit);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "start_date.desc");
        applyMosqueScope(params, scope);
        var result = supabaseClient.getAllPaged("donation_campaigns", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getCampaignById(Long id) {
        accessControlService.requirePermission(null, Permission.DONATIONS_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("donation_campaigns", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> createCampaign(CampaignRequest request) {
        accessControlService.requirePermission(null, Permission.DONATIONS_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
        var body = campaignToMap(request);
        var result = supabaseClient.post("donation_campaigns", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateCampaign(Long id, CampaignRequest request) {
        accessControlService.requirePermission(null, Permission.DONATIONS_WRITE);
        verifyMosqueOwnership("donation_campaigns", String.valueOf(id));
        var body = campaignToMap(request);
        var result = supabaseClient.patch("donation_campaigns", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    // ─── Mappers ───────────────────────────────────────────

    private Map<String, Object> donationToMap(DonationRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "campaign_id", r.getCampaignId());
        putIfNotNull(map, "donor_name", r.getDonorName());
        putIfNotNull(map, "donor_email", r.getDonorEmail());
        putIfNotNull(map, "donor_phone", r.getDonorPhone());
        putIfNotNull(map, "amount", r.getAmount());
        putIfNotNull(map, "payment_method", r.getPaymentMethod());
        putIfNotNull(map, "type", r.getType());
        putIfNotNull(map, "is_anonymous", r.getIsAnonymous());
        putIfNotNull(map, "status", r.getStatus());
        putIfNotNull(map, "date", r.getDate());
        putIfNotNull(map, "notes", r.getNotes());
        return map;
    }

    private Map<String, Object> campaignToMap(CampaignRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "title", r.getTitle());
        putIfNotNull(map, "description", r.getDescription());
        putIfNotNull(map, "target_amount", r.getTargetAmount());
        putIfNotNull(map, "collected_amount", r.getCollectedAmount());
        putIfNotNull(map, "start_date", r.getStartDate());
        putIfNotNull(map, "end_date", r.getEndDate());
        putIfNotNull(map, "status", r.getStatus());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
