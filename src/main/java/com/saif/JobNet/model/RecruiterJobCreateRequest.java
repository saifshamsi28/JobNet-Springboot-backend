package com.saif.JobNet.model;

import lombok.Data;

import java.util.List;

@Data
public class RecruiterJobCreateRequest {
    private String title;
    private String company;
    private String location;
    private String salary;
    private String openings;
    private String shortDescription;
    private String fullDescription;
    private String employmentType;
    private String jobType;
    private String workMode;
    private String category;
    private List<String> requiredSkills;
    private String url;
    private String status;
}
