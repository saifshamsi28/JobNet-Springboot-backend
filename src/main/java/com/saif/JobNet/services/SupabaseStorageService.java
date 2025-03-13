package com.saif.JobNet.services;

import com.saif.JobNet.model.JobNetResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class SupabaseStorageService {
    private final String SUPABASE_URL = System.getenv("SUPABASE_URL");
    private final String SUPABASE_RESUME_BUCKET = System.getenv("SUPABASE_RESUME_BUCKET");
    private final String SUPABASE_PROFILE_BUCKET = System.getenv("SUPABASE_PROFILE_BUCKET");
    private final String SUPABASE_SERVICE_ROLE_KEY = System.getenv("SUPABASE_SERVICE_ROLE_KEY");

    public JobNetResponse uploadToSupabase(String fileName, File file, String fileType) throws IOException {
        String uploadUrl;
        if(fileType.contains("resume"))
            uploadUrl= SUPABASE_URL + "/storage/v1/object/" + SUPABASE_RESUME_BUCKET + "/" + fileName;
        else
            uploadUrl= SUPABASE_URL + "/storage/v1/object/" + SUPABASE_PROFILE_BUCKET + "/" + fileName;
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

        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, requestEntity, String.class);

        String uploadedUrl;
        if (response.getStatusCode() == HttpStatus.OK) {
            if(fileType.contains("resume"))
                uploadedUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_RESUME_BUCKET + "/" + fileName;
            else
                uploadedUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_PROFILE_BUCKET + "/" + fileName;
            System.out.println("Upload successful: " + uploadedUrl);
            return new JobNetResponse(uploadedUrl,HttpStatus.OK.value());
        } else {
            System.err.println("Supabase upload failed: " + response.getBody());
            return new JobNetResponse("Failed to upload to Supabase: " + response.getBody(), response.getStatusCode().value());
        }
    }
}
