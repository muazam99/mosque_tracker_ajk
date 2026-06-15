package com.qiyam.donation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DonationRequest {
    private Long campaignId;
    private String donorName;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private LocalDate date;
    private String notes;
}
