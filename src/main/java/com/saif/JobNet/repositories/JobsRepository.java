package com.saif.JobNet.repositories;

import com.saif.JobNet.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobsRepository extends MongoRepository<Job,String> {

}
