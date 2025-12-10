package com.aicodementor.controller;

import com.aicodementor.entity.Submission;
import com.aicodementor.entity.User;
import com.aicodementor.entity.Exercise;
import com.aicodementor.repository.SubmissionRepository;
import com.aicodementor.repository.UserRepository;
import com.aicodementor.repository.ExerciseRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SubmissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Submission> submissions = submissionRepository.findAll(pageable);
            
            // Ensure lazy-loaded associations are initialized
            submissions.getContent().forEach(sub -> {
                if (sub.getUser() != null) {
                    Hibernate.initialize(sub.getUser());
                    sub.getUser().getId();
                    sub.getUser().getUsername();
                }
                if (sub.getExercise() != null) {
                    Hibernate.initialize(sub.getExercise());
                    sub.getExercise().getId();
                    sub.getExercise().getTitle();
                }
            });
            
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            System.err.println("Error in getAllSubmissions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des soumissions: " + 
                        (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
    
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSubmissionById(@PathVariable Long id) {
        try {
            Optional<Submission> submissionOpt = submissionRepository.findById(id);
            if (submissionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Submission submission = submissionOpt.get();
            
            // Ensure lazy-loaded associations are initialized
            if (submission.getUser() != null) {
                Hibernate.initialize(submission.getUser());
                submission.getUser().getId();
                submission.getUser().getUsername();
            }
            if (submission.getExercise() != null) {
                Hibernate.initialize(submission.getExercise());
                submission.getExercise().getId();
                submission.getExercise().getTitle();
            }
            
            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            System.err.println("Error in getSubmissionById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération de la soumission: " + 
                        (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createSubmission(@Valid @RequestBody Submission submission) {
        try {
            // Verify user and exercise exist
            if (submission.getUser() == null || submission.getUser().getId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("User ID is required");
            }
            if (submission.getExercise() == null || submission.getExercise().getId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Exercise ID is required");
            }
            
            Optional<User> userOpt = userRepository.findById(submission.getUser().getId());
            Optional<Exercise> exercise = exerciseRepository.findById(submission.getExercise().getId());
            
            // If user doesn't exist, create a default student user
            User user;
            if (userOpt.isEmpty()) {
                logger.warn("User not found with ID: {}, creating default student user", submission.getUser().getId());
                
                // Generate unique username and email
                String baseUsername = "student_" + submission.getUser().getId();
                String baseEmail = "student" + submission.getUser().getId() + "@demo.com";
                
                // Check if username or email already exists, if so, find or create with different suffix
                String username = baseUsername;
                String email = baseEmail;
                int suffix = 0;
                
                while (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
                    suffix++;
                    username = baseUsername + "_" + suffix;
                    email = "student" + submission.getUser().getId() + "_" + suffix + "@demo.com";
                }
                
                // Create a default student user
                User defaultUser = new User();
                defaultUser.setUsername(username);
                defaultUser.setEmail(email);
                defaultUser.setPassword("demo123");
                defaultUser.setFullName("Student " + submission.getUser().getId() + (suffix > 0 ? " (" + suffix + ")" : ""));
                defaultUser.setRole(User.UserRole.STUDENT);
                defaultUser.setCreatedAt(LocalDateTime.now());
                defaultUser.setUpdatedAt(LocalDateTime.now());
                
                try {
                    user = userRepository.save(defaultUser);
                    logger.info("Created default student user with ID: {}, username: {}", user.getId(), username);
                } catch (Exception e) {
                    logger.error("Failed to create default student user: {}", e.getMessage(), e);
                    // Try to find existing user by username as fallback
                    Optional<User> existingUser = userRepository.findByUsername(baseUsername);
                    if (existingUser.isPresent()) {
                        user = existingUser.get();
                        logger.info("Using existing user with ID: {}, username: {}", user.getId(), user.getUsername());
                    } else {
                        throw new RuntimeException("Failed to create or find user: " + e.getMessage(), e);
                    }
                }
            } else {
                user = userOpt.get();
            }
            
            if (exercise.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Exercise not found with ID: " + submission.getExercise().getId());
            }
            
            submission.setUser(user);
            submission.setExercise(exercise.get());
            
            // Set default status if not provided
            if (submission.getStatus() == null) {
                submission.setStatus(Submission.SubmissionStatus.PENDING);
            }
            
            // Ensure code is not null
            if (submission.getCode() == null || submission.getCode().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Code cannot be empty");
            }
            
            Submission savedSubmission = submissionRepository.save(submission);
            logger.info("Submission created successfully with ID: {} for user: {}, exercise: {}", 
                savedSubmission.getId(), user.getId(), exercise.get().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSubmission);
        } catch (Exception e) {
            logger.error("Error creating submission: {}", e.getMessage(), e);
            e.printStackTrace();
            String errorMessage = "Error creating submission: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            // Check for specific constraint violations
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                errorMessage = "Database constraint violation: " + e.getMessage();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMessage);
        }
    }
    
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateSubmission(@PathVariable Long id, @Valid @RequestBody Submission submissionDetails) {
        try {
            Optional<Submission> optionalSubmission = submissionRepository.findById(id);
            
            if (optionalSubmission.isEmpty()) {
                logger.warn("Submission not found for update: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Submission not found with ID: " + id);
            }
            
            Submission submission = optionalSubmission.get();
            
            // Validate code is not empty
            if (submissionDetails.getCode() != null) {
                if (submissionDetails.getCode().trim().isEmpty()) {
                    logger.warn("Attempted to update submission {} with empty code", id);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Code cannot be empty");
                }
                submission.setCode(submissionDetails.getCode().trim());
                logger.debug("Updated submission {} code, length: {}", id, submission.getCode().length());
            }
            
            // Update other fields if provided
            if (submissionDetails.getOutput() != null) {
                submission.setOutput(submissionDetails.getOutput());
            }
            if (submissionDetails.getErrorMessage() != null) {
                submission.setErrorMessage(submissionDetails.getErrorMessage());
            }
            if (submissionDetails.getStatus() != null) {
                submission.setStatus(submissionDetails.getStatus());
            }
            if (submissionDetails.getExecutionTime() != null) {
                submission.setExecutionTime(submissionDetails.getExecutionTime());
            }
            if (submissionDetails.getMemoryUsage() != null) {
                submission.setMemoryUsage(submissionDetails.getMemoryUsage());
            }
            if (submissionDetails.getTestCasesPassed() != null) {
                submission.setTestCasesPassed(submissionDetails.getTestCasesPassed());
            }
            if (submissionDetails.getTotalTestCases() != null) {
                submission.setTotalTestCases(submissionDetails.getTotalTestCases());
            }
            
            Submission updatedSubmission = submissionRepository.save(submission);
            logger.info("Submission updated successfully - ID: {}, User: {}, Exercise: {}, Code length: {}", 
                updatedSubmission.getId(),
                updatedSubmission.getUser() != null ? updatedSubmission.getUser().getId() : "null",
                updatedSubmission.getExercise() != null ? updatedSubmission.getExercise().getId() : "null",
                updatedSubmission.getCode() != null ? updatedSubmission.getCode().length() : 0);
            
            return ResponseEntity.ok(updatedSubmission);
        } catch (Exception e) {
            logger.error("Error updating submission {}: {}", id, e.getMessage(), e);
            e.printStackTrace();
            String errorMessage = "Error updating submission: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMessage);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        if (submissionRepository.existsById(id)) {
            submissionRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSubmissionsByUser(@PathVariable Long userId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<Submission> submissions = submissionRepository.findByUser(user.get());
            
            // Ensure lazy-loaded associations are initialized
            submissions.forEach(sub -> {
                if (sub.getExercise() != null) {
                    Hibernate.initialize(sub.getExercise());
                    sub.getExercise().getId();
                    sub.getExercise().getTitle();
                }
            });
            
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            System.err.println("Error in getSubmissionsByUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des soumissions: " + 
                        (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
    
    @GetMapping("/exercise/{exerciseId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSubmissionsByExercise(
            @PathVariable Long exerciseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Optional<Exercise> exercise = exerciseRepository.findById(exerciseId);
            if (exercise.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Submission> submissions = submissionRepository.findByExerciseOrderByCreatedAtDesc(exercise.get(), pageable);
            
            // Ensure lazy-loaded associations are initialized
            submissions.getContent().forEach(sub -> {
                if (sub.getUser() != null) {
                    Hibernate.initialize(sub.getUser());
                    sub.getUser().getId();
                    sub.getUser().getUsername();
                }
            });
            
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            System.err.println("Error in getSubmissionsByExercise: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des soumissions: " + 
                        (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
    
    @GetMapping("/user/{userId}/exercise/{exerciseId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSubmissionsByUserAndExercise(
            @PathVariable Long userId,
            @PathVariable Long exerciseId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            Optional<Exercise> exercise = exerciseRepository.findById(exerciseId);
            
            if (user.isEmpty() || exercise.isEmpty()) {
                // Return empty list instead of 404 for better UX
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            
            // Use method that orders by createdAt DESC to get latest first
            List<Submission> submissions = submissionRepository.findLatestSubmissionsByUserAndExercise(user.get(), exercise.get());
            
            // Ensure lazy-loaded associations are initialized
            submissions.forEach(sub -> {
                if (sub.getExercise() != null) {
                    Hibernate.initialize(sub.getExercise());
                    sub.getExercise().getId();
                    sub.getExercise().getTitle();
                }
            });
            
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            logger.error("Error in getSubmissionsByUserAndExercise: {}", e.getMessage(), e);
            e.printStackTrace();
            // Return empty list on error instead of 500, for better UX
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }
}









