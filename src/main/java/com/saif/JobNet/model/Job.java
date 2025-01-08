package com.saif.JobNet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection ="jobs")
@Data //for automatic getter,setter,toString generator
public class Job {

    @Id
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("company")
    private String company;

    @JsonProperty("location")
    private String location;

    @JsonProperty("salary")
    private String salary;

    @JsonProperty("url")
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

//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public void setLocation(String location) {
//        this.location = location;
//    }
//
//    public void setSalary(String salary) {
//        this.salary = salary;
//    }
//
//    public void setUrl(String url) {
//        this.url = url;
//    }
//
//    public void setRating(String rating) {
//        this.rating = rating;
//    }
//
//    public void setReviews(String reviews) {
//        this.reviews = reviews;
//    }
//
//    public void setPost_date(String post_date) {
//        this.post_date = post_date;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public void setCompany(String company) {
//        this.company = company;
//    }
//
////    public void setOpenings(String openings) {
////        this.openings = openings;
////    }
////    public void setApplicants(String applicants) {
////        this.applicants = applicants;
////    }
//
//    public String getId() {
//        return id;
//    }
//    public String getTitle() {
//        return title;
//    }
//
//    public String getCompany() {
//        return company;
//    }
//
//    public String getLocation() {
//        return location;
//    }
//
//    public String getSalary() {
//        return salary;
//    }
//
////    public String getOpenings() {
////        return openings;
////    }
////    public String getApplicants() {
////        return applicants;
////    }
//    public String getUrl() {
//        return url;
//    }
//
//    public String getRating() {
//        return rating;
//    }
//    public String getPostDate() {
//        return post_date;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//    public String getReviews() {
//        return reviews;
//    }
//    public LocalDateTime getDate() {
//        return date;
//    }
//
//    public void setDate(LocalDateTime date) {
//        this.date = date;
//    }


    @Override
    public String toString() {
        return "id=" + id + '\n' +
                " title=" + title + '\n' +
                " company=" + company + '\n' +
                " location=" + location + '\n' +
                " salary=" + salary + '\n' +
                " url=" + url + '\n' +
                " rating=" + rating + '\n' +
                " reviews=" + reviews + '\n' +
                " post_date=" + post_date + '\n' +
                " date_added=" + date + '\n' +
                " description=" + description + '\n';
    }
}
