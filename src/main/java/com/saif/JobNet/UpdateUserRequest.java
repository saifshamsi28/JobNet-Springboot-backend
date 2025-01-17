package com.saif.JobNet;

import com.saif.JobNet.model.Job;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    private String id;
    private String name;
    private String userName;
    private String email;
    private String password;
    private String phoneNumber;
    private List<String> savedJobs;
}
