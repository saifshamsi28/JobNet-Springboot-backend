package com.saif.JobNet.repositories;

import com.saif.JobNet.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface JobsRepository extends MongoRepository<Job, String> {
    Job findByUrl(String url);
    // Use the actual field name
    List<Job> findByDateTimeAfter(LocalDateTime dateTime);


}


