package com.saif.JobNet.model;

import com.saif.JobNet.model.education.Class10Details;
import com.saif.JobNet.model.education.Class12Details;
import com.saif.JobNet.model.education.GraduationDetails;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.Iterator;
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
    private Resume resume;

    @DBRef
    private List<Job> savedJobs = new ArrayList<>();

    @Field("basic_details") // Embedded document
    private BasicDetails basicDetails;

    @Field("graduation_details")
    private GraduationDetails graduationDetails;

    @Field("class12th_details")
    private Class12Details class12Details;

    @Field("class10th_details")
    private Class10Details class10Details;

    private List<String> skills=new ArrayList<>();

    public void addOrRemoveJobToUser(Job job) {
        boolean found = false;

        // Use iterator to safely remove while iterating
        Iterator<Job> iterator = savedJobs.iterator();
        while (iterator.hasNext()) {
            Job savedJob = iterator.next();

            if (savedJob == null) continue; // SKIP null jobs to avoid crash

            if (savedJob.getId().equals(job.getId())) {
                iterator.remove();
                System.out.println("Removing the job: " + job.getId() + " , title: " + job.getTitle());
                found = true;
                break;
            }
        }

        if (!found) {
            savedJobs.add(job);
            System.out.println("Adding the job: " + job.getId() + " , title: " + job.getTitle());
        } else {
            System.out.println("removing the job: " + job.getId() + " , title: " + job.getTitle());
        }
    }

}
