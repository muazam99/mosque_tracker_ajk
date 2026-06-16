package com.qiyam.donation.controller;

import com.qiyam.donation.dto.CampaignRequest;
import com.qiyam.donation.dto.DonationRequest;
import com.qiyam.donation.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Manage donations and fundraising campaigns")
public class DonationController {
    private final DonationService donationService;

    // ─── Donations ─────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all donations", description = "Returns a paginated list of all donations")
    public ResponseEntity<List<Map<String, Object>>> getAllDonations(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(donationService.getAllDonations(limit, offset));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donation by ID", description = "Returns a single donation by its unique identifier")
    public ResponseEntity<Map<String, Object>> getDonationById(@Parameter(description = "Donation ID") @PathVariable Long id) {
        return donationService.getDonationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a donation", description = "Records a new donation")
    public ResponseEntity<Map<String, Object>> createDonation(@RequestBody DonationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.createDonation(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a donation", description = "Deletes a donation record by ID")
    public ResponseEntity<Void> deleteDonation(@Parameter(description = "Donation ID") @PathVariable Long id) {
        donationService.deleteDonation(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Campaigns ─────────────────────────────────────────

    @GetMapping("/campaigns")
    @Operation(summary = "Get all campaigns", description = "Returns a paginated list of all fundraising campaigns")
    public ResponseEntity<List<Map<String, Object>>> getAllCampaigns(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(donationService.getAllCampaigns(limit, offset));
    }

    @GetMapping("/campaigns/{id}")
    @Operation(summary = "Get campaign by ID", description = "Returns a single fundraising campaign by its ID")
    public ResponseEntity<Map<String, Object>> getCampaignById(@Parameter(description = "Campaign ID") @PathVariable Long id) {
        return donationService.getCampaignById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/campaigns")
    @Operation(summary = "Create a campaign", description = "Creates a new fundraising campaign")
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.createCampaign(request));
    }

    @PutMapping("/campaigns/{id}")
    @Operation(summary = "Update a campaign", description = "Updates an existing fundraising campaign by ID")
    public ResponseEntity<Map<String, Object>> updateCampaign(
            @Parameter(description = "Campaign ID") @PathVariable Long id, @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(donationService.updateCampaign(id, request));
    }
}
