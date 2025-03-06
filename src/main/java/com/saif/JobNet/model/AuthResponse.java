package com.saif.JobNet.model;

import lombok.Data;

@Data
public class AuthResponse {
    private String message;
    private int status;

    public AuthResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }
}


