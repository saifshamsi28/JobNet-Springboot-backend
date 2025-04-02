package com.saif.JobNet.services;

import com.saif.JobNet.model.User;
import com.saif.JobNet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void saveUser(User user) {
        userRepository.save(user); // Using save instead of `insert` for both insert and update
    }

    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id){
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUserName(String userName){
        return userRepository.getUserByUserName(userName);
    }

    public Optional<User> getUserByEmail(String email){
        return userRepository.getUserByEmail(email);
    }


    public void deleteUserById(String id){
        userRepository.deleteById(id);
    }

    public boolean checkUserNameAvailable(String username){
        return userRepository.existsByUserName(username);
    }

    public boolean checkEmailAlreadyExists(String email){
        return userRepository.existsByEmail(email);
    }

}
