package com.saif.JobNet.services;

import com.saif.JobNet.NumberExtractor;
import com.saif.JobNet.exception_handling.JobNotFoundException;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.repositories.JobsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
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

    private String flaskUrl=System.getenv("FLASK_HOSTED_URL");

    private final List<String> jobRoles = List.of(
            "Software Engineer", "Data Analyst","Web Developer",
            "Backend Developer", "Frontend Developer", "Android Developer",
            "Machine Learning Engineer","Data Scientist","Java Developer"
    );

    public JobsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public int insertAllJob(List<Job> jobs){
        List<Job> jobsBeforeInsertedNewJobs = jobsRepository.findAll();
        for(Job job:jobs) {
            job.setDateTime(LocalDateTime.now());
            int[] salaryRange = parseSalary(job.getSalary());
            job.setMinSalary(salaryRange[0]);
            job.setMaxSalary(salaryRange[1]);
            String extractedReviews = NumberExtractor.extractNumber(job.getReviews());
            if (extractedReviews != null) {
                job.setReviews(extractedReviews);
            }

            String extractedRating = NumberExtractor.extractNumber(job.getRating());
            if (extractedRating != null) {
                job.setRating(extractedRating);
            }

            String extractedApplicants = NumberExtractor.extractNumber(job.getApplicants());
            if (extractedApplicants != null) {
                job.setApplicants(extractedApplicants);
            }

            String extractedPostDate = NumberExtractor.extractNumber(job.getPost_date());
            if (extractedPostDate!=null) {
                job.setPost_date(extractedPostDate);
            }
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
    public Job fetchJobDescriptionFromFlask(String url) {
        try {
            url = url.trim();

            // Construct the Flask backend URL
            String flaskEndpoint = "https://jobnet-flask-backend.onrender.com/url?url=" + url;
//            String flaskEndpoint="http://10.162.1.53:5000/url?url="+url;

            System.out.println("Sending request to Flask: " + flaskEndpoint);

            // Call Flask backend using RestTemplate
//            ResponseEntity<Map> response = restTemplate.getForEntity(flaskEndpoint, Map.class);
            ResponseEntity<Job> response = restTemplate.getForEntity(flaskEndpoint, Job.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Job job = response.getBody();
                if (job != null) {
                    return job;
                }
            } else {
                throw new RuntimeException("Failed to fetch job description from Flask: " +
                        response.getStatusCode());
            }
        } catch (Exception e) {
            // Handle exceptions gracefully
            System.err.println("Error communicating with Flask backend: " + e.getMessage());
            return null;
        }
        return null;
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

    @Scheduled(fixedRate = 3600000) // in milliseconds
    public void fetchJobsForMultipleRoles() {
//        System.out.println("Running job scheduler for multiple roles...");

        for (String role : jobRoles) {
            try {
                String encodedRole = URLEncoder.encode(role, StandardCharsets.UTF_8);
                String fullUrl = flaskUrl + "?job_title=" + encodedRole;

                ResponseEntity<Job[]> response = restTemplate.getForEntity(fullUrl, Job[].class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    List<Job> fetchedJobs = Arrays.asList(response.getBody());
                    int newCount = 0;

                    for (Job job : fetchedJobs) {
                        if (job.getUrl() != null && jobsRepository.findByUrl(job.getUrl()) == null) {
                                job.setDateTime(LocalDateTime.now());
                                int[] salaryRange = parseSalary(job.getSalary());
                                job.setMinSalary(salaryRange[0]);
                                job.setMaxSalary(salaryRange[1]);
                            String extractedReviews = NumberExtractor.extractNumber(job.getReviews());
                            if (extractedReviews != null) {
                                job.setReviews(extractedReviews);
                            }

                            String extractedRating = NumberExtractor.extractNumber(job.getRating());
                            if (extractedRating != null) {
                                job.setRating(extractedRating);
                            }

                            String extractedApplicants = NumberExtractor.extractNumber(job.getApplicants());
                            if (extractedApplicants != null) {
                                job.setApplicants(extractedApplicants);
                            }

                            String extractedPostDate = NumberExtractor.extractNumber(job.getPost_date());
                            if (extractedPostDate!=null) {
                                job.setPost_date(extractedPostDate);
                            }
                            job.setFullDescription(null);
                            jobsRepository.save(job);
                            newCount++;
                        }
                    }

//                    System.out.println("[" + role + "] Added " + newCount + " new jobs.");
                } else {
                    System.err.println("[" + role + "] Failed to fetch jobs. Status: " + response.getStatusCode());
                }

            } catch (Exception e) {
                System.err.println("[" + role + "] Error: " + e.getMessage());
            }
        }

//        System.out.println("Scheduler cycle complete.");
    }

    public List<Job> getNewJobs() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(6);
        return jobsRepository.findByDateTimeAfter(yesterday);
    }
}
