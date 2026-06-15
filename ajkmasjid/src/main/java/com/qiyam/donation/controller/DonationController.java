package com.qiyam.donation.controller;

import com.qiyam.donation.dto.CampaignRequest;
import com.qiyam.donation.dto.DonationRequest;
import com.qiyam.donation.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {
    private final DonationService donationService;

    // ─── Donations ─────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDonations(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(donationService.getAllDonations(limit, offset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDonationById(@PathVariable Long id) {
        return donationService.getDonationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDonation(@RequestBody DonationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.createDonation(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDonation(@PathVariable Long id) {
        donationService.deleteDonation(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Campaigns ─────────────────────────────────────────

    @GetMapping("/campaigns")
    public ResponseEntity<List<Map<String, Object>>> getAllCampaigns(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(donationService.getAllCampaigns(limit, offset));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<Map<String, Object>> getCampaignById(@PathVariable Long id) {
        return donationService.getCampaignById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.createCampaign(request));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<Map<String, Object>> updateCampaign(
            @PathVariable Long id, @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(donationService.updateCampaign(id, request));
    }
}
