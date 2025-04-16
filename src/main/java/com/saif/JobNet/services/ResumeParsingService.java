package com.saif.JobNet.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ResumeParsingService {

    public static Map<String, Object> parseResumeWithFlask(File resumeFile) throws IOException {
        // Replace this with your actual Flask server URL
//        String flaskUrl = "http://10.162.1.53:5000/parse-resume";
        String flaskUrl = System.getenv("FLASK_BASE_URL");

        HttpPost post = new HttpPost(flaskUrl);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("resume", resumeFile, ContentType.DEFAULT_BINARY, resumeFile.getName());

        HttpEntity multipart = builder.build();
        post.setEntity(multipart);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String result = EntityUtils.toString(responseEntity);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(result, new TypeReference<Map<String, Object>>() {});
            } else {
                throw new IOException("No response from Flask server");
            }
        }
    }
}
