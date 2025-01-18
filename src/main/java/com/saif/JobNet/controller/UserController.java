package com.saif.JobNet.controller;

import com.saif.JobNet.UpdateUserRequest;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.SaveJobsModel;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserLoginCredentials;
import com.saif.JobNet.services.JobsEntryService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JobsEntryService jobsEntryService;

    @GetMapping("all")
    public ResponseEntity<List<User>> getAllUsers(){
        return new ResponseEntity<>(userService.getAllUser(),HttpStatus.OK);
    }

    @GetMapping("id/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id){
        Optional<User> user=userService.getUserById(id);
        if(user.isPresent()){
            return new ResponseEntity<>(user.get(),HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("username/{username}")
    public ResponseEntity<Boolean> checkUserNameAvailable(@PathVariable String username) {
        boolean available = userService.checkUserNameAvailable(username);
        System.out.println("username: " + username + ", available: " + available);
        return ResponseEntity.status(HttpStatus.OK)
                .body(available);
    }

    @PostMapping("email/{email}")
    public ResponseEntity<Boolean> checkEmailAlreadyExists(@PathVariable String email) {
        boolean available = userService.checkEmailAlreadyExists(email);
        System.out.println("email: " + email + ", available: " + available);
        return ResponseEntity.status(HttpStatus.OK)
                .body(available);
    }

    @PostMapping
    public ResponseEntity<?> saveUser(@RequestBody User user){
        System.out.println("we got the user : "+user.getName()+" username: "+user.getUserName());
        userService.saveUser(user);
        return new ResponseEntity<>(user,HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginCredentials credentials) {
        String userNameOrEmail = credentials.getUserNameOrEmail();
        String password = credentials.getPassword();

        System.out.println("username or email: "+credentials.getUserNameOrEmail());
        System.out.println("password: "+credentials.getPassword());
        Optional<User> userOpt = userService.getAllUser().stream()
                .filter(user -> (user.getUserName().equals(userNameOrEmail) || user.getEmail().equals(userNameOrEmail))
                        && user.getPassword().equals(password))
                .findFirst();

        if (userOpt.isPresent()) {
            return new ResponseEntity<>(userOpt.get(), HttpStatus.OK); // Return user details
        } else {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED); // Invalid credentials
        }
    }

    @PutMapping
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserRequest updateRequest) {
        if (updateRequest.getId() == null || updateRequest.getId().isEmpty()) {
            return new ResponseEntity<>("id or email is mandatory to update the details",HttpStatus.BAD_REQUEST); // ID is mandatory
        }

        Optional<User> existingUserOpt = userService.getUserById(updateRequest.getId());
        if (existingUserOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // User not found
        }

        User existingUser = existingUserOpt.get();

        // Update only the provided fields
        if (updateRequest.getName() != null) {
            existingUser.setName(updateRequest.getName());
        }
        if (updateRequest.getUserName() != null) {
            existingUser.setUserName(updateRequest.getUserName());
        }
        if (updateRequest.getEmail() != null) {
            existingUser.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPassword() != null) {
            existingUser.setPassword(updateRequest.getPassword());
        }
        if (updateRequest.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        userService.saveUser(existingUser);
        return new ResponseEntity<>(existingUser, HttpStatus.OK);
    }


    @DeleteMapping("id/{id}")
    public ResponseEntity<Map<String, Object>> deleteUserById(@PathVariable String id) {
        Optional<User> userBox = userService.getUserById(id);

        if (userBox.isPresent()) {
            User user = userBox.get();
            userService.deleteUserById(id);
            Optional<User> isDeleted = userService.getUserById(id);

            if (isDeleted.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Failed to delete user");
                response.put("user", user);
                return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User deleted successfully");
                response.put("user", user);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No user is present with id: " + id);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("all")
    public ResponseEntity<?> deleteAllUsers(){
        userService.deleteAllUsers();
        if(userService.getAllUser().isEmpty()){
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }

    @PutMapping("/save-jobs")
    public ResponseEntity<?> saveJobForUser(@RequestBody SaveJobsModel saveJobsModel) {
        System.out.println("Saving job ID: " + saveJobsModel.getJobId() + " to user ID: " + saveJobsModel.getUserId());


        Optional<User> user = userService.getUserById(saveJobsModel.getUserId());
        Optional<Job> job = jobsEntryService.getJobById(saveJobsModel.getJobId());

        if (user.isPresent() && job.isPresent()) {
            user.get().addJobToUser(job.get());
            userService.saveUser(user.get());
            System.out.println("Saved job: " + job.get().getTitle() + " to user: " + user.get().getName());
            return new ResponseEntity<>(user.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User or job not found", HttpStatus.NOT_FOUND);
        }
    }

}
