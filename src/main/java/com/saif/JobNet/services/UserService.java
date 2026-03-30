package com.saif.JobNet.services;

import com.saif.JobNet.model.UserRole;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserLoginCredentials;
import com.saif.JobNet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Locale;
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
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (user.getUserName() != null) {
            user.setUserName(user.getUserName().trim());
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.JOB_SEEKER);
        }
        String password = user.getPassword();
        if (password != null && !password.isBlank()) {
            boolean alreadyHashed = password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
            if (!alreadyHashed) {
                user.setPassword(new BCryptPasswordEncoder(12).encode(password));
            }
        }
        userRepository.save(user); // Using save instead of `insert` for both insert and update
    }

    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id){
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUserName(String userName){
        if (userName == null || userName.isBlank()) {
            return Optional.empty();
        }
        return userRepository.getUserByUserNameIgnoreCase(userName.trim());
    }

    public Optional<User> getUserByEmail(String email){
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.getUserByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
    }


    public void deleteUserById(String id){
        userRepository.deleteById(id);
    }

    public boolean checkUserNameAvailable(String username){
        if (username == null || username.isBlank()) {
            return false;
        }
        return userRepository.existsByUserNameIgnoreCase(username.trim());
    }

    public boolean checkEmailAlreadyExists(String email){
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
    }

    public String verify(UserLoginCredentials credentials) {
        Authentication authentication=
                authManager.authenticate(new UsernamePasswordAuthenticationToken(credentials.getUserNameOrEmail(),credentials.getPassword()));

        if(authentication.isAuthenticated()){
            Optional<User> user = getUserByIdentity(credentials.getUserNameOrEmail());
            if (user.isPresent()) {
                return jwtService.generateToken(user.get().getUserName());
            }
            return jwtService.generateToken(credentials.getUserNameOrEmail());
        }else {
            return "Authentication failed";
        }
    }

    public boolean authenticate(UserLoginCredentials credentials) {
        Authentication authentication =
                authManager.authenticate(new UsernamePasswordAuthenticationToken(credentials.getUserNameOrEmail(), credentials.getPassword()));
        return authentication.isAuthenticated();
    }

    public Optional<User> getUserByIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            return Optional.empty();
        }
        String normalized = identity.trim();
        Optional<User> byUserName = getUserByUserName(normalized);
        if (byUserName.isPresent()) {
            return byUserName;
        }
        return getUserByEmail(normalized);
    }
}
