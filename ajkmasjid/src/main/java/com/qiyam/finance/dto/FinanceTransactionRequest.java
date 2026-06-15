package com.qiyam.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceTransactionRequest {
    private Long mosqueId;
    private Long financeAccountId;
    private Long categoryId;
    private String transactionType;
    private BigDecimal amount;
    private String referenceNo;
    private String description;
    private LocalDate transactionDate;
    private String receiptUrl;
    private Long createdBy;
    private Long approvedBy;
    private String status;
}
