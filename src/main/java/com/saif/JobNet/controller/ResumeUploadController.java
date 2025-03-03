package com.saif.JobNet.controller;

import com.saif.JobNet.exception_handling.ResumeResponseEntity;
import com.saif.JobNet.model.Resume;
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
import java.util.Arrays;
import java.util.Comparator;

@RestController
@RequestMapping("/user/resume")
public class ResumeUploadController {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

    private final String SUPABASE_URL = System.getenv("SUPABASE_URL");
    private final String SUPABASE_BUCKET = System.getenv("SUPABASE_BUCKET");
    private final String SUPABASE_SERVICE_ROLE_KEY = System.getenv("SUPABASE_SERVICE_ROLE_KEY");

    @Autowired
    private UserService userService;

//    @PostMapping("/upload-chunk")
//    public ResponseEntity<?> uploadResumeChunk(
//            @RequestParam("userId") String userId,
//            @RequestParam("resumeName") String resumeName,
//            @RequestParam("chunkIndex") int chunkIndex,
//            @RequestParam("totalChunks") int totalChunks,
//            @RequestPart("file") MultipartFile file) {
//
//        System.err.println("Received chunk " + chunkIndex + "/" + totalChunks + " for " + resumeName);
//
//        // Ensure user directory exists
//        File tempDir = new File(UPLOAD_DIR + File.separator + userId);
//        if (!tempDir.exists()) {
//            if (!tempDir.mkdirs()) {
//                System.err.println("Failed to create directory for user: " + userId);
//                return new ResponseEntity<>(new ResumeResponseEntity("Failed to create temporary user directory to merge chunks",HttpStatus.INTERNAL_SERVER_ERROR.value()),HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//        } else {
//            System.out.println("Directory exists: " + tempDir.getAbsolutePath());
//        }
//
//        File chunkFile = new File(tempDir, resumeName + ".part" + chunkIndex);
//        try {
//            file.transferTo(chunkFile);
//            System.out.println("Chunk " + chunkIndex + " saved successfully at " + chunkFile.getAbsolutePath()+", chunkSize: "+chunkFile.length());
//        } catch (IOException e) {
//            System.err.println("Error saving chunk " + chunkIndex + ": " + e.getMessage());
//            return new ResponseEntity<>(new ResumeResponseEntity("Error saving chunk: " + e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR.value()),HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        return new ResponseEntity<>(new ResumeResponseEntity("Chunk " + chunkIndex + " uploaded successfully",HttpStatus.OK.value()),HttpStatus.OK);
////        return ResponseEntity.ok();
//    }

//    @PostMapping("/finalize-upload")
//    public ResponseEntity<?> finalizeUpload(
//            @RequestParam("userId") String userId,
//            @RequestParam("resumeDate") String resumeDate,
//            @RequestParam("resumeSize") String resumeSize,
//            @RequestParam("resumeName") String resumeName) {
//
//        System.out.println("Finalizing upload for: " + resumeName + " (User: " + userId + ")");
//
//        File tempDir = new File(UPLOAD_DIR + File.separator + userId);
//        File finalFile = new File(tempDir, resumeName);
//
//        try (FileOutputStream fos = new FileOutputStream(finalFile, true)) {
//            int chunkIndex = 0;
//            while (true) {
//                File chunkFile = new File(tempDir, resumeName + ".part" + chunkIndex);
//                if (!chunkFile.exists()) {
//                    break;
//                }
//                System.out.println("Merging chunk " + chunkIndex);
//                Files.copy(chunkFile.toPath(), fos);
//                chunkFile.delete(); // Delete chunk after merging
//                chunkIndex++;
//            }
//        } catch (IOException e) {
//            System.err.println("Error merging chunks: " + e.getMessage());
//            return new ResponseEntity<>(new ResumeResponseEntity("Error merging chunks: " + e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR.value()),HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        if (finalFile.length() == 0) {
//            System.err.println("Final merged file is empty. Upload failed.");
////            finalFile.delete();
////            tempDir.delete();
//            return new ResponseEntity<>(new ResumeResponseEntity("Final merge file is empty",HttpStatus.INTERNAL_SERVER_ERROR.value()),HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        // Generate a unique file name before uploading to Supabase
//        String uniqueFileName = userService.generateUniqueFileName(resumeName);
//        String supabaseUploadedFileUrl;
//        try {
//            supabaseUploadedFileUrl = uploadToSupabase(uniqueFileName, finalFile);
//            System.out.println("Uploaded file to Supabase: " + supabaseUploadedFileUrl);
//        } catch (IOException e) {
//            System.err.println("Error uploading to Supabase: " + e.getMessage());
//            return new ResponseEntity<>(new ResumeResponseEntity("Error uploading to Supabase: " + e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR.value()),HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        // Delete the merged file after successful upload
//        if (finalFile.delete()) {
//            System.out.println("Deleted local merged file: " + finalFile.getAbsolutePath());
//        } else {
//            System.err.println("Failed to delete local merged file: " + finalFile.getAbsolutePath());
//        }
//
//        Resume resume=new Resume();
//        resume.setUserId(userId);
//        resume.setResumeName(resumeName);
//        resume.setResumeUploadDate(resumeDate);
//        resume.setResumeSize(resumeSize);
//        resume.setResumeUrl(supabaseUploadedFileUrl);
//        return new ResponseEntity<>(resume,HttpStatus.OK);
//    }

    @PostMapping("/upload-chunk")
    public ResponseEntity<?> uploadResumeChunk(
            @RequestParam("userId") String userId,
            @RequestParam("resumeName") String resumeName,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestPart("file") MultipartFile file) {

        System.out.println("Received chunk " + chunkIndex + "/" + totalChunks + " for " + resumeName + " (Size: " + file.getSize() + " bytes)");

        // Ensure user directory exists
        File tempDir = new File(UPLOAD_DIR + File.separator + userId);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            return new ResponseEntity<>(new ResumeResponseEntity("Failed to create user directory", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File chunkFile = new File(tempDir, resumeName + ".part" + chunkIndex);
        try {
            file.transferTo(chunkFile);
            System.out.println("✔ Chunk " + chunkIndex + " saved at " + chunkFile.getAbsolutePath() + " (Size: " + chunkFile.length() + " bytes)");
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

        System.out.println("\n🔹 Finalizing upload for: " + resumeName + " (User: " + userId + ")");
        System.out.println("merging total "+totalChunks+" chunks in finalize method");

//        File tempDir = new File(UPLOAD_DIR + File.separator + userId);
//        File finalFile = new File(tempDir, resumeName);
//
//
//        // Collect all chunk files
//        File[] chunkFiles = tempDir.listFiles((dir, name) -> name.startsWith(resumeName + ".part"));
//
//        if (chunkFiles == null || chunkFiles.length == 0) {
//            return new ResponseEntity<>(new ResumeResponseEntity("No chunks found!", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        // ✅ Check if all chunks are received
//        if (chunkFiles.length != totalChunks) {
//            System.err.println("❌ Not all chunks received! Received: " + chunkFiles.length + " / Expected: " + totalChunks);
//            return new ResponseEntity<>(new ResumeResponseEntity("Not all chunks received yet!", HttpStatus.PARTIAL_CONTENT.value()), HttpStatus.PARTIAL_CONTENT);
//        }
//
//        // ✅ Sort chunks numerically
//        Arrays.sort(chunkFiles, Comparator.comparingInt(f -> {
//            try {
//                return Integer.parseInt(f.getName().split("part")[1]); // Extract part number
//            } catch (Exception e) {
//                return Integer.MAX_VALUE;
//            }
//        }));
//
//        // ✅ Verify chunk sizes before merging
//        long mergedFileSize = 0;
//        for (File chunk : chunkFiles) {
//            System.out.println("✔ Chunk: " + chunk.getName() + " (Size: " + chunk.length() + " bytes)");
//            mergedFileSize += chunk.length();
//        }
//
//        // ✅ Merge the chunks properly
//        try (FileOutputStream fos = new FileOutputStream(finalFile, true);
//             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
//            int count=1;
//            for (File chunkFile : chunkFiles) {
//                Files.copy(chunkFile.toPath(), mergingStream);
//                String chunkName=chunkFile.getName();
//                System.out.println("merging chunk: "+chunkName);
//                boolean isDeleted=chunkFile.delete(); // Delete chunk after merging
//                if(isDeleted){
//                    System.out.println("successfully deleted the chunk: "+chunkName);
//                }else {
//                    System.err.println("failed to deleted the chunk: "+chunkName);
//                }
//            }
//
//            System.out.println("merged all the chunks successfully");
//
//        } catch (Exception e) {
//            System.err.println("Error merging chunks: " + e.getMessage());
//            return new ResponseEntity<>(new ResumeResponseEntity("Error merging chunks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        // ✅ Verify merged file size
//        long expectedSize = Long.parseLong(resumeSize);
//        System.out.println("🔹 Expected merged file size: " + expectedSize + " bytes");
//        System.out.println("🔹 Actual merged file size: " + finalFile.length() + " bytes");
//
//        if (finalFile.length() != expectedSize) {
//            System.err.println("❌ File size mismatch! Expected: " + expectedSize + " bytes, Got: " + finalFile.length() + " bytes");
//            return new ResponseEntity<>(new ResumeResponseEntity("File size mismatch", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
//        }

        // ✅ Upload to Supabase
        File userDir = new File(UPLOAD_DIR + File.separator + userId);
        if (!userDir.exists()) {
            return new ResponseEntity<>(new ResumeResponseEntity("User directory not found", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
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
        System.out.println("🔹 Expected merged file size: " + expectedSize + " bytes");
        System.out.println("🔹 Actual merged file size: " + finalFile.length() + " bytes");
        if (finalFile.length() != expectedSize) {
            return new ResponseEntity<>(new ResumeResponseEntity("File size mismatch! Expected: " + expectedSize + " bytes, Got: " + finalFile.length() + " bytes", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        System.out.println("✔ Merged all chunks successfully: " + finalFile.getAbsolutePath());

        String uniqueFileName = userService.generateUniqueFileName(resumeName);
        String supabaseUploadedFileUrl;
        try {
            supabaseUploadedFileUrl = uploadToSupabase(uniqueFileName, finalFile);
            System.out.println("✔ Uploaded file to Supabase: " + supabaseUploadedFileUrl);
        } catch (IOException e) {
            return new ResponseEntity<>(new ResumeResponseEntity("Error uploading to Supabase: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ✅ Delete the merged file after successful upload
        if (finalFile.delete()) {
            System.out.println("✔ Deleted local merged file: " + finalFile.getAbsolutePath());
        }


        if (userDir.delete()) {
            System.out.println("✔ Deleted user dir: " + userDir.getAbsolutePath());
        }

        // ✅ Store file details in the database
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
        //https://ynsrmwwmlwmagvanssnx.supabase.co/storage/v1/object/public/resumes//JobNet_final_report_1740993747623.pdf
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
//            System.out.println("response from supabase: "+response);
            String uploadedUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + fileName;
            System.out.println("Upload successful: " + uploadedUrl);
            return uploadedUrl;
        } else {
            System.err.println("Supabase upload failed: " + response.getBody());
            throw new IOException("Failed to upload to Supabase: " + response.getBody());
        }
    }
}
