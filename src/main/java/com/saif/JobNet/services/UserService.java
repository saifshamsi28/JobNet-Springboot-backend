package com.saif.JobNet.services;

import com.saif.JobNet.model.AuthResponse;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserLoginCredentials;
import com.saif.JobNet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JWTService jwtService;

    public void saveUser(User user) {
        user.setPassword(new BCryptPasswordEncoder(12).encode(user.getPassword()));
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

    public String verify(UserLoginCredentials credentials) {

        Authentication authentication=
                authManager.authenticate(new UsernamePasswordAuthenticationToken(credentials.getUserNameOrEmail(),credentials.getPassword()));

        if(authentication.isAuthenticated()){
            return jwtService.generateToken(credentials.getUserNameOrEmail());
        }else {
            return "Authentication failed";
        }

    }
}
