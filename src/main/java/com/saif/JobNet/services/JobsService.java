package com.saif.JobNet.services;

import com.saif.JobNet.NumberExtractor;
import com.saif.JobNet.exception_handling.JobNotFoundException;
import com.saif.JobNet.model.EmploymentType;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.JobStatus;
import com.saif.JobNet.model.RecruiterJobCreateRequest;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.WorkMode;
import com.saif.JobNet.repositories.JobApplicationRepository;
import com.saif.JobNet.repositories.JobsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    private final MongoTemplate mongoTemplate;

    @Value("${app.flask.base-url:http://127.0.0.1:5000}")
    private String flaskBaseUrl;

    private final List<String> jobRoles = List.of(
            "Software Engineer", "Data Analyst","Web Developer",
            "Backend Developer", "Frontend Developer", "Android Developer",
            "Machine Learning Engineer","Data Scientist","Java Developer"
    );

    public JobsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void cleanupLegacyIndexes() {
        try {
            mongoTemplate.getCollection("jobs").dropIndex("uniq_id");
            System.out.println("Dropped legacy jobs index: uniq_id");
        } catch (Exception ignored) {
            // Index may not exist in fresh databases; ignore safely.
        }
    }

    private String normalizedFlaskBaseUrl() {
        String base = flaskBaseUrl == null ? "" : flaskBaseUrl.trim();
        if (base.isEmpty()) {
            base = "http://127.0.0.1:5000";
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://" + base;
        }
        return base;
    }

    private URI buildFlaskUri(String path, Map<String, Object> queryParams) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(normalizedFlaskBaseUrl())
                    .path(path);

            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }

            URI uri = builder.build(true).toUri();
            if (!uri.isAbsolute()) {
                throw new IllegalArgumentException("Flask URI is not absolute: " + uri);
            }
            return uri;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid Flask base URL: " + flaskBaseUrl, e);
        }
    }

    public int insertAllJob(List<Job> jobs){
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        List<Job> jobsBeforeInsertedNewJobs = jobsRepository.findAll();
        for(Job job:jobs) {
            ensureJobId(job);
            job.setDateTime(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            int[] salaryRange = parseSalary(job.getSalary());
            job.setMinSalary(salaryRange[0]);
            job.setMaxSalary(salaryRange[1]);
            if (job.getStatus() == null) {
                job.setStatus(JobStatus.PUBLISHED);
            }
            if (job.getSource() == null || job.getSource().isBlank()) {
                job.setSource("aggregated");
            }
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
            if (job.getShortDescription() == null && job.getFullDescription() != null) {
                String plain = job.getFullDescription().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                if (!plain.isBlank()) {
                    job.setShortDescription(plain.length() > 500 ? plain.substring(0, 500) : plain);
                }
            }
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
        ensureJobId(jobs);
        jobs.setUpdatedAt(LocalDateTime.now());
        if (jobs.getDateTime() == null) {
            jobs.setDateTime(LocalDateTime.now());
        }
        if (jobs.getStatus() == null) {
            jobs.setStatus(JobStatus.PUBLISHED);
        }
        if (jobs.getSource() == null || jobs.getSource().isBlank()) {
            jobs.setSource("aggregated");
        }
        jobsRepository.save(jobs);
    }

    //to get all jobs
    public List<Job> getAllJobs() {
        return jobsRepository.findByStatusOrderByDateTimeDesc(JobStatus.PUBLISHED);
    }


    //to get specific job
    public Optional<Job> getJobById(String id){
        return jobsRepository.findById(id);
    }

    public Optional<Job> getJobByUrl(String url){
        return Optional.ofNullable(jobsRepository.findByUrl(url));
    }

    public List<Job> getJobsPostedByRecruiter(String recruiterUserId) {
        if (recruiterUserId == null || recruiterUserId.isBlank()) {
            return List.of();
        }
        List<Job> jobs = jobsRepository.findByPostedByUserIdOrderByDateTimeDesc(recruiterUserId);
        for (Job job : jobs) {
            if (job == null || isBlank(job.getId())) {
                continue;
            }
            long applicantsCount = jobApplicationRepository.countByJobId(job.getId());
            job.setApplicants(String.valueOf(Math.max(0, applicantsCount)));
        }
        return jobs;
    }

    public Optional<Job> createRecruiterJob(User recruiter, RecruiterJobCreateRequest request) {
        if (recruiter == null || request == null) {
            return Optional.empty();
        }
        JobStatus requestedStatus = parseRequestedStatus(request.getStatus());
        boolean isDraft = requestedStatus == JobStatus.DRAFT;

        if (!isDraft && (isBlank(request.getTitle()) || isBlank(request.getCompany()) || isBlank(request.getLocation()))) {
            return Optional.empty();
        }

        Job job = new Job();
        ensureJobId(job);
        job.setTitle(defaultIfBlank(request.getTitle(), isDraft ? "Untitled Draft" : null));
        job.setCompany(defaultIfBlank(request.getCompany(), isDraft ? "Draft Company" : null));
        job.setLocation(defaultIfBlank(request.getLocation(), isDraft ? "TBD" : null));
        job.setSalary(defaultIfBlank(request.getSalary(), "Not disclosed"));
        job.setOpenings(defaultIfBlank(request.getOpenings(), "1"));
        String computedCategory = defaultIfBlank(request.getCategory(), inferCategory(job.getTitle(), request.getShortDescription(), request.getFullDescription()));
        job.setCategory(defaultIfBlank(computedCategory, "General"));
        job.setRequiredSkills(sanitizeSkills(request.getRequiredSkills()));
        EmploymentType employmentType = parseEmploymentType(request);
        WorkMode workMode = parseWorkMode(request.getWorkMode());
        job.setEmploymentType(employmentType);
        job.setWorkMode(workMode);
        job.setLocation(normalizeLocationForWorkMode(job.getLocation(), workMode, isDraft));
        job.setShortDescription(defaultIfBlank(request.getShortDescription(), isDraft ? "Draft in progress" : "Description not provided"));
        job.setFullDescription(defaultIfBlank(request.getFullDescription(), request.getShortDescription()));
        job.setUrl(defaultIfBlank(request.getUrl(), null));
        job.setSource("recruiter");
        job.setPostedByUserId(recruiter.getId());
        job.setStatus(requestedStatus);
        job.setPost_date("0d");
        job.setDateTime(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        int[] salaryRange = parseSalary(job.getSalary());
        job.setMinSalary(salaryRange[0]);
        job.setMaxSalary(salaryRange[1]);

        return Optional.of(jobsRepository.save(job));
    }

    public Optional<Job> updateRecruiterJob(User recruiter, String jobId, RecruiterJobCreateRequest request) {
        if (recruiter == null || isBlank(jobId) || request == null) {
            return Optional.empty();
        }

        Optional<Job> jobBox = jobsRepository.findByIdAndPostedByUserId(jobId, recruiter.getId());
        if (jobBox.isEmpty()) {
            return Optional.empty();
        }

        Job job = jobBox.get();
        JobStatus requestedStatus = parseRequestedStatus(request.getStatus());
        boolean willBeDraft = requestedStatus == JobStatus.DRAFT;

        String title = defaultIfBlank(request.getTitle(), job.getTitle());
        String company = defaultIfBlank(request.getCompany(), job.getCompany());
        String location = defaultIfBlank(request.getLocation(), job.getLocation());
        if (!willBeDraft && (isBlank(title) || isBlank(company) || isBlank(location))) {
            return Optional.empty();
        }

        job.setTitle(defaultIfBlank(title, willBeDraft ? "Untitled Draft" : job.getTitle()));
        job.setCompany(defaultIfBlank(company, willBeDraft ? "Draft Company" : job.getCompany()));
        job.setLocation(defaultIfBlank(location, willBeDraft ? "TBD" : job.getLocation()));
        job.setSalary(defaultIfBlank(request.getSalary(), job.getSalary()));
        job.setOpenings(defaultIfBlank(request.getOpenings(), job.getOpenings()));
        String fallbackCategory = defaultIfBlank(job.getCategory(), inferCategory(title, request.getShortDescription(), request.getFullDescription()));
        job.setCategory(defaultIfBlank(request.getCategory(), fallbackCategory));
        if (request.getRequiredSkills() != null) {
            job.setRequiredSkills(sanitizeSkills(request.getRequiredSkills()));
        } else if (job.getRequiredSkills() == null) {
            job.setRequiredSkills(new ArrayList<>());
        }
        EmploymentType employmentType = parseEmploymentType(request);
        WorkMode workMode = parseWorkMode(request.getWorkMode());
        job.setEmploymentType(employmentType);
        job.setWorkMode(workMode);
        job.setShortDescription(defaultIfBlank(request.getShortDescription(), job.getShortDescription()));
        job.setFullDescription(defaultIfBlank(request.getFullDescription(), defaultIfBlank(request.getShortDescription(), job.getFullDescription())));
        job.setUrl(defaultIfBlank(request.getUrl(), job.getUrl()));
        job.setLocation(normalizeLocationForWorkMode(defaultIfBlank(request.getLocation(), job.getLocation()), workMode, willBeDraft));
        job.setStatus(requestedStatus);
        job.setUpdatedAt(LocalDateTime.now());

        int[] salaryRange = parseSalary(job.getSalary());
        job.setMinSalary(salaryRange[0]);
        job.setMaxSalary(salaryRange[1]);

        return Optional.of(jobsRepository.save(job));
    }

    public Optional<Job> updateRecruiterJobStatus(User recruiter, String jobId, JobStatus status) {
        if (recruiter == null || isBlank(jobId) || status == null) {
            return Optional.empty();
        }
        Optional<Job> jobBox = jobsRepository.findByIdAndPostedByUserId(jobId, recruiter.getId());
        if (jobBox.isEmpty()) {
            return Optional.empty();
        }

        Job job = jobBox.get();
        job.setStatus(status);
        job.setUpdatedAt(LocalDateTime.now());
        return Optional.of(jobsRepository.save(job));
    }

    public boolean deleteRecruiterJob(User recruiter, String jobId) {
        if (recruiter == null || isBlank(jobId)) {
            return false;
        }

        Optional<Job> jobBox = jobsRepository.findByIdAndPostedByUserId(jobId, recruiter.getId());
        if (jobBox.isEmpty()) {
            return false;
        }

        jobApplicationRepository.deleteByJobId(jobId);
        jobsRepository.deleteById(jobId);
        return true;
    }

    private void ensureJobId(Job job) {
        if (job == null) {
            return;
        }
        if (job.getId() == null || job.getId().isBlank()) {
            job.setId(UUID.randomUUID().toString());
        }
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

            URI flaskEndpoint = buildFlaskUri("/url", Map.of("url", url));
            System.out.println("Sending request to Flask: " + flaskEndpoint);
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
        query.addCriteria(Criteria.where("status").is(JobStatus.PUBLISHED));

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

        if (jobs.isEmpty() && title != null && !title.isEmpty()) {
            // 👇 Call Flask Scraper
            jobs = fetchJobsFromFlask(title);

            if (jobs != null && !jobs.isEmpty()) {
                // 👇 Save scraped jobs into MongoDB
                mongoTemplate.insertAll(jobs);
                return jobs;
            } else {
                throw new JobNotFoundException("No jobs found even after scraping.");
            }
        }

        if (jobs.isEmpty()) {
            throw new JobNotFoundException("No jobs found for the given preferences.");
        }

        return jobs;
    }

    public List<Job> fetchJobsFromFlask(String jobTitle) {
        try {
            URI jobsUri = buildFlaskUri("/jobs", Map.of(
                    "title", jobTitle,
                    "page", 1,
                    "size", 100
            ));
            System.out.println("Fetching jobs from Flask: " + jobsUri);

            ResponseEntity<Map> response = restTemplate.getForEntity(jobsUri, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object itemsObject = response.getBody().get("items");
                if (itemsObject instanceof List<?> rawItems) {
                    List<Job> jobs = new ArrayList<>();
                    for (Object item : rawItems) {
                        Job job = objectMapper.convertValue(item, Job.class);
                        if (job.getDateTime() == null) {
                            job.setDateTime(LocalDateTime.now());
                        }
                        jobs.add(job);
                    }
                    return jobs;
                }
            } else {
                System.err.println("Flask returned non-OK status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error fetching jobs from Flask: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    // Function to parse salary string into min and max salary
    private int[] parseSalary(String salary) {
        if (salary == null || salary.isBlank()) {
            return new int[]{0, 0};
        }

        Pattern pattern = Pattern.compile("(\\d+)-(\\d+)");
        Matcher matcher = pattern.matcher(salary);

        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1)) * 100000; // Convert to integer in Lacs
            int max = Integer.parseInt(matcher.group(2)) * 100000; // Convert to integer in Lacs
            return new int[]{min, max};
        }

        return new int[]{0, 0}; // Default if parsing fails
    }

    private List<String> sanitizeSkills(List<String> requestedSkills) {
        List<String> sanitized = new ArrayList<>();
        if (requestedSkills == null) {
            return sanitized;
        }
        for (String skill : requestedSkills) {
            if (isBlank(skill)) {
                continue;
            }
            String clean = skill.trim();
            if (clean.length() > 40) {
                clean = clean.substring(0, 40).trim();
            }
            if (clean.isEmpty() || sanitized.contains(clean)) {
                continue;
            }
            sanitized.add(clean);
            if (sanitized.size() == 20) {
                break;
            }
        }
        return sanitized;
    }

    private String inferCategory(String title, String shortDescription, String fullDescription) {
        String haystack = String.join(" ",
                defaultIfBlank(title, ""),
                defaultIfBlank(shortDescription, ""),
                defaultIfBlank(fullDescription, "")
        ).toLowerCase(Locale.ROOT);

        if (containsAny(haystack, "design", "ui", "ux", "figma", "visual")) return "Design";
        if (containsAny(haystack, "engineer", "developer", "software", "android", "ios", "frontend", "backend", "devops")) return "Engineering";
        if (containsAny(haystack, "marketing", "seo", "campaign", "brand", "content", "growth")) return "Marketing";
        if (containsAny(haystack, "finance", "account", "audit", "investment", "analyst")) return "Finance";
        if (containsAny(haystack, "hr", "human resources", "recruit", "talent", "people")) return "HR";
        if (containsAny(haystack, "teacher", "education", "trainer", "instructor", "curriculum")) return "Education";
        return "General";
    }

    private boolean containsAny(String text, String... keys) {
        if (text == null || text.isBlank() || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank() && text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private JobStatus parseRequestedStatus(String rawStatus) {
        if (isBlank(rawStatus)) {
            return JobStatus.PUBLISHED;
        }
        try {
            return JobStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return JobStatus.PUBLISHED;
        }
    }

    private EmploymentType parseEmploymentType(RecruiterJobCreateRequest request) {
        if (request == null) {
            return EmploymentType.FULL_TIME;
        }
        String raw = isBlank(request.getEmploymentType()) ? request.getJobType() : request.getEmploymentType();
        return EmploymentType.from(raw);
    }

    private WorkMode parseWorkMode(String rawWorkMode) {
        return WorkMode.from(rawWorkMode);
    }

    private String normalizeLocationForWorkMode(String candidateLocation, WorkMode workMode, boolean isDraft) {
        if (workMode == WorkMode.REMOTE) {
            return "Remote (Anywhere)";
        }
        if (!isBlank(candidateLocation)) {
            return candidateLocation.trim();
        }
        return isDraft ? "TBD" : "On-site";
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

//    @Scheduled(fixedRate = 3600000) // in milliseconds
//    public void fetchJobsForMultipleRoles() {
////        System.out.println("Running job scheduler for multiple roles...");
//
//        for (String role : jobRoles) {
//            try {
//                URI scrapeUri = buildFlaskUri("/scrape", null);
//                Map<String, Object> scrapePayload = new HashMap<>();
//                scrapePayload.put("title", role);
//                scrapePayload.put("pages", 2);
//
//                ResponseEntity<Map> response = restTemplate.postForEntity(scrapeUri, scrapePayload, Map.class);
//                if (!(response.getStatusCode().is2xxSuccessful())) {
//                    System.err.println("[" + role + "] Failed to enqueue scrape. Status: " + response.getStatusCode());
//                }
//
//            } catch (Exception e) {
//                System.err.println("[" + role + "] Error: " + e.getMessage());
//            }
//        }
//
////        System.out.println("Scheduler cycle complete.");
//    }

    public List<Job> getNewJobs() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(6);
        return jobsRepository.findByStatusAndDateTimeAfterOrderByDateTimeDesc(JobStatus.PUBLISHED, yesterday);
    }
}
