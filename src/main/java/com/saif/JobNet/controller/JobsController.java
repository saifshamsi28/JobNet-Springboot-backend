package com.saif.JobNet.controller;

import com.saif.JobNet.NumberExtractor;
import com.saif.JobNet.exception_handling.JobNotFoundException;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.JobUpdateDTO;
import com.saif.JobNet.model.RecruiterJobCreateRequest;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserRole;
import com.saif.JobNet.services.JWTService;
import com.saif.JobNet.services.UserService;
import com.saif.JobNet.services.JobsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/home")
public class JobsController {
    @Autowired
    JobsService jobsService;

    @Autowired
    UserService userService;

    @Autowired
    JWTService jwtService;

    @PostMapping("/recruiter/jobs")
    public ResponseEntity<?> createRecruiterJob(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestBody RecruiterJobCreateRequest request) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can post jobs"), HttpStatus.FORBIDDEN);
        }

        return jobsService.createRecruiterJob(user, request)
                .<ResponseEntity<?>>map(job -> new ResponseEntity<>(job, HttpStatus.CREATED))
                .orElseGet(() -> new ResponseEntity<>(Map.of("error", "Invalid job payload"), HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/recruiter/jobs/me")
    public ResponseEntity<?> myPostedJobs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can view recruiter jobs"), HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(jobsService.getJobsPostedByRecruiter(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/suggested-jobs")
    public List<Job> getSuggestedJobs(){
        List<Job> jobs = new ArrayList<>(jobsService.getAllJobs());
        jobs.sort(Comparator.comparing(Job::getDateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return jobs.stream().limit(10).toList();
    }

    @GetMapping("/recent-jobs")
    public List<Job> getRecent(){
        List<Job> jobs = new ArrayList<>(jobsService.getAllJobs());
        jobs.sort(Comparator.comparing(Job::getDateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return jobs.stream().limit(10).toList();
    }

    @GetMapping("/new-jobs")
    public ResponseEntity<List<Job>> getNewJobs() {
        List<Job> newJobs = jobsService.getNewJobs();
        return ResponseEntity.ok(newJobs);
    }

    @GetMapping("/job-details")
    public ResponseEntity<?> getJobDescription(@RequestParam("job_url") String job_url) {
        Optional<Job> jobBox=jobsService.getJobByUrl(job_url);
        if(jobBox.isPresent()) {
            Job job = jobBox.get();
            if(job.getFullDescription()==null){
                // Call Flask backend to fetch the description
                Job job1 = jobsService.fetchJobDescriptionFromFlask(job_url);
                if(job1==null ){
                    job.setFullDescription(null);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }else {
                    if(job1.getFullDescription().length()>100) {
                        String extractedReviews = NumberExtractor.extractNumber(job1.getReviews());
                        if (extractedReviews != null) {
                            job.setReviews(extractedReviews);
                        }

                        String extractedRating = NumberExtractor.extractNumber(job1.getRating());
                        if (extractedRating != null) {
                            job.setRating(extractedRating);
                        }

                        String extractedApplicants = NumberExtractor.extractNumber(job1.getApplicants());
                        if (extractedApplicants != null) {
                            job.setApplicants(extractedApplicants);
                        }

                        String extractedPostDate = NumberExtractor.extractNumber(job1.getPost_date());
                        if (extractedPostDate!=null) {
                            job.setPost_date(extractedPostDate);
                        }

                        job.setFullDescription(job1.getFullDescription());
                        return new ResponseEntity<>(job, HttpStatus.OK);
                    }else
                        job.setFullDescription(null);
                }

            }else {
                return new ResponseEntity<>(job,HttpStatus.OK);
            }
        }
            return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> insertAllJob(@RequestBody List<Job> jobs){
        System.out.println("no of job to insert: "+jobs.size());
            int noOfInsertedJobs=jobsService.insertAllJob(jobs);
            if(noOfInsertedJobs==0){
                Map<String, String> response = Map.of(
                        "message", "New jobs not inserted",
                        "status", "Failed"
                );
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
            }else {
                Map<String, String> response = Map.of(
                        "message", noOfInsertedJobs+" new jobs inserted successfully",
                        "status", "success"
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
    }

    @GetMapping("id/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable String id) {
        Optional<Job> jobById = jobsService.getJobById(id);
        if(jobById.isPresent()){
            return new ResponseEntity<>(jobById.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> getJobsByFilters(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) String jobType) {

        if(minSalary==null){
            minSalary=0;
        }

        minSalary=minSalary*100000;
        System.out.println("request received with title: "+title+", salary: "+minSalary);

        try {
            List<Job> jobs = jobsService.getJobsByFilters(title, minSalary, location, company);
            System.out.println("no of jobs matched for the preference: "+jobs.size());
            return ResponseEntity.ok(jobs);
        } catch (JobNotFoundException e) {
            System.out.println("no jobs found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Something went wrong!"));
        }
    }


    @DeleteMapping("id/{id}")
    public ResponseEntity<Map<String, String>> deleteJobById(@PathVariable String id) {
        boolean isDeleted = jobsService.deleteJobById(id);
        if (isDeleted) {
            Map<String, String> response = Map.of(
                    "message", "Job with ID " + id + " has been deleted successfully",
                    "status", "success"
            );
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            Map<String, String> errorResponse = Map.of(
                    "error", "Job not found",
                    "message", "No job found with ID: " + id
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @DeleteMapping("delete-all")
    public ResponseEntity<Map<String, String>> deleteAllJobs(){
        List<Job> deletedJobs=jobsService.deleteAllJobs();
        if(!deletedJobs.isEmpty()){
            Map<String, String> response = Map.of(
                    "message", deletedJobs.size()+" jobs deleted successfully",
                    "deleted jobs", deletedJobs.toString(),
                    "status", "Success"
            );
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }else {
            Map<String, String> response = Map.of(
                    "message", "Jobs not deleted",
                    "status", "Failed"
            );
            return ResponseEntity.status(HttpStatus.METHOD_FAILURE).body(response);
        }
    }

    @GetMapping("/jobs/description/{id}")
    public ResponseEntity<Job> getJobDescription(@PathVariable String id, @RequestParam String url) {
//        System.out.println("id received: "+id);
//        System.out.println("url received: "+url);
        try {
            Optional<Job> jobById = jobsService.getJobByUrl(url);
            if (jobById.isPresent()) {
                Job job = jobById.get();

//                System.out.println("received request with url(in controller): \n"+url);

                String description;
                if(job.getFullDescription()==null){
                    // Call Flask backend to fetch the description
                    Job job1 = jobsService.fetchJobDescriptionFromFlask(url);
                    if(job1==null ){
                        job.setFullDescription(null);
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }else {
                        if(job1.getFullDescription().length()>100) {
                            String extractedReviews = NumberExtractor.extractNumber(job1.getReviews());
                            if (extractedReviews != null) {
                                job.setReviews(extractedReviews);
                            }

                            String extractedRating = NumberExtractor.extractNumber(job1.getRating());
                            if (extractedRating != null) {
                                job.setRating(extractedRating);
                            }

                            String extractedApplicants = NumberExtractor.extractNumber(job1.getApplicants());
                            if (extractedApplicants != null) {
                                job.setApplicants(extractedApplicants);
                            }

                            String extractedPostDate = NumberExtractor.extractNumber(job1.getPost_date());
                            if (extractedPostDate!=null) {
                                job.setPost_date(extractedPostDate);
                            }

                            job.setFullDescription(job1.getFullDescription());
                            return new ResponseEntity<>(job, HttpStatus.OK);
                        }else
                            job.setFullDescription(null);
                    }

                }else {
                    return new ResponseEntity<>(job,HttpStatus.OK);
                }

            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("job/{id}/update-description")
    public ResponseEntity<Job> updateJobDescription(@PathVariable String id,@RequestBody JobUpdateDTO jobUpdateDTO) {
        Optional<Job> jobOptional = jobsService.getJobByUrl(jobUpdateDTO.getUrl());

        if (jobOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Job job = jobOptional.get();
        job.setFullDescription(jobUpdateDTO.getFullDescription()); // Update only fullDescription
        jobsService.insertJob(job);

        if(job.getFullDescription()!=null)
            return ResponseEntity.ok(job);
        else {
            return new ResponseEntity<>(job,HttpStatus.NOT_IMPLEMENTED);
        }
    }

    private Optional<User> resolveAuthenticatedUser(String authHeader) {
        if (authHeader != null && !authHeader.isBlank() && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String identity = jwtService.extractUserName(token);
                Optional<User> userByToken = userService.getUserByIdentity(identity);
                if (userByToken.isPresent()) {
                    return userByToken;
                }
            } catch (Exception ignored) {
                // Fallback to SecurityContext below.
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity = authentication == null ? null : authentication.getName();
        return userService.getUserByIdentity(identity);
    }
}
