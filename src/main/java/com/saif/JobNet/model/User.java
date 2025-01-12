package com.saif.JobNet.model;

import lombok.Data;
import lombok.Generated;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String phoneNumber;
    @DBRef
    private List<Job> savedJobs=new ArrayList<>();
}
