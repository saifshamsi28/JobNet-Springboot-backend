package com.saif.JobNet.services;

import com.saif.JobNet.model.User;
import com.saif.JobNet.repositories.JobsRepository;
import com.saif.JobNet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void insertUser(User user){
        userRepository.insert(user);
    }

    public List<User> getAllUser(){
        return userRepository.findAll();
    }

}
