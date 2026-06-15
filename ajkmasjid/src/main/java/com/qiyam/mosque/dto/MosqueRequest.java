package com.qiyam.mosque.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MosqueRequest {
    private String name;
    private String thumbnailUrl;
    private List<String> imageUrls;
    private String description;
    private String googleMapsEmbedded;
    private String googleMapsUrl;
    private Double latitude;
    private Double longitude;
    private String address;
    private Map<String, Object> reviewsPerRating;
    private Long countryId;
    private Long stateId;
    private Long districtId;
    private String websiteUrl;
    private String phone;
    private String email;
    private String category;
    private String status;
}
