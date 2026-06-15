package com.qiyam.activity.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityRequest {
    private Long mosqueId;
    private String title;
    private String description;
    private String status;
    private String activityStatus;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean isRecurring;
    private String recurrenceType;
    private Integer recurrenceInterval;
    private String recurrenceByWeekday;
    private LocalDateTime recurrenceUntil;
}
