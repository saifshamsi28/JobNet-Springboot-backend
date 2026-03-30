package com.saif.JobNet.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "applications")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_user_job_application", def = "{ 'userId': 1, 'jobId': 1 }", unique = true),
        @CompoundIndex(name = "idx_recruiter_status", def = "{ 'recruiterId': 1, 'status': 1 }")
})
public class JobApplication {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String recruiterId;

    @Indexed
    private String jobId;

    private String jobTitle;
    private String company;
    private String resumeUrl;
    private String coverLetter;

    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime appliedAt;

    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
