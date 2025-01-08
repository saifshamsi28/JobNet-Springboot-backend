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
}
