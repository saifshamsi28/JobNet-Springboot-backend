package com.saif.JobNet.repositories;

import com.saif.JobNet.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
}
