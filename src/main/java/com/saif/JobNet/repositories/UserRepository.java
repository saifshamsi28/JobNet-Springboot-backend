package com.saif.JobNet.repositories;

import com.saif.JobNet.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByUserName(String userName);
    boolean existsByUserNameIgnoreCase(String userName);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> getUserByEmail(String email);
    Optional<User> getUserByEmailIgnoreCase(String email);
    Optional<User> getUserByUserName(String userName);
    Optional<User> getUserByUserNameIgnoreCase(String userName);
}
