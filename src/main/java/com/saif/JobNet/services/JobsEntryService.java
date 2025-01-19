package com.saif.JobNet.services;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.repositories.JobsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Component
public class JobsEntryService {
    @Autowired
    private JobsRepository jobsEntryRepository;
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
        //        for(Job job:jobs){
//            if(job!=null){
//                System.out.println(job);
//            }
//        }
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

}
