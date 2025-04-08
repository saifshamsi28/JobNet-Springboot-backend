package com.saif.JobNet;

import com.saif.JobNet.model.Job;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
class JobNetApplicationTests {

	@Autowired
	private RestTemplate restTemplate;
	@Test
	void contextLoads() {
		String url="http://10.151.17.41:5000/home?job_title=android developer";

		try {
			url = url.trim();

			// Construct the Flask backend URL

//			System.out.println("Sending request to Flask: " + flaskEndpoint);

			// Call Flask backend using RestTemplate
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				List<Job> responseBody = (List<Job>) response.getBody();
				if (responseBody != null) {
//					if (responseBody.containsKey("full_description")) {
//						return (String) responseBody.get("full_description");
//					} else if (responseBody.containsKey("error")) {
//						return "Error from Flask: " + responseBody.get("error");
//					} else {
//						return "Unexpected response from Flask.";
//					}
					System.out.println(responseBody);
				} else {
					System.out.println( "Empty response from Flask.");
				}
			} else {
				throw new RuntimeException("Failed to fetch job description from Flask: " +
						response.getStatusCode());
			}
		} catch (Exception e) {
			// Handle exceptions gracefully
			System.err.println("Error communicating with Flask backend: " + e.getMessage());
//			return "Unable to fetch job description due to a backend issue.";
		}
	}

}
