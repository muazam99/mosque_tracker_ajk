package com.qiyam.meeting.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MeetingRequest {
    private Long mosqueId;
    private Long committeeId;
    private String title;
    private String agenda;
    private String location;
    private LocalDateTime scheduledAt;
    private String status;
}
