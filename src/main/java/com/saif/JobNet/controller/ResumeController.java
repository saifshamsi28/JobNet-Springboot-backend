package com.saif.JobNet.controller;

import com.saif.JobNet.model.ResumeResponseEntity;
import com.saif.JobNet.model.Resume;
import com.saif.JobNet.model.User;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

@RestController
@RequestMapping("/user/resume")
public class ResumeController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

    private final String SUPABASE_URL = System.getenv("SUPABASE_URL");
    private final String SUPABASE_BUCKET = System.getenv("SUPABASE_BUCKET");
    private final String SUPABASE_SERVICE_ROLE_KEY = System.getenv("SUPABASE_SERVICE_ROLE_KEY");

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

        String uniqueFileName = userService.generateUniqueFileName(resumeName);
        String supabaseUploadedFileUrl;
        try {
            supabaseUploadedFileUrl = uploadToSupabase(uniqueFileName, finalFile);
            System.out.println("Uploaded file to Supabase: " + supabaseUploadedFileUrl);
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
        user.setResumeUploaded(true);
        user.setResumeName(resumeName);
        user.setResumeUploadDate(resumeDate);
        user.setResumeUrl(supabaseUploadedFileUrl);
        user.setResumeSize(resumeSize);

        //save updated user to database
        userService.saveUser(user);

        // prepare resume response
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setResumeName(resumeName);
        resume.setResumeUploadDate(resumeDate);
        resume.setResumeSize(resumeSize);
        resume.setResumeUrl(supabaseUploadedFileUrl);


        return new ResponseEntity<>(resume, HttpStatus.OK);
    }

    private String uploadToSupabase(String fileName, File file) throws IOException {
        String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + fileName;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + SUPABASE_SERVICE_ROLE_KEY);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(Files.readAllBytes(file.toPath())) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String uploadedUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + fileName;
            System.out.println("Upload successful: " + uploadedUrl);
            return uploadedUrl;
        } else {
            System.err.println("Supabase upload failed: " + response.getBody());
            throw new IOException("Failed to upload to Supabase: " + response.getBody());
        }
    }
}
