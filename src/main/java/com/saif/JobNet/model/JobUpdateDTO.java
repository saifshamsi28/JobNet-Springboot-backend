package com.saif.JobNet.model;

import lombok.Data;

@Data
public class JobUpdateDTO {
    private String url;
    private String fullDescription;

    public JobUpdateDTO() {}
}
