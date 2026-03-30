package com.saif.JobNet.model;

import lombok.Data;

@Data
public class ApplyJobRequest {
    private String userId;
    private String jobId;
    private String resumeUrl;
    private String coverLetter;
}
