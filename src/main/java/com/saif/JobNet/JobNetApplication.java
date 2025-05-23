package com.saif.JobNet;

import com.saif.JobNet.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JobNetApplication {
	public static void main(String[] args) {
		SpringApplication.run(JobNetApplication.class, args);
	}
}
