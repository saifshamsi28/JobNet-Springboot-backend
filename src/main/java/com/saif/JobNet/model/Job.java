package com.saif.JobNet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String company;

    @JsonProperty("location")
    private String location;

    @JsonProperty("salary")
    private String salary;

    private long minSalary;

    private long maxSalary;

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
