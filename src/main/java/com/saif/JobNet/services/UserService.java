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
    public void deleteUserById(String id){
        userRepository.deleteById(id);
    }

    public void deleteAllUsers(){
        userRepository.deleteAll();
    }
    public boolean checkUpdatedOrNot(User userBefore, User userAfter) {
        if(userBefore.getEmail().equals(userAfter.getEmail())
        && userBefore.getPassword().equals(userAfter.getPassword())
        && userBefore.getName().equals(userAfter.getName())
        && userBefore.getPhoneNumber().equals(userAfter.getPhoneNumber())){
            return false;
        }
        return true;
    }

    public boolean checkUserNameAvailable(String username){
        return !userRepository.existsByUserName(username); //returning true if username not exist
    }
}
