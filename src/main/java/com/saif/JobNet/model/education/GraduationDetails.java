package com.saif.JobNet.model.education;

import lombok.Data;

@Data
public class GraduationDetails {
    private String course;
    private String specialization;
    private String college;
    private String courseType;
    private String gpaScale;
    private String cgpaObtained;
    private String enrollmentYear;
    private String passingYear;
}