package com.saif.JobNet.model;

import lombok.Data;

@Data
public class AuthResponse {
    private String message;
    private int status;
    private String accessToken;
    private String refreshToken;

    public AuthResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public AuthResponse(String message, int status, String accessToken, String refreshToken) {
        this.message = message;
        this.status = status;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}


