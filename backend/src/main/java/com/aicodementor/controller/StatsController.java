package com.aicodementor.controller;

import com.aicodementor.repository.ExerciseRepository;
import com.aicodementor.repository.SubmissionRepository;
import com.aicodementor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class StatsController {
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get dashboard statistics for teachers
     */
    @GetMapping("/teacher")
    public ResponseEntity<Map<String, Object>> getTeacherStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Only count real exercises (with creator)
        long totalExercises = exerciseRepository.findByCreatorNotNullOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        ).getTotalElements();
        
        // Count active students (users with role STUDENT)
        long activeStudents = userRepository.countByRole(
            com.aicodementor.entity.User.UserRole.STUDENT
        );
        
        // Count total submissions
        long totalSubmissions = submissionRepository.count();
        
        // Calculate success rate (submissions with status COMPLETED)
        long passedSubmissions = submissionRepository.countByStatus(
            com.aicodementor.entity.Submission.SubmissionStatus.COMPLETED
        );
        double successRate = totalSubmissions > 0 
            ? (double) passedSubmissions / totalSubmissions * 100 
            : 0.0;
        
        stats.put("totalExercises", totalExercises);
        stats.put("activeStudents", activeStudents);
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("successRate", Math.round(successRate));
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get dashboard statistics for students
     */
    @GetMapping("/student/{userId}")
    public ResponseEntity<Map<String, Object>> getStudentStats(@PathVariable Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Count completed exercises for this student
        long completedExercises = submissionRepository.countByUserIdAndStatus(
            userId, 
            com.aicodementor.entity.Submission.SubmissionStatus.COMPLETED
        );
        
        // Count total published exercises
        long totalExercises = exerciseRepository.findByIsPublishedTrueAndCreatorNotNullOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        ).getTotalElements();
        
        // Calculate points (simplified: 10 points per completed exercise)
        long points = completedExercises * 10;
        
        // Count consecutive days (simplified: return 0 for now)
        long consecutiveDays = 0;
        
        // Calculate ranking (simplified: return 0 for now)
        long ranking = 0;
        
        stats.put("completedExercises", completedExercises);
        stats.put("totalExercises", totalExercises);
        stats.put("points", points);
        stats.put("consecutiveDays", consecutiveDays);
        stats.put("ranking", ranking);
        
        return ResponseEntity.ok(stats);
    }
}

