package com.qiyam.user.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String email;
    private String username;
    private String fullname;
    private String phone;
    private String role;
    private String imagePath;
    private String status;
}

//deployment trigger
