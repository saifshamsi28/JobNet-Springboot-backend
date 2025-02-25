package com.saif.JobNet.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private static final String SUPABASE_URL = "https://your-project-id.supabase.co";
    private static final String SUPABASE_BUCKET = "resumes";
    private static final String SUPABASE_SERVICE_ROLE_KEY = "your-supabase-service-role-key";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + fileName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_SERVICE_ROLE_KEY);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String fileUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + fileName;

                // Store fileUrl in MongoDB (Implement your MongoDB service here)
                // Example: resumeService.saveResumeUrl(userId, fileUrl);

                return ResponseEntity.ok(Map.of("message", "Resume uploaded successfully", "url", fileUrl));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Failed to upload resume");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading resume: " + e.getMessage());
        }
    }
}
