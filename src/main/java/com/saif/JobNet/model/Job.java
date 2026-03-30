package com.saif.JobNet.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection ="jobs")
@Data //for automatic getter,setter,toString generation
public class Job {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("company")
    @JsonAlias({"company_name"})
    private String company;

    @JsonProperty("location")
    private String location;

    @JsonProperty("salary")
    private String salary;

    @JsonProperty("minSalary")
    private long minSalary;

    @JsonProperty("maxSalary")
    private long maxSalary;

    @JsonProperty("link")
    @JsonAlias({"url"})
    private String url;

    @JsonProperty("rating")
    private String rating;

    @JsonProperty("reviews")
    private String reviews;

    @JsonProperty("openings")
    private String openings;

    @JsonProperty("category")
    private String category;

    @JsonProperty("requiredSkills")
    private List<String> requiredSkills = new ArrayList<>();

    @JsonProperty("applicants")
    private String applicants;

    @JsonProperty("post_date")
    private String post_date;

    @JsonProperty("description")
    @JsonAlias({"shortDescription"})
    private String shortDescription;

    @JsonProperty("full_description")
    @JsonAlias({"fullDescription"})
    private String fullDescription;

    @JsonProperty("jobType")
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @JsonProperty("workMode")
    private WorkMode workMode = WorkMode.ONSITE;

    @JsonProperty("source")
    private String source;

    @JsonProperty("postedByUserId")
    private String postedByUserId;

    @JsonProperty("status")
    private JobStatus status = JobStatus.PUBLISHED;

    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateTime;

    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
