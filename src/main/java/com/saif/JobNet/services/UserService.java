package com.saif.JobNet.services;

import com.saif.JobNet.model.User;
import com.saif.JobNet.repositories.JobsRepository;
import com.saif.JobNet.repositories.UserRepository;
import org.bson.types.ObjectId;
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

    public void saveUser(User user){
        userRepository.insert(user);
    }

    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    public User getUserById(ObjectId id){
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()){
            return user.get();
        }
        return null;
    }

    public void deleteUserById(ObjectId id){
        userRepository.deleteById(id);
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
}
