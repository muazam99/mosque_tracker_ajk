package com.qiyam.committee.dto;

import lombok.Data;

@Data
public class CommitteeRequest {
    private Long mosqueId;
    private String roleName;
    private String description;
    private Integer hierarchyLevel;
    private Boolean isSystemRole;
}
