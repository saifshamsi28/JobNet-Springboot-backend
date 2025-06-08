package com.saif.JobNet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saif.JobNet.model.*;
import com.saif.JobNet.model.resume.ParsedResumeDetails;
import com.saif.JobNet.model.resume.Resume;
import com.saif.JobNet.model.resume.ResumeResponseEntity;
import com.saif.JobNet.repositories.JobsRepository;
import com.saif.JobNet.services.ResumeParsingService;
import com.saif.JobNet.services.SupabaseStorageService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/user/resume")
public class ResumeController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
    @Autowired
    private UserService userService;

    @Autowired
    private JobsRepository jobsRepository;

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
        System.out.println("Merging total " + totalChunks + " chunks");

        Optional<User> userBox = userService.getUserById(userId);
        if (userBox.isEmpty()) {
            return new ResponseEntity<>(new ResumeResponseEntity("User not found with userId: " + userId, HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }

        File userDir = new File(UPLOAD_DIR + File.separator + userId);
        if (!userDir.exists()) {
            return new ResponseEntity<>(new ResumeResponseEntity("User upload directory not found", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File finalFile = new File(userDir, resumeName);

        try (FileOutputStream fos = new FileOutputStream(finalFile, true);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {

            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(userDir, resumeName + ".part" + i);
                if (!chunkFile.exists()) {
                    cleanupDirectory(userDir);
                    return new ResponseEntity<>(new ResumeResponseEntity("Missing chunk: " + i, HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
                }

                try (FileInputStream fis = new FileInputStream(chunkFile);
                     BufferedInputStream chunkStream = new BufferedInputStream(fis)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = chunkStream.read(buffer)) != -1) {
                        mergingStream.write(buffer, 0, bytesRead);
                    }
                }

                if (!chunkFile.delete()) {
                    System.err.println("Failed to delete chunk: " + chunkFile.getName());
                }
            }

        } catch (IOException e) {
            cleanupDirectory(userDir);
            return new ResponseEntity<>(new ResumeResponseEntity("Error merging chunks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        long expectedSize = Long.parseLong(resumeSize);
        if (finalFile.length() != expectedSize) {
            cleanupDirectory(userDir);
            return new ResponseEntity<>(new ResumeResponseEntity("File size mismatch! Expected: " + expectedSize + " bytes, Got: " + finalFile.length(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Upload to Supabase
        JobNetResponse supabaseResponse;
        try {
            SupabaseStorageService supabaseStorageService = new SupabaseStorageService();
            supabaseResponse = supabaseStorageService.uploadToSupabase(finalFile.getName(), finalFile, "resume");
            System.out.println("Uploaded file to Supabase: " + supabaseResponse.getMessage());
        } catch (IOException e) {
            cleanupDirectory(userDir);
            return new ResponseEntity<>(new ResumeResponseEntity("Error uploading to Supabase: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Call Flask backend with final file
        Map<String, Object> parsedInfo = null;

        ParsedResumeDetails parsedDetails;
        try {
            parsedInfo = ResumeParsingService.parseResumeWithFlask(finalFile);
            parsedDetails = new ObjectMapper().convertValue(parsedInfo, ParsedResumeDetails.class);
            System.out.println("parsed details: "+parsedDetails);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        if (!finalFile.delete()) {
            System.err.println("Warning: Could not delete merged file: " + finalFile.getAbsolutePath());
        }
        if (!userDir.delete()) {
            System.err.println("Warning: Could not delete user directory: " + userDir.getAbsolutePath());
        }

        // Update User
        User user = userBox.get();
        Resume resume = new Resume();
        resume.setResumeName(resumeName);
        resume.setResumeUploadDate(resumeDate);
        resume.setResumeSize(resumeSize);
        resume.setResumeUrl(supabaseResponse.getMessage());
        user.setResume(resume);

        List<String> existingSkills = user.getSkills();
        List<String> parsedSkills = parsedDetails.getSkills();

        // Normalize existing skills into a map with lowercase keys for comparison
        Map<String, String> normalizedSkillsMap = new LinkedHashMap<>();
        for (String skill : existingSkills) {
            normalizedSkillsMap.put(skill.toLowerCase(), skill);
        }

        // Add new parsed skills, only if not already present (case-insensitive)
        for (String skill : parsedSkills) {
            String normalized = skill.toLowerCase();

            // Skip if similar skill already exists (like "java" vs "Java (Programming Language)")
            boolean isDuplicate = normalizedSkillsMap.keySet().stream()
                    .anyMatch(existing -> normalized.contains(existing) || existing.contains(normalized));

            if (!isDuplicate) {
                normalizedSkillsMap.put(normalized, skill);
            }
        }

        // Update skills and save
        user.setSkills(new ArrayList<>(normalizedSkillsMap.values()));
        user.setParsedDetails(parsedDetails);
        userService.saveUser(user);

        return new ResponseEntity<>(resume, HttpStatus.OK);
    }

    private void cleanupDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        System.err.println("Failed to delete: " + file.getAbsolutePath());
                    }else {
                        System.out.println(file.getName()+" deleted successfully");
                    }
                }
            }
            if (!directory.delete()) {
                System.err.println("Failed to delete directory: " + directory.getAbsolutePath());
            } else {
                System.out.println("Successfully cleaned up: " + directory.getAbsolutePath());
            }
        }
    }
}
