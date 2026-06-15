package com.qiyam.announcement.dto;

import lombok.Data;

@Data
public class AnnouncementRequest {
    private Long mosqueId;
    private String title;
    private String content;
    private String status;
}
