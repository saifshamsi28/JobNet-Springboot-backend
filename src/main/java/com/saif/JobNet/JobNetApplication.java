package com.saif.JobNet;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.repositories.JobsRepository;
import com.saif.JobNet.services.JobsEntryService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class JobNetApplication {
	public static void main(String[] args) {
		SpringApplication.run(JobNetApplication.class, args);
		System.out.println("HOST: " + System.getenv("HOST"));
		System.out.println("PORT: " + System.getenv("PORT"));
		System.out.println("DATABASE: " + System.getenv("DATABASE"));

	}
}
