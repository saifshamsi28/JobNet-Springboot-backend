package com.saif.JobNet.services;

import com.saif.JobNet.model.ApplicationStatus;
import com.saif.JobNet.model.ApplyJobRequest;
import com.saif.JobNet.model.Job;
import com.saif.JobNet.model.JobApplication;
import com.saif.JobNet.model.JobStatus;
import com.saif.JobNet.model.User;
import com.saif.JobNet.model.UserRole;
import com.saif.JobNet.repositories.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JobsService jobsService;

    public Optional<JobApplication> apply(ApplyJobRequest request, String authenticatedIdentity) {
        if (request == null || isBlank(request.getUserId()) || isBlank(request.getJobId())) {
            return Optional.empty();
        }

        Optional<User> userBox = userService.getUserById(request.getUserId());
        Optional<Job> jobBox = jobsService.getJobById(request.getJobId());

        if (userBox.isEmpty() || jobBox.isEmpty()) {
            return Optional.empty();
        }

        User user = userBox.get();
        if (!isAllowedIdentity(authenticatedIdentity, user)) {
            return Optional.empty();
        }
        if (user.getRole() != null && user.getRole() != UserRole.JOB_SEEKER) {
            return Optional.empty();
        }

        Job job = jobBox.get();
        if (job.getStatus() != JobStatus.PUBLISHED) {
            return Optional.empty();
        }
        JobApplication application = applicationRepository
                .findByUserIdAndJobId(user.getId(), job.getId())
                .orElseGet(JobApplication::new);

        application.setUserId(user.getId());
        application.setJobId(job.getId());
        application.setJobTitle(defaultIfBlank(job.getTitle(), "Untitled Role"));
        application.setCompany(defaultIfBlank(job.getCompany(), "Unknown Company"));
        application.setRecruiterId(defaultIfBlank(job.getPostedByUserId(), null));
        application.setResumeUrl(defaultIfBlank(request.getResumeUrl(), null));
        application.setCoverLetter(defaultIfBlank(request.getCoverLetter(), null));
        application.setStatus(ApplicationStatus.APPLIED);
        if (application.getAppliedAt() == null) {
            application.setAppliedAt(LocalDateTime.now());
        }
        application.setUpdatedAt(LocalDateTime.now());

        return Optional.of(applicationRepository.save(application));
    }

    public List<JobApplication> getMyApplications(String userId, String authenticatedIdentity) {
        Optional<User> userBox = userService.getUserById(userId);
        if (userBox.isEmpty() || !isAllowedIdentity(authenticatedIdentity, userBox.get())) {
            return List.of();
        }
        return applicationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Optional<JobApplication> getMyApplicationByJob(String userId, String jobId, String authenticatedIdentity) {
        Optional<User> userBox = userService.getUserById(userId);
        if (userBox.isEmpty() || !isAllowedIdentity(authenticatedIdentity, userBox.get())) {
            return Optional.empty();
        }
        return applicationRepository.findByUserIdAndJobId(userId, jobId);
    }

    public Optional<JobApplication> updateStatus(String applicationId, ApplicationStatus status, String authenticatedIdentity) {
        if (isBlank(applicationId) || status == null) {
            return Optional.empty();
        }
        Optional<User> userBox = userService.getUserByIdentity(authenticatedIdentity);
        if (userBox.isEmpty()) {
            return Optional.empty();
        }
        User user = userBox.get();
        Optional<JobApplication> box = applicationRepository.findById(applicationId);
        if (box.isEmpty()) {
            return Optional.empty();
        }
        JobApplication application = box.get();

        if (user.getRole() == UserRole.RECRUITER) {
            if (!user.getId().equals(application.getRecruiterId())) {
                return Optional.empty();
            }
            // Recruiters can progress candidates but cannot withdraw on behalf of seekers.
            if (status == ApplicationStatus.WITHDRAWN) {
                return Optional.empty();
            }
        } else if (user.getRole() == UserRole.JOB_SEEKER) {
            if (status != ApplicationStatus.WITHDRAWN) {
                return Optional.empty();
            }
            if (!user.getId().equals(application.getUserId())) {
                return Optional.empty();
            }
            if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
                return Optional.of(application);
            }
        } else {
            return Optional.empty();
        }

        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        return Optional.of(applicationRepository.save(application));
    }

    public List<JobApplication> getRecruiterApplicationsForJob(String jobId, String recruiterId) {
        if (isBlank(jobId) || isBlank(recruiterId)) {
            return List.of();
        }

        List<JobApplication> applications = applicationRepository.findByJobIdOrderByUpdatedAtDesc(jobId);
        return applications.stream()
                .filter(app -> recruiterId.equals(app.getRecruiterId()))
                .toList();
    }

    private boolean isAllowedIdentity(String identity, User user) {
        if (isBlank(identity) || user == null) {
            return false;
        }
        return identity.equalsIgnoreCase(user.getUserName()) || identity.equalsIgnoreCase(user.getEmail());
    }

    private String defaultIfBlank(String value, String fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
