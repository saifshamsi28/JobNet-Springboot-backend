package com.saif.JobNet.controller;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.services.JobsEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @GetMapping("{url}")
    public ResponseEntity<?> getFullJobDescription(@PathVariable String url){
        String fullDescription=jobsEntryService.fetchJobDescriptionFromApi(url);
        if(fullDescription.isEmpty()){
            return new ResponseEntity<>(fullDescription,HttpStatus.OK);
        }else {
            return new ResponseEntity<>("No description found",HttpStatus.NOT_FOUND);
        }
    }
}
