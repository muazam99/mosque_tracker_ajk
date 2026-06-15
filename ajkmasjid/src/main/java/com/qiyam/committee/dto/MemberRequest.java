package com.qiyam.committee.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberRequest {
    private Long userId;
    private Long committeeRoleId;
    private LocalDate appointmentStart;
    private LocalDate appointmentEnd;
    private String status;
    private Long appointedBy;
}
