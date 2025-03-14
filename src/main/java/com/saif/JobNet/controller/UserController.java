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

import java.io.*;
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

    @PostMapping("{id}/upload-profile-chunk")
    public ResponseEntity<?> uploadProfileImageChunk(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks) {

        System.out.println("Received chunk: id=" + id + ", chunkIndex=" + chunkIndex + "/" + totalChunks +
                ", Size: " + file.getSize() + " bytes");

        // Validate user
        Optional<User> userBox = userService.getUserById(id);
        if (userBox.isEmpty()) {
            return new ResponseEntity<>(new JobNetResponse("User not found", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg"; // Default to .jpg

        if (!fileExtension.matches("\\.(jpg|jpeg|png|webp|bmp|gif)")) {
            return new ResponseEntity<>(new JobNetResponse("Unsupported file type: " + fileExtension, HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        // Ensure user directory exists
        File userDir = new File(UPLOAD_DIR + File.separator + id);
        if (!userDir.exists() && !userDir.mkdirs()) {
            System.err.println("failed to create user directory, creating again");
            userDir = new File(UPLOAD_DIR + File.separator + id);
            if(userDir.exists()){
                System.out.println("user dir created successfully");
            }
        }
        if (!userDir.exists() && !userDir.mkdirs()) {
            System.err.println("failed to create user directory, returning error");
            return new ResponseEntity<>(new JobNetResponse("Failed to create user directory", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Save the chunk
        File chunkFile = new File(userDir, chunkIndex + ".part");
        try {
            file.transferTo(chunkFile);
        } catch (IOException e) {
            return new ResponseEntity<>(new JobNetResponse("Error saving chunk: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Track received chunks
        File chunkTrackerFile = new File(userDir, "chunks_received.txt");
        Set<Integer> receivedChunks = new HashSet<>();

        // Read existing chunks from the file
        if (chunkTrackerFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(chunkTrackerFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        receivedChunks.add(Integer.parseInt(line));
                    }
                }
            } catch (IOException e) {
                return new ResponseEntity<>(new JobNetResponse("Error reading chunk tracker", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Add the current chunk if not already present
        if (!receivedChunks.contains(chunkIndex)) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(chunkTrackerFile, true))) {
                bw.write(chunkIndex + "\n"); // Append instead of overwriting
                bw.flush(); // Ensure data is written immediately
            } catch (IOException e) {
                return new ResponseEntity<>(new JobNetResponse("Error updating chunk tracker", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Check if all chunks are received
        receivedChunks.add(chunkIndex); // Add current chunk in the set
        if (receivedChunks.size() == totalChunks) {
            System.out.println("All chunks received. Starting merge...");

            File finalFile = new File(userDir, "profile_" + id + fileExtension);
            if (mergeChunks(userDir, totalChunks, finalFile)) {
                System.out.println("Profile image successfully merged: " + finalFile.getAbsolutePath());

                if (chunkTrackerFile.delete()) {
                    System.out.println("Chunk tracker deleted: " + chunkTrackerFile.getName());
                } else {
                    System.err.println("Failed to delete chunk tracker: " + chunkTrackerFile.getName());
                }

                // Upload to Supabase
                User user = userBox.get();
                SupabaseStorageService storageService = new SupabaseStorageService();
                try {
                    JobNetResponse supabaseResponse = storageService.uploadToSupabase(finalFile.getName(), finalFile, "profile");
                    if (supabaseResponse != null && supabaseResponse.getStatus() == 200) {
                        String cacheBusterUrl = supabaseResponse.getMessage() + "?t=" + System.currentTimeMillis();
                        user.setProfileImage(cacheBusterUrl);
                        userService.saveUser(user);

                        if (!finalFile.delete()) {
                            System.err.println("Final file is not deleted");
                        } else {
                            System.out.println("Final file deleted successfully");
                        }

                        if (!userDir.delete()) {
                            System.err.println("User dir is not deleted");
                        } else {
                            System.out.println("User dir deleted successfully");
                        }

                        return new ResponseEntity<>(supabaseResponse, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(supabaseResponse, HttpStatus.BAD_REQUEST);
                    }
                } catch (IOException e) {
                    return new ResponseEntity<>(new JobNetResponse("Error uploading to Supabase: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity<>(new JobNetResponse("Error merging chunks", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(new JobNetResponse("Chunk " + chunkIndex + " uploaded successfully.", HttpStatus.OK.value()), HttpStatus.OK);
    }



    /**
     * Merge all received chunks in order
     */
    private boolean mergeChunks(File userDir, int totalChunks, File outputFile) {
        File[] chunkFiles = userDir.listFiles((dir, name) -> name.endsWith(".part"));

        if (chunkFiles == null || chunkFiles.length != totalChunks) {
            System.out.println("Not all chunks received yet. Waiting...");
            return false;
        }

        // Sort chunks by index
        Arrays.sort(chunkFiles, Comparator.comparingInt(f -> Integer.parseInt(f.getName().replace(".part", ""))));

        try (FileOutputStream fos = new FileOutputStream(outputFile, true);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {

            for (File chunk : chunkFiles) {
                try (FileInputStream fis = new FileInputStream(chunk);
                     BufferedInputStream chunkStream = new BufferedInputStream(fis)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = chunkStream.read(buffer)) != -1) {
                        mergingStream.write(buffer, 0, bytesRead);
                    }
                }
                if(chunk.delete()){
                    System.out.println("chunk "+chunk.getName()+" deleted successfully");
                }else {
                    System.err.println("Failed to delete: "+chunk.getName());
                }
            }
        } catch (IOException e) {
            System.err.println("Error merging chunks: " + e.getMessage());
            return false;
        }

        return true;
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
