package com.saif.JobNet.model;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
public class User {
    @Id
    private String id;
    @NonNull
    private String name;
    @Indexed(unique = true)
    @NonNull
    private String userName;
    @NonNull
    @Indexed(unique = true)
    private String email;
    @NonNull
    private String password;
    private String profileImage;
    private String phoneNumber;
    private boolean isResumeUploaded;
    private String resumeUrl;
    private String resumeName;
    private String resumeUploadDate;
    private String resumeSize;

    @DBRef
    private List<Job> savedJobs = new ArrayList<>();

    @Field("basic_details") // Embedded document
    private BasicDetails basicDetails;

    public void addOrRemoveJobToUser(Job job) {
        boolean found = false;

        // Iterate through the saved jobs and compare by ID
        for (Job savedJob : savedJobs) {
            if (savedJob.getId().equals(job.getId())) {
                savedJobs.remove(savedJob);
                System.out.println("Removing the job: " + job.getId() + " , title: " + job.getTitle());
                found = true;
                break;
            }
        }

        if (!found) {
            savedJobs.add(job);
//            job.setSaved(true);
            System.out.println("Adding the job: " + job.getId() + " , title: " + job.getTitle());
        }else {
//            savedJobs.remove(job);
//            job.setSaved(false);
            System.out.println("removing the job: " + job.getId() + " , title: " + job.getTitle());
        }
    }

}
