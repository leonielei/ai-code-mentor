package com.aicodementor.controller;

import com.aicodementor.entity.Submission;
import com.aicodementor.entity.User;
import com.aicodementor.entity.Exercise;
import com.aicodementor.repository.SubmissionRepository;
import com.aicodementor.repository.UserRepository;
import com.aicodementor.repository.ExerciseRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.hibernate.Hibernate;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SubmissionController {
    
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
    public ResponseEntity<Submission> createSubmission(@Valid @RequestBody Submission submission) {
        try {
            // Verify user and exercise exist
            Optional<User> user = userRepository.findById(submission.getUser().getId());
            Optional<Exercise> exercise = exerciseRepository.findById(submission.getExercise().getId());
            
            if (user.isEmpty() || exercise.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            submission.setUser(user.get());
            submission.setExercise(exercise.get());
            
            // Set default status if not provided
            if (submission.getStatus() == null) {
                submission.setStatus(Submission.SubmissionStatus.PENDING);
            }
            
            Submission savedSubmission = submissionRepository.save(submission);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSubmission);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Submission> updateSubmission(@PathVariable Long id, @Valid @RequestBody Submission submissionDetails) {
        Optional<Submission> optionalSubmission = submissionRepository.findById(id);
        
        if (optionalSubmission.isPresent()) {
            Submission submission = optionalSubmission.get();
            submission.setCode(submissionDetails.getCode());
            submission.setOutput(submissionDetails.getOutput());
            submission.setErrorMessage(submissionDetails.getErrorMessage());
            submission.setStatus(submissionDetails.getStatus());
            submission.setExecutionTime(submissionDetails.getExecutionTime());
            submission.setMemoryUsage(submissionDetails.getMemoryUsage());
            submission.setTestCasesPassed(submissionDetails.getTestCasesPassed());
            submission.setTotalTestCases(submissionDetails.getTotalTestCases());
            
            Submission updatedSubmission = submissionRepository.save(submission);
            return ResponseEntity.ok(updatedSubmission);
        } else {
            return ResponseEntity.notFound().build();
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
                return ResponseEntity.notFound().build();
            }
            
            List<Submission> submissions = submissionRepository.findByUserAndExercise(user.get(), exercise.get());
            
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
            System.err.println("Error in getSubmissionsByUserAndExercise: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des soumissions: " + 
                        (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}









