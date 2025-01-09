package com.saif.JobNet.controller;

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

    @PostMapping
    public ResponseEntity<?> saveUser(@RequestBody User user){
        userService.saveUser(user);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user){
        Optional<User> userBefore=userService.getUserById(user.getId());
        userService.saveUser(user);
        Optional<User> userAfter =userService.getUserById(user.getId());
        if(userService.checkUpdatedOrNot(userBefore.get(),userAfter.get()))
            return new ResponseEntity<>(userAfter.get(),HttpStatus.ACCEPTED);

        return new ResponseEntity<>(userAfter.get(),HttpStatus.NOT_MODIFIED);
    }

}
