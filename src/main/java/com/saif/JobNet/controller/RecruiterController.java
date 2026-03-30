package com.saif.JobNet.controller;

import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.JobApplication;
import com.saif.JobNet.model.JobStatus;
import com.saif.JobNet.model.RecruiterJobCreateRequest;
import com.saif.JobNet.model.UpdateRecruiterJobStatusRequest;
import com.saif.JobNet.model.AuthResponse;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserRole;
import com.saif.JobNet.services.ApplicationService;
import com.saif.JobNet.services.JWTService;
import com.saif.JobNet.services.JobsService;
import com.saif.JobNet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/recruiter")
public class RecruiterController {

    @Autowired
    private JobsService jobsService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private ApplicationService applicationService;

    @PostMapping("/jobs")
    public ResponseEntity<?> createJob(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestBody RecruiterJobCreateRequest request) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can post jobs"), HttpStatus.FORBIDDEN);
        }

        try {
            return jobsService.createRecruiterJob(user, request)
                    .<ResponseEntity<?>>map(job -> new ResponseEntity<>(job, HttpStatus.CREATED))
                    .orElseGet(() -> new ResponseEntity<>(new AuthResponse("Invalid job payload", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST));
        } catch (DuplicateKeyException ex) {
            return new ResponseEntity<>(new AuthResponse("Could not create job due to duplicate data. Please try again.", HttpStatus.CONFLICT.value()), HttpStatus.CONFLICT);
        } catch (Exception ex) {
            return new ResponseEntity<>(new AuthResponse("Failed to create job right now. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/jobs/me")
    public ResponseEntity<?> myJobs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can view recruiter jobs"), HttpStatus.FORBIDDEN);
        }

        List<Job> jobs = jobsService.getJobsPostedByRecruiter(user.getId());
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<?> jobApplicants(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @PathVariable("jobId") String jobId) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can view applicants"), HttpStatus.FORBIDDEN);
        }

        List<JobApplication> applications = applicationService.getRecruiterApplicationsForJob(jobId, user.getId());
        return new ResponseEntity<>(applications, HttpStatus.OK);
    }

    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<?> updateJob(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @PathVariable("jobId") String jobId,
                                       @RequestBody RecruiterJobCreateRequest request) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can edit jobs"), HttpStatus.FORBIDDEN);
        }

        try {
            return jobsService.updateRecruiterJob(user, jobId, request)
                    .<ResponseEntity<?>>map(job -> new ResponseEntity<>(job, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(new AuthResponse("Invalid payload or job not found", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST));
        } catch (Exception ex) {
            return new ResponseEntity<>(new AuthResponse("Failed to update job right now. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/jobs/{jobId}/status")
    public ResponseEntity<?> updateJobStatus(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                             @PathVariable("jobId") String jobId,
                                             @RequestBody UpdateRecruiterJobStatusRequest request) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can change job status"), HttpStatus.FORBIDDEN);
        }

        JobStatus status = request == null ? null : request.getStatus();
        if (status == null) {
            return new ResponseEntity<>(new AuthResponse("Status is required", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }

        try {
            return jobsService.updateRecruiterJobStatus(user, jobId, status)
                    .<ResponseEntity<?>>map(job -> new ResponseEntity<>(job, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(new AuthResponse("Job not found or access denied", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND));
        } catch (Exception ex) {
            return new ResponseEntity<>(new AuthResponse("Failed to update job status. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<?> deleteJob(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @PathVariable("jobId") String jobId) {
        Optional<User> authenticatedUser = resolveAuthenticatedUser(authHeader);
        if (authenticatedUser.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        User user = authenticatedUser.get();
        if (user.getRole() != UserRole.RECRUITER) {
            return new ResponseEntity<>(Map.of("error", "Only recruiters can delete jobs"), HttpStatus.FORBIDDEN);
        }

        try {
            boolean deleted = jobsService.deleteRecruiterJob(user, jobId);
            if (!deleted) {
                return new ResponseEntity<>(new AuthResponse("Job not found or access denied", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(Map.of("message", "Job deleted successfully"), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new AuthResponse("Failed to delete job right now. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Optional<User> resolveAuthenticatedUser(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            String token = authHeader.substring(7);
            String identity = jwtService.extractUserName(token);
            return userService.getUserByIdentity(identity);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
