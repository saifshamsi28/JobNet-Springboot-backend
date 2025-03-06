package com.saif.JobNet.model;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserModel {
    private String id;
    private String name;
    private String userName;
    private String email;
    private String password;
    private String phoneNumber;
    private List<String> savedJobs;
}
