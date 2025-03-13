package com.saif.JobNet.services;

import com.saif.JobNet.model.User;
import com.saif.JobNet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
//        System.out.println("in user service getting user by id: "+id);
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
        return userRepository.existsByUserName(username);
    }

    public boolean checkEmailAlreadyExists(String email){
        return userRepository.existsByEmail(email);
    }

    public String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");

        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);  // Get file extension
            originalFileName = originalFileName.substring(0, dotIndex);  // Remove extension
        }

        String timestamp = String.valueOf(System.currentTimeMillis());  // Unique timestamp
        return originalFileName + "_" + timestamp + extension;  // New unique filename
    }

    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}
