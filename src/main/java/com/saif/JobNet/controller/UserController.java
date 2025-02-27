package com.saif.JobNet.controller;

import com.saif.JobNet.model.UpdateUserRequest;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.SaveJobsModel;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserLoginCredentials;
import com.saif.JobNet.services.JobsService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/user")
public class UserController {

//    @Value("${SUPABASE_URL}")
    private final String SUPABASE_URL=System.getenv("SUPABASE_URL");

//    @Value("${SUPABASE_BUCKET}")
    private final String SUPABASE_BUCKET=System.getenv("SUPABASE_BUCKET");

//    @Value("${SUPABASE_SERVICE_ROLE_KEY}")
    private final String SUPABASE_SERVICE_ROLE_KEY=System.getenv("SUPABASE_SERVICE_ROLE_KEY");
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

            System.out.println("Saved jobs length: " + savedBefore.size() + "\nSaved jobs after: " + savedAfter.size());

            if (savedBefore.size() == savedAfter.size()) {
                if (saveJobsModel.isWantToSave()) {
                    System.out.println("Failed to save job, jobid: " + job.getId() + " , title: " + job.getTitle());
                    return new ResponseEntity<>("Failed to save job, jobid: " + job.getId() + " , title: " + job.getTitle(), HttpStatus.INTERNAL_SERVER_ERROR);
                } else {
                    System.out.println("Failed to remove job, jobid: " + job.getId() + " , title: " + job.getTitle());
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

    @PostMapping("resume/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("id") String id,
                                          @RequestParam("filename") String resumeName,
                                          @RequestParam("file") MultipartFile file) {
        System.out.println("user id: "+id);
        System.out.println("user resume name received: "+resumeName);
        try {
            String fileName =generateUniqueFileName(resumeName);
            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + fileName;
            System.out.println("upload url: " + uploadUrl);

            Optional<User> userBox=userService.getUserById(id);

            if(userBox.isEmpty()){
                return new ResponseEntity<>("Failed to upload resume as user not found",HttpStatus.NOT_FOUND);
            }

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_SERVICE_ROLE_KEY);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create request body
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // Important for Supabase to recognize file format
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String fileUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + fileName;
                System.out.println("file uploaded successfully: url=" + fileUrl);

                User user=userBox.get();
                user.setResumeUploaded(true);
                user.setResumeUrl(fileUrl);
                user.setResumeUploadDate(getCurrentDate());
                userService.saveUser(user);
                return ResponseEntity.ok(fileUrl);
            } else {
                System.out.println("response from supabase: " + response);
                return ResponseEntity.status(response.getStatusCode()).body("Failed to upload resume");
            }

        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading resume: " + e.getMessage());
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");

        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);  // Get file extension
            originalFileName = originalFileName.substring(0, dotIndex);  // Remove extension
        }

        String timestamp = String.valueOf(System.currentTimeMillis());  // Unique timestamp
        return originalFileName + "_" + timestamp + extension;  // New unique filename
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}
