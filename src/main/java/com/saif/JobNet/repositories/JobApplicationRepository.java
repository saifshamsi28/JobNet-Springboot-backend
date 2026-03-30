package com.saif.JobNet.repositories;

import com.saif.JobNet.model.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    Optional<JobApplication> findByUserIdAndJobId(String userId, String jobId);
    List<JobApplication> findByUserIdOrderByUpdatedAtDesc(String userId);
    List<JobApplication> findByRecruiterIdOrderByUpdatedAtDesc(String recruiterId);
    List<JobApplication> findByJobIdOrderByUpdatedAtDesc(String jobId);
    long countByJobId(String jobId);
    long deleteByJobId(String jobId);
}
