package com.saif.JobNet.controller;

import com.saif.JobNet.model.*;
import com.saif.JobNet.services.JobsService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JobsService jobsEntryService;

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

    @GetMapping("email")
    public ResponseEntity<Boolean> checkEmailAvailable(@RequestParam("email") String email) {
        boolean available = !userService.checkEmailAlreadyExists(email);
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

    @PutMapping("update")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        if (userUpdateDTO.getId() == null || userUpdateDTO.getId().isEmpty()) {
            return new ResponseEntity<>(new AuthResponse("id or email is mandatory to update the details",HttpStatus.BAD_REQUEST.value()),HttpStatus.BAD_REQUEST); // ID is mandatory
        }
        System.out.println("got details to update: "+userUpdateDTO);

        Optional<User> existingUserOpt = userService.getUserById(userUpdateDTO.getId());
        if (existingUserOpt.isEmpty()) {
            return new ResponseEntity<>(new AuthResponse("User not found",HttpStatus.NOT_FOUND.value()),HttpStatus.NOT_FOUND); // User not found
        }

        User existingUser = existingUserOpt.get();

        // Update only the provided fields
        if (userUpdateDTO.getName() != null) {
            existingUser.setName(userUpdateDTO.getName());
        }
        if (userUpdateDTO.getEmail() != null) {
            existingUser.setEmail(userUpdateDTO.getEmail());
        }
        if (userUpdateDTO.getPassword() != null) {
            existingUser.setPassword(userUpdateDTO.getPassword());
        }
        if (userUpdateDTO.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(userUpdateDTO.getPhoneNumber());
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

    @PutMapping("/save-jobs")
    public ResponseEntity<?> saveJobForUser(@RequestBody SaveJobsModel saveJobsModel) {

        Optional<User> userOptional = userService.getUserById(saveJobsModel.getUserId());
        Optional<Job> jobOptional = jobsEntryService.getJobById(saveJobsModel.getJobId());

        if (userOptional.isPresent() && jobOptional.isPresent()) {
            User user = userOptional.get();
            Job job = jobOptional.get();

            // Resolve savedJobs if using DBRef
            user.getSavedJobs().size(); // Force lazy-loading

            List<Job> savedBefore = new ArrayList<>(user.getSavedJobs());
            user.addOrRemoveJobToUser(job);
            List<Job> savedAfter = new ArrayList<>(user.getSavedJobs());

//            System.out.println("Saved jobs length: " + savedBefore.size() + "\nSaved jobs after: " + savedAfter.size());

            if (savedBefore.size() == savedAfter.size()) {
                if (saveJobsModel.isWantToSave()) {
//                    System.out.println("Failed to save job, jobid: " + job.getId() + " , title: " + job.getTitle());
                    return new ResponseEntity<>("Failed to save job, jobid: " + job.getId() + " , title: " + job.getTitle(), HttpStatus.INTERNAL_SERVER_ERROR);
                } else {
//                    System.out.println("Failed to remove job, jobid: " + job.getId() + " , title: " + job.getTitle());
                    return new ResponseEntity<>("Failed to remove job, jobid: " + job.getId() + " , title: " + job.getTitle(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                userService.saveUser(user); // Persist changes to the database
                if(saveJobsModel.isWantToSave()){
                    return new ResponseEntity<>("successfully saved the job : "+job.getId() + " , title: " + job.getTitle(), HttpStatus.OK);
                }else {
                    return new ResponseEntity<>("successfully removed the job : "+job.getId() + " , title: " + job.getTitle(), HttpStatus.OK);
                }
            }
        } else {
            return new ResponseEntity<>("User or job not found", HttpStatus.NOT_FOUND);
        }
    }

}
