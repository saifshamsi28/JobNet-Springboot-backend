package com.saif.JobNet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection ="jobs")
@Data //for automatic getter,setter,toString generation
public class Job {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("company")
    private String company;

    @JsonProperty("location")
    private String location;

    @JsonProperty("salary")
    private String salary;

    @JsonProperty("link")
    private String url;

    @JsonProperty("rating")
    private String rating;

    @JsonProperty("reviews")
    private String reviews;

    @JsonProperty("post_date")
    private String post_date;

    @JsonProperty("description")
    private String description;

    private LocalDateTime date;
}
