package com.saif.JobNet.services;

import com.saif.JobNet.exception_handling.JobNotFoundException;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.repositories.JobsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Service
@Component
public class JobsService {
    @Autowired
    private JobsRepository jobsRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final MongoTemplate mongoTemplate;

    public JobsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public int insertAllJob(List<Job> jobs){
        List<Job> jobsBeforeInsertedNewJobs = jobsRepository.findAll();
        for(Job job:jobs) {
            job.setDate(LocalDateTime.now());
            int[] salaryRange = parseSalary(job.getSalary());
            job.setMinSalary(salaryRange[0]);
            job.setMaxSalary(salaryRange[1]);
            job.setFullDescription(null);
        }
        jobsRepository.saveAll(jobs);
        List<Job> jobsAfterInsertedNewJobs = jobsRepository.findAll();
        if(jobsBeforeInsertedNewJobs.size()!=jobsAfterInsertedNewJobs.size()){
            return  jobsAfterInsertedNewJobs.size()-jobsBeforeInsertedNewJobs.size();
        }else {
            return 0;
        }
    }

    public void insertJob(Job jobs){
        jobsRepository.save(jobs);
    }

    //to get all jobs
    public List<Job> getAllJobs() {
        return jobsRepository.findAll();
    }


    //to get specific job
    public Optional<Job> getJobById(String id){
        return jobsRepository.findById(id);
    }

    public Optional<Job> getJobByUrl(String url){
        return Optional.ofNullable(jobsRepository.findByUrl(url));
    }

    public boolean deleteJobById(String id) {
        if (jobsRepository.existsById(id)) { // Check if the job exists
            jobsRepository.deleteById(id);   // Perform the deletion
            return true;                          // Indicate successful deletion
        }
        return false; // Indicate failure if the job does not exist
    }

    public List<Job> deleteAllJobs() {
        // Fetch all jobs
        List<Job> jobs = jobsRepository.findAll();

        // Filter jobs where job ID or URL is null, empty, or invalid
        List<Job> jobsToDelete = jobs.stream()
                .filter(job -> job.getId() == null || job.getId().isEmpty() || job.getUrl() == null || job.getUrl().isEmpty() || !isValidUrl(job.getUrl()))
                .collect(Collectors.toList());

        // Delete the jobs that meet the criteria
        if (!jobsToDelete.isEmpty()) {
            int previousSize= jobsRepository.findAll().size();
            jobsRepository.deleteAll(jobsToDelete);
            int afterSize= jobsRepository.findAll().size();

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
                    if (responseBody.containsKey("full_description")) {
                        return (String) responseBody.get("full_description");
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
                        response.getStatusCode());
            }
        } catch (Exception e) {
            // Handle exceptions gracefully
            System.err.println("Error communicating with Flask backend: " + e.getMessage());
            return "Unable to fetch job description due to a backend issue.";
        }
    }

    public List<Job> getJobsByFilters(String title, Integer minSalary, String location, String company) {
        Query query = new Query();

        if (title != null && !title.isEmpty()) {
            // Search title in both title and description fields
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(title, "i"),
                    Criteria.where("description").regex(title, "i")
            ));
        }
        if (location != null && !location.isEmpty()) {
            query.addCriteria(Criteria.where("location").regex(location, "i"));
        }
        if (company != null && !company.isEmpty()) {
            query.addCriteria(Criteria.where("company").regex(company, "i"));
        }
        if (minSalary != null && minSalary > 0) {
            query.addCriteria(Criteria.where("minSalary").gte(minSalary));
        }

        List<Job> jobs = mongoTemplate.find(query, Job.class);

        if (jobs.isEmpty()) {
            throw new JobNotFoundException("No jobs found for the given preferences.");
        }

        return jobs;
    }

    // Function to parse salary string into min and max salary
    private int[] parseSalary(String salary) {
        Pattern pattern = Pattern.compile("(\\d+)-(\\d+)");
        Matcher matcher = pattern.matcher(salary);

        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1)) * 100000; // Convert to integer in Lacs
            int max = Integer.parseInt(matcher.group(2)) * 100000; // Convert to integer in Lacs
            return new int[]{min, max};
        }

        return new int[]{0, 0}; // Default if parsing fails
    }
}
