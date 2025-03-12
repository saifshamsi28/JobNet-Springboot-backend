package com.saif.JobNet.model;

import lombok.Data;

@Data
public class JobNetResponse {
    private String message;
    private int status;

    public JobNetResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }
}
