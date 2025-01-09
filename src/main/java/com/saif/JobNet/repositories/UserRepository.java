package com.saif.JobNet.repositories;

import com.saif.JobNet.model.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

public interface UserRepository extends MongoRepository<User, ObjectId> {
}
