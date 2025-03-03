package com.saif.JobNet.exception_handling;

public class ResumeResponseEntity {
    private String message;
    private int status;

    public ResumeResponseEntity(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
