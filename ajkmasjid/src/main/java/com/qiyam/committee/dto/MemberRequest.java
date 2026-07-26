package com.qiyam.committee.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberRequest {
    private String userId;
    private Long committeeRoleId;
    private LocalDate appointmentStart;
    private LocalDate appointmentEnd;
    private String status;
    private String appointedBy;
}
