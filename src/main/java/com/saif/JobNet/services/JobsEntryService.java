package com.saif.JobNet.services;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.repositories.JobsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
@Component
public class JobsEntryService {
//    private final String BASE_URL = "https://jobnet-flask-backend.onrender.com";
    @Autowired
    private JobsRepository jobsEntryRepository;

    @Autowired
    private RestTemplate restTemplate;

    public int insertJob(List<Job> jobs){
        List<Job> jobsBeforeInsertedNewJobs = jobsEntryRepository.findAll();
        for(Job job:jobs)
            job.setDate(LocalDateTime.now());
        jobsEntryRepository.saveAll(jobs);
        List<Job> jobsAfterInsertedNewJobs = jobsEntryRepository.findAll();
        if(jobsBeforeInsertedNewJobs.size()!=jobsAfterInsertedNewJobs.size()){
            return  jobsAfterInsertedNewJobs.size()-jobsBeforeInsertedNewJobs.size();
        }else {
            return 0;
        }
    }

    //to get all jobs
    public List<Job> getAllJobs() {
        return jobsEntryRepository.findAll();
    }


    //to get specific job
    public Optional<Job> getJobById(String id){
        return jobsEntryRepository.findById(id);
    }

    public boolean deleteJobById(String id) {
        if (jobsEntryRepository.existsById(id)) { // Check if the job exists
            jobsEntryRepository.deleteById(id);   // Perform the deletion
            return true;                          // Indicate successful deletion
        }
        return false; // Indicate failure if the job does not exist
    }

    public List<Job> deleteAllJobs() {
        // Fetch all jobs
        List<Job> jobs = jobsEntryRepository.findAll();

        // Filter jobs where job ID or URL is null, empty, or invalid
        List<Job> jobsToDelete = jobs.stream()
                .filter(job -> job.getId() == null || job.getId().isEmpty() || job.getUrl() == null || job.getUrl().isEmpty() || !isValidUrl(job.getUrl()))
                .collect(Collectors.toList());

        // Delete the jobs that meet the criteria
        if (!jobsToDelete.isEmpty()) {
            int previousSize=jobsEntryRepository.findAll().size();
            jobsEntryRepository.deleteAll(jobsToDelete);
            int afterSize=jobsEntryRepository.findAll().size();

            return jobsToDelete;
        }

        return new ArrayList<>();
    }

    // You can implement a simple URL validation method if necessary
    private boolean isValidUrl(String url) {
        try {
            new URL(url);  // Attempt to create a URL object
            return true;
        } catch (MalformedURLException e) {
            return false;  // Invalid URL
        }
    }
    public String fetchJobDescriptionFromFlask(String url) {
        try {
            url = url.trim();

            // Construct the Flask backend URL
            String flaskEndpoint = "https://jobnet-flask-backend.onrender.com/url?url=" + url;

            System.out.println("Sending request to Flask: " + flaskEndpoint);

            // Call Flask backend using RestTemplate
            ResponseEntity<Map> response = restTemplate.getForEntity(flaskEndpoint, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    if (responseBody.containsKey("description")) {
                        return (String) responseBody.get("description");
                    } else if (responseBody.containsKey("error")) {
                        return "Error from Flask: " + responseBody.get("error");
                    } else {
                        return "Unexpected response from Flask.";
                    }
                } else {
                    return "Empty response from Flask.";
                }
            } else {
                throw new RuntimeException("Failed to fetch job description from Flask: " +
                        response.getStatusCodeValue());
            }
        } catch (Exception e) {
            // Handle exceptions gracefully
            System.err.println("Error communicating with Flask backend: " + e.getMessage());
            return "Unable to fetch job description due to a backend issue.";
        }
    }

    public List<Job> getJobByTitle(String title) {
        return jobsEntryRepository.getJobByTitle(title);
    }
}
