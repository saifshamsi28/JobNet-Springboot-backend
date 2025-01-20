package com.saif.JobNet.controller;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.services.JobsEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/home")
public class JobsController {
    @Autowired
    JobsEntryService jobsEntryService;

    @GetMapping
    public List<Job> getAllJobs(){
//        jobsEntryService.
        return jobsEntryService.getAllJobs();
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> insertJob(@RequestBody List<Job> jobs){
            int noOfInsertedJobs=jobsEntryService.insertJob(jobs);
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
        Optional<Job> jobById = jobsEntryService.getJobById(id);
        if(jobById.isPresent()){
            return new ResponseEntity<>(jobById.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("id/{id}")
    public ResponseEntity<Map<String, String>> deleteJobById(@PathVariable String id) {
        boolean isDeleted = jobsEntryService.deleteJobById(id);
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
        List<Job> deletedJobs=jobsEntryService.deleteAllJobs();
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
        try {
            Optional<Job> jobById = jobsEntryService.getJobById(id);
            if (jobById.isPresent()) {
                Job job = jobById.get();

                System.out.println("received request with url(in controller): \n"+url);

                // Call Flask backend to fetch the description
                String description = jobsEntryService.fetchJobDescriptionFromFlask(url);
                System.out.println("desc in controller: \n"+description);

                // Update the job description
                job.setDescription(description);

//                Map<String, String> response = new HashMap<>();
//                response.put("description", description);
                return new ResponseEntity<>(job,HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
