package com.qiyam.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinanceAccountRequest {
    private Long mosqueId;
    private String accountName;
    private String accountType;
    private BigDecimal balance;
    private String bankName;
    private String accountNumber;
    private String status;
}
