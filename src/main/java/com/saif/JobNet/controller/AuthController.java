package com.saif.JobNet.controller;

import com.saif.JobNet.model.AuthResponse;
import com.saif.JobNet.model.RefreshTokenRequest;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserLoginCredentials;
import com.saif.JobNet.services.JWTService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (user == null) {
            return new ResponseEntity<>(new AuthResponse("Request body is required", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = user.getUserName() == null ? "" : user.getUserName().trim();
        String password = user.getPassword() == null ? "" : user.getPassword();
        String name = user.getName() == null ? "" : user.getName().trim();

        if (name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
            return new ResponseEntity<>(new AuthResponse("Name, username, email, and password are required", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }
        if (!email.contains("@") || !email.contains(".")) {
            return new ResponseEntity<>(new AuthResponse("Please provide a valid email address", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }
        if (password.length() < 6) {
            return new ResponseEntity<>(new AuthResponse("Password must be at least 6 characters", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        if (userService.checkEmailAlreadyExists(email)) {
            return new ResponseEntity<>(new AuthResponse("Email is already registered", HttpStatus.CONFLICT.value()), HttpStatus.CONFLICT);
        }
        if (userService.checkUserNameAvailable(username)) {
            return new ResponseEntity<>(new AuthResponse("Username is already taken", HttpStatus.CONFLICT.value()), HttpStatus.CONFLICT);
        }

        user.setEmail(email);
        user.setUserName(username);
        user.setName(name);
        userService.saveUser(user);
        return new ResponseEntity<>(new AuthResponse("User registered successfully", HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

//    @PostMapping("/login")
//    public ResponseEntity<?> loginUser(@RequestBody UserLoginCredentials credentials) {
//        Optional<User> userOpt = userService.getUserByUserName(credentials.getUserNameOrEmail());
//        if(userOpt.isEmpty()){
//            userOpt=userService.getUserByEmail(credentials.getUserNameOrEmail());
//        }
//
//        if (userOpt.isPresent()) {
//            User user = userOpt.get();
//            if (user.getPassword().equals(new BCryptPasswordEncoder(12).encode(credentials.getPassword()))) {
//                return new ResponseEntity<>(user, HttpStatus.OK);
//            }else {
//                System.out.println("invalid credentials: email or UserName: "+credentials.getUserNameOrEmail());
//                return new ResponseEntity<>(new AuthResponse("Invalid credentials", HttpStatus.UNAUTHORIZED.value()),HttpStatus.UNAUTHORIZED);
//            }
//        }else {
//            return new ResponseEntity<>(new AuthResponse("user not found",HttpStatus.NOT_FOUND.value()),HttpStatus.NOT_FOUND);
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody UserLoginCredentials credentials) {
        if (credentials == null) {
            return new ResponseEntity<>(new AuthResponse("Request body is required", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        String identity = credentials.getUserNameOrEmail() == null ? "" : credentials.getUserNameOrEmail().trim();
        String password = credentials.getPassword() == null ? "" : credentials.getPassword();

        if (identity.isBlank() || password.isBlank()) {
            return new ResponseEntity<>(new AuthResponse("Username/email and password are required", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        System.out.println("login attempted by: " + identity);

        Optional<User> user = userService.getUserByIdentity(identity);
        if (user.isEmpty()) {
            return new ResponseEntity<>(new AuthResponse("No account found for this username or email", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }

        try {
            boolean authenticated = userService.authenticate(credentials);
            if (!authenticated) {
                return new ResponseEntity<>(new AuthResponse("Incorrect password", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
            }
            String accessToken = jwtService.generateAccessToken(user.get().getUserName());
            String refreshToken = jwtService.generateRefreshToken(user.get().getUserName());
            System.out.println("login successful");
            return new ResponseEntity<>(
                    new AuthResponse("Login successful", HttpStatus.OK.value(), accessToken, refreshToken),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            System.err.println("login failed: " + e.getMessage());
            return new ResponseEntity<>(new AuthResponse("Incorrect password", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return new ResponseEntity<>(new AuthResponse("Refresh token is required", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        String refreshToken = request.getRefreshToken().trim();
        try {
            if (!jwtService.isRefreshTokenValid(refreshToken)) {
                return new ResponseEntity<>(new AuthResponse("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
            }

            String identity = jwtService.extractUserName(refreshToken);
            Optional<User> userOpt = userService.getUserByIdentity(identity);
            if (userOpt.isEmpty()) {
                return new ResponseEntity<>(new AuthResponse("User not found", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
            }

            String newAccessToken = jwtService.generateAccessToken(userOpt.get().getUserName());
            String rotatedRefreshToken = jwtService.generateRefreshToken(userOpt.get().getUserName());
            return new ResponseEntity<>(
                    new AuthResponse("Token refreshed", HttpStatus.OK.value(), newAccessToken, rotatedRefreshToken),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(new AuthResponse("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getLoggedInUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("fetching user");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            return new ResponseEntity<>(new AuthResponse("Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.replace("Bearer ", "");
        String emailOrUsername;
        try {
            emailOrUsername = jwtService.extractUserName(token);
        } catch (Exception e) {
            return new ResponseEntity<>(new AuthResponse("Invalid or expired token", HttpStatus.UNAUTHORIZED.value()), HttpStatus.UNAUTHORIZED);
        }

//        System.out.println("extracted username or mail: "+emailOrUsername);
        Optional<User> userOpt = userService.getUserByUserName(emailOrUsername);
        if(userOpt.isEmpty()) {
            userOpt = userService.getUserByEmail(emailOrUsername);
        }

        return userOpt
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
