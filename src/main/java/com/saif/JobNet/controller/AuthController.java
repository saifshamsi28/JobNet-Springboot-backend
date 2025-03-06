package com.saif.JobNet.controller;

import com.saif.JobNet.model.AuthResponse;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserLoginCredentials;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userService.checkEmailAlreadyExists(user.getEmail()) || userService.checkUserNameAvailable(user.getUserName())) {
            System.out.println("email already exists");
            return new ResponseEntity<>(new AuthResponse("Email or username already exists", HttpStatus.BAD_REQUEST.value()),HttpStatus.BAD_REQUEST);
        }

        userService.saveUser(user);
        return new ResponseEntity<>(new AuthResponse("User Registered Successfully",HttpStatus.CREATED.value()),HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginCredentials credentials) {
        Optional<User> userOpt = userService.getUserByUserName(credentials.getUserNameOrEmail());
        if(userOpt.isEmpty()){
            userOpt=userService.getUserByEmail(credentials.getUserNameOrEmail());
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(credentials.getPassword())) {
                return new ResponseEntity<>(user, HttpStatus.OK);
            }
        }
        System.out.println("invalid credentials: emailOrUserName: "+credentials.getUserNameOrEmail()+", password: "+ credentials.getPassword());
        return new ResponseEntity<>(new AuthResponse("Invalid credentials", HttpStatus.UNAUTHORIZED.value()),HttpStatus.UNAUTHORIZED);
    }
}
