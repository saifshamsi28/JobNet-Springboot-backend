package com.saif.JobNet.model;

import lombok.Data;

@Data
public class ResumeResponseEntity {
    private String message;
    private int status;

    public ResumeResponseEntity(String message, int status) {
        this.message = message;
        this.status = status;
    }
}
