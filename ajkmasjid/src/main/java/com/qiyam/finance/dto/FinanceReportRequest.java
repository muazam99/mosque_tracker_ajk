package com.qiyam.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FinanceReportRequest {
    private Long mosqueId;
    private LocalDate auditPeriodStart;
    private LocalDate auditPeriodEnd;
    private Long auditedBy;
    private String reportUrl;
    private String status;
    private String remarks;
}
