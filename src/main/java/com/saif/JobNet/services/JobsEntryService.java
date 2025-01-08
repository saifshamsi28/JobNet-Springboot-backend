package com.saif.JobNet.services;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.repositories.JobsEntryRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Component
public class JobsEntryService {
    @Autowired
    private JobsEntryRepository jobsEntryRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Job upsertJobByTitle(Job job) {
        Query query = new Query(Criteria.where("title").is(job.getTitle())); // Match by title
        Update update = new Update()
                .set("company", job.getCompany())
                .set("location", job.getLocation())
                .set("salary", job.getSalary())
                .set("url", job.getUrl())
                .set("rating", job.getRating())
                .set("reviews", job.getReviews())
                .set("post_date", job.getPostDate())
                .set("description", job.getDescription())
                .set("date", LocalDateTime.now()); // Update date
        return mongoTemplate.findAndModify(query, update, Job.class, "jobs");
    }

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
        List<Job> jobs = jobsEntryRepository.findAll();
        for(Job job:jobs){
            if(job!=null){
                System.out.println(job);
            }
        }
        return jobs;
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

    public boolean deleteAllJobs(){
        jobsEntryRepository.deleteAll();
        List<Job> jobs = jobsEntryRepository.findAll();
        return jobs.isEmpty();
    }

    public void reindexJobsByTitle() {
        // Fetch all jobs
        List<Job> allJobs = jobsEntryRepository.findAll();

        // Group jobs by title
        allJobs.stream()
                .collect(Collectors.groupingBy(Job::getTitle))
                .forEach((title, jobsWithSameTitle) -> {
                    // Keep the first job as the primary entry and update others with reference
                    Job primaryJob = jobsWithSameTitle.getFirst();

                    // Create or update primary job
                    Query query = new Query(Criteria.where("title").is(title));
                    Update update = new Update()
                            .set("company", primaryJob.getCompany())
                            .set("location", primaryJob.getLocation())
                            .set("salary", primaryJob.getSalary())
                            .set("url", primaryJob.getUrl())
                            .set("rating", primaryJob.getRating())
                            .set("reviews", primaryJob.getReviews())
                            .set("post_date", primaryJob.getPostDate())
                            .set("description", primaryJob.getDescription())
                            .set("date", primaryJob.getDate())
                            .set("url", primaryJob.getUrl());
                    mongoTemplate.upsert(query, update, Job.class);

                    // Optional: Log duplicates or handle them as needed
                    if (jobsWithSameTitle.size() > 1) {
                        System.out.println("Duplicates found for title: " + title);
                    }
                });
        mongoTemplate.getCollection("jobs").createIndex(new Document("title", 1));

        System.out.println("Reindexing completed.");
    }
}
