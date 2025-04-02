package com.saif.JobNet.controller;

import com.saif.JobNet.model.JobNetResponse;
import com.saif.JobNet.model.ResumeResponseEntity;
import com.saif.JobNet.model.Resume;
import com.saif.JobNet.model.User;
import com.saif.JobNet.services.SupabaseStorageService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Optional;

@RestController
@RequestMapping("/user/resume")
public class ResumeController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
    @Autowired
    private UserService userService;

    @PostMapping("/upload-chunk")
    public ResponseEntity<?> uploadResumeChunk(
            @RequestParam("userId") String userId,
            @RequestParam("resumeName") String resumeName,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestPart("file") MultipartFile file) {

        System.out.println("Received chunk " + chunkIndex + "/" + totalChunks + " for " + resumeName + " (Size: " + file.getSize() + " bytes)");

        Optional<User> user=userService.getUserById(userId);

        if(user.isEmpty()){
            return new ResponseEntity<>(new ResumeResponseEntity("user not found with the given user id: "+userId, HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }

        // Ensure user directory exists
        File tempDir = new File(UPLOAD_DIR + File.separator + userId);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
//            return new ResponseEntity<>(new ResumeResponseEntity("Failed to create user directory", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
            tempDir = new File(UPLOAD_DIR + File.separator + userId);
            System.out.println("user dir not found created one user dir");

        }

        if (!tempDir.exists() && !tempDir.mkdirs()) {
            System.out.println("user dir not created returning error");
            return new ResponseEntity<>(new ResumeResponseEntity("Failed to create user directory", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
//            tempDir = new File(UPLOAD_DIR + File.separator + userId);
        }


        File chunkFile = new File(tempDir, resumeName + ".part" + chunkIndex);
        try {
            file.transferTo(chunkFile);
            System.out.println("Chunk " + chunkIndex + " saved at " + chunkFile.getAbsolutePath() + " (Size: " + chunkFile.length() + " bytes)");
        } catch (IOException e) {
            return new ResponseEntity<>(new ResumeResponseEntity("Error saving chunk: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new ResumeResponseEntity("Chunk " + chunkIndex + " uploaded successfully", HttpStatus.OK.value()), HttpStatus.OK);
    }

    @PostMapping("/finalize-upload")
    public ResponseEntity<?> finalizeUpload(
            @RequestParam("userId") String userId,
            @RequestParam("resumeDate") String resumeDate,
            @RequestParam("resumeSize") String resumeSize,
            @RequestParam("resumeName") String resumeName,
            @RequestParam("totalChunks") int totalChunks) {

        System.out.println("\nFinalizing upload for: " + resumeName + " (User: " + userId + ")");
        System.out.println("merging total "+totalChunks+" chunks in finalize method");

        Optional<User> userBox=userService.getUserById(userId);
        if(userBox.isEmpty()){
            return new ResponseEntity<>(new ResumeResponseEntity("user not found with the given user id: "+userId, HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }

        //Upload to Supabase
        File userDir = new File(UPLOAD_DIR + File.separator + userId);
        if (!userDir.exists()) {
            userDir = new File(UPLOAD_DIR + File.separator + userId);
            System.out.println("user dir not found created one user dir");
        }

        File finalFile = new File(userDir, resumeName);
        try (FileOutputStream fos = new FileOutputStream(finalFile, true);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {

            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(userDir, resumeName + ".part" + i);
                if (!chunkFile.exists()) {
                    return new ResponseEntity<>(new ResumeResponseEntity("Missing chunk: " + i, HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
                }

                // Read and merge chunk
                try (FileInputStream fis = new FileInputStream(chunkFile);
                     BufferedInputStream chunkStream = new BufferedInputStream(fis)) {

                    byte[] buffer = new byte[8192];  // Use 8KB buffer for efficiency
                    int bytesRead;
                    while ((bytesRead = chunkStream.read(buffer)) != -1) {
                        mergingStream.write(buffer, 0, bytesRead);
                    }
                }

                // Delete chunk after merging
                if (!chunkFile.delete()) {
                    System.err.println("Failed to delete chunk: " + chunkFile.getName());
                }
            }
        } catch (IOException e) {
            return new ResponseEntity<>(new ResumeResponseEntity("Error merging chunks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Verify file size
        long expectedSize = Long.parseLong(resumeSize);
        System.out.println("Expected merged file size: " + expectedSize + " bytes");
        System.out.println("Actual merged file size: " + finalFile.length() + " bytes");
        if (finalFile.length() != expectedSize) {
            return new ResponseEntity<>(new ResumeResponseEntity("File size mismatch! Expected: " + expectedSize + " bytes, Got: " + finalFile.length() + " bytes", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        System.out.println("Merged all chunks successfully: " + finalFile.getAbsolutePath());

        JobNetResponse supabaseResponse;
        try {
            SupabaseStorageService supabaseStorageService=new SupabaseStorageService();
//            supabaseUploadedFileUrl = uploadToSupabase(uniqueFileName, finalFile);
            supabaseResponse=supabaseStorageService.uploadToSupabase(finalFile.getName(),finalFile,"resume");
            System.out.println("Uploaded file to Supabase: " + supabaseResponse);
        } catch (IOException e) {
            return new ResponseEntity<>(new ResumeResponseEntity("Error uploading to Supabase: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //Delete the merged file after successful upload
        if (finalFile.delete()) {
            System.out.println("Deleted local merged file: " + finalFile.getAbsolutePath());
        }


        if (userDir.delete()) {
            System.out.println("Deleted user dir: " + userDir.getAbsolutePath());
        }


        //update user with resume details
        User user=userBox.get();

        // prepare resume response
        Resume resume = new Resume();
        resume.setResumeName(resumeName);
        resume.setResumeUploadDate(resumeDate);
        resume.setResumeSize(resumeSize);
        resume.setResumeUrl(supabaseResponse.getMessage());

        //save updated user to database
        user.setResume(resume);
        userService.saveUser(user);

        return new ResponseEntity<>(resume, HttpStatus.OK);
    }

}
