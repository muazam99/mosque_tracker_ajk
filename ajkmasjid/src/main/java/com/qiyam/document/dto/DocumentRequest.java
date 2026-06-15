package com.qiyam.document.dto;

import lombok.Data;

@Data
public class DocumentRequest {
    private Long mosqueId;
    private String name;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String category;
    private Long uploadedBy;
}
