package com.saif.JobNet.repositories;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.JobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface JobsRepository extends MongoRepository<Job, String> {
    Job findByUrl(String url);
    // Use the actual field name
    List<Job> findByDateTimeAfter(LocalDateTime dateTime);
    List<Job> findByStatusOrderByDateTimeDesc(JobStatus status);
    List<Job> findByStatusAndDateTimeAfterOrderByDateTimeDesc(JobStatus status, LocalDateTime dateTime);
    List<Job> findByPostedByUserIdOrderByDateTimeDesc(String postedByUserId);
    Optional<Job> findByIdAndPostedByUserId(String id, String postedByUserId);

    @Query("{ '$or': [ " +
            "{ 'description': { $regex: ?0, $options: 'i' } }, " +
            "{ 'full_description': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Job> findJobsBySkillKeyword(String keyword);




}


