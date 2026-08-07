package com.qiyam.donation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DonationRequest {
    private Long mosqueId;
    private Long campaignId;
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private BigDecimal amount;
    private String paymentMethod;
    private String type;
    private Boolean isAnonymous;
    private String status;
    private LocalDate date;
    private String notes;
}
