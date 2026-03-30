package com.saif.JobNet.controller;

import com.saif.JobNet.model.ApplyJobRequest;
import com.saif.JobNet.model.ApplicationStatus;
import com.saif.JobNet.model.JobApplication;
import com.saif.JobNet.model.UpdateApplicationStatusRequest;
import com.saif.JobNet.services.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody ApplyJobRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity = authentication == null ? null : authentication.getName();

        Optional<JobApplication> result = applicationService.apply(request, identity);
        if (result.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Invalid request, user/job not found, or unauthorized"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(result.get(), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<?> myApplications(@RequestParam("userId") String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity = authentication == null ? null : authentication.getName();

        List<JobApplication> applications = applicationService.getMyApplications(userId, identity);
        return new ResponseEntity<>(applications, HttpStatus.OK);
    }

    @GetMapping("/me/job/{jobId}")
    public ResponseEntity<?> myApplicationForJob(@RequestParam("userId") String userId, @PathVariable String jobId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity = authentication == null ? null : authentication.getName();

        return applicationService.getMyApplicationByJob(userId, jobId, identity)
                .<ResponseEntity<?>>map(jobApplication -> new ResponseEntity<>(jobApplication, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable("id") String applicationId,
                                          @RequestBody UpdateApplicationStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity = authentication == null ? null : authentication.getName();
        ApplicationStatus status = request == null ? null : request.getStatus();
        return applicationService.updateStatus(applicationId, status, identity)
                .<ResponseEntity<?>>map(jobApplication -> new ResponseEntity<>(jobApplication, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(Map.of("error", "Application not found, forbidden, or invalid status"), HttpStatus.BAD_REQUEST));
    }
}
