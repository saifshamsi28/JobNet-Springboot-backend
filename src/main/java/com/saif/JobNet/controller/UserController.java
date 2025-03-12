package com.saif.JobNet.controller;

import ch.qos.logback.core.util.FileUtil;
import com.saif.JobNet.model.*;
import com.saif.JobNet.services.JobsService;
import com.saif.JobNet.services.SupabaseStorageService;
import com.saif.JobNet.services.UserService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/user")
public class UserController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads/profile" + File.separator;

    @Autowired
    private UserService userService;

    @Autowired
    private JobsService jobsEntryService;

    @GetMapping("all")
    public ResponseEntity<List<User>> getAllUsers(){
        return new ResponseEntity<>(userService.getAllUser(),HttpStatus.OK);
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getUserById(@PathVariable String id){
        System.out.println("synchronizing the details");
        Optional<User> user=userService.getUserById(id);
        if(user.isPresent()){
            return new ResponseEntity<>(user.get(),HttpStatus.OK);
        }
        return new ResponseEntity<>(new JobNetResponse("user not found with id: "+id,HttpStatus.NOT_FOUND.value()),HttpStatus.NOT_FOUND);
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

    @PatchMapping("id/{id}/update-basic-details")
    public ResponseEntity<?> updateBasicDetails(@PathVariable String id,@RequestBody User user){
        System.out.println("got id : "+id+", user= "+user);
        if(user==null || user.getBasicDetails()==null){
            return new ResponseEntity<>(new JobNetResponse("User basic details missing",HttpStatus.BAD_REQUEST.value()),HttpStatus.BAD_REQUEST);
        }
        Optional<User> userBox=userService.getUserById(id);
        if(userBox.isPresent()){
            User existingUser=userBox.get();
            System.out.println("existing user basic details(before): "+existingUser.getBasicDetails());
            existingUser=user;
            userService.saveUser(existingUser);
            System.out.println("existing user basic details(after): "+existingUser.getBasicDetails());
            if(existingUser.getBasicDetails()!=null){
                return new ResponseEntity<>(new JobNetResponse("Basic Details Updated Successfully",HttpStatus.OK.value()),HttpStatus.OK);
            }else {
                return new ResponseEntity<>(new JobNetResponse("Basic Details not updated Successfully",HttpStatus.INTERNAL_SERVER_ERROR.value()),HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }else {
            return new ResponseEntity<>(new JobNetResponse("User not found ",HttpStatus.NOT_FOUND.value()),HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("{id}/upload-profile-image")
    public ResponseEntity<?> uploadProfileImage(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        System.out.println("received request with id: "+id+", file: "+file.getOriginalFilename());
        try {
            Optional<User> userBox = userService.getUserById(id);
            if (userBox.isPresent()) {
                System.out.println("received request with id: "+id+", file: "+file.getOriginalFilename());
                User user = userBox.get();

                // Convert MultipartFile to File
                File convertedFile = convertMultipartFileToFile(file);

                // Upload to Supabase
                SupabaseStorageService supabaseStorageService = new SupabaseStorageService();
                String profileImageUrl = supabaseStorageService.uploadToSupabase(file.getOriginalFilename(), convertedFile, "profile");

                // Update user's profile image URL
                user.setProfileImage(profileImageUrl);
                userService.saveUser(user);

                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new JobNetResponse("User not found", HttpStatus.NOT_FOUND.value()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @PostMapping("{id}/upload-profile-chunk")
    public ResponseEntity<?> uploadProfileImageChunk(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks) {

        System.out.println("received: id="+id+", chunkIndex: "+chunkIndex+", totalChunks: "+totalChunks);
        try {
            Optional<User> userBox = userService.getUserById(id);
            if (userBox.isPresent()) {
                File tempFile = new File("uploads/profile_" + id + "_" + chunkIndex + ".tmp");
                file.transferTo(tempFile);

                // If last chunk, merge all chunks
                if (chunkIndex == totalChunks - 1) {
                    File finalFile = new File("uploads/profile_" + id + ".jpg");
                    FileOutputStream outputStream = new FileOutputStream(finalFile, true);

                    for (int i = 0; i < totalChunks; i++) {
                        File chunk = new File("uploads/profile_" + id + "_" + i + ".tmp");
                        Files.copy(chunk.toPath(), outputStream);
                        chunk.delete();
                    }
                    outputStream.close();
                    return ResponseEntity.ok("Profile picture uploaded successfully.");
                }

                return ResponseEntity.ok("Chunk " + chunkIndex + " uploaded.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed.");
        }
    }

    // Convert MultipartFile to File
    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File convFile = File.createTempFile("profile", multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(multipartFile.getBytes());
        }
        return convFile;
    }

}
