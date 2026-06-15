package com.qiyam.donation.service;

import com.qiyam.donation.dto.CampaignRequest;
import com.qiyam.donation.dto.DonationRequest;
import com.qiyam.shared.client.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class DonationService {
    private final SupabaseClient supabaseClient;

    // ─── Donations ─────────────────────────────────────────

    public List<Map<String, Object>> getAllDonations(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "date.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("donations", params, Map.class);
    }

    public Optional<Map<String, Object>> getDonationById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("donations", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createDonation(DonationRequest request) {
        var body = donationToMap(request);
        var result = supabaseClient.post("donations", body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteDonation(Long id) {
        supabaseClient.delete("donations", "id", String.valueOf(id));
    }

    // ─── Campaigns ─────────────────────────────────────────

    public List<Map<String, Object>> getAllCampaigns(int limit, int offset) {
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "start_date.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("donation_campaigns", params, Map.class);
    }

    public Optional<Map<String, Object>> getCampaignById(Long id) {
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("donation_campaigns", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createCampaign(CampaignRequest request) {
        var body = campaignToMap(request);
        var result = supabaseClient.post("donation_campaigns", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateCampaign(Long id, CampaignRequest request) {
        var body = campaignToMap(request);
        var result = supabaseClient.patch("donation_campaigns", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    // ─── Mappers ───────────────────────────────────────────

    private Map<String, Object> donationToMap(DonationRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "campaign_id", r.getCampaignId());
        putIfNotNull(map, "donor_name", r.getDonorName());
        putIfNotNull(map, "amount", r.getAmount());
        putIfNotNull(map, "payment_method", r.getPaymentMethod());
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
