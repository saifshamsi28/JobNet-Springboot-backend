package com.saif.JobNet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.saif.JobNet.UpdateUserRequest;
import com.saif.JobNet.model.User;
import com.saif.JobNet.services.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping()
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

    @PostMapping
    public ResponseEntity<?> saveUser(@RequestBody User user){
        userService.saveUser(user);
        return new ResponseEntity<>(HttpStatus.CREATED);
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
        if (updateRequest.getSavedJobs() != null) {
            existingUser.setSavedJobs(updateRequest.getSavedJobs());
        }

        userService.saveUser(existingUser);

        return new ResponseEntity<>(existingUser, HttpStatus.OK);
    }



    @DeleteMapping("all")
    public ResponseEntity<?> deleteAllUsers(){
        userService.deleteAllUsers();
        if(userService.getAllUser().isEmpty()){
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }

}
