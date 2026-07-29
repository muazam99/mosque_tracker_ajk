package com.qiyam.committee.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberRequest {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("committee_role_id")
    private Long committeeRoleId;
    private LocalDate appointmentStart;
    private LocalDate appointmentEnd;
    private String status;
    @JsonProperty("appointed_by")
    private String appointedBy;
}
