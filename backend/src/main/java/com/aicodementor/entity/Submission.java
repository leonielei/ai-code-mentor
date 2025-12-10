package com.aicodementor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "submissions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Submission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"createdExercises", "submissions", "password", "hibernateLazyInitializer", "handler"})
    private User user;
    
    @NotNull(message = "Exercise is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    @JsonIgnoreProperties({"submissions", "creator", "hibernateLazyInitializer", "handler"})
    private Exercise exercise;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;
    
    @Column(columnDefinition = "TEXT")
    private String output;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;
    
    @Column(name = "execution_time")
    private Long executionTime; // in milliseconds
    
    @Column(name = "memory_usage")
    private Long memoryUsage; // in bytes
    
    @Column(name = "test_cases_passed")
    private Integer testCasesPassed;
    
    @Column(name = "total_test_cases")
    private Integer totalTestCases;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Submission() {
    }
    
    public Submission(Long id, User user, Exercise exercise, String code, String output, String errorMessage, SubmissionStatus status, Long executionTime, Long memoryUsage, Integer testCasesPassed, Integer totalTestCases, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.exercise = exercise;
        this.code = code;
        this.output = output;
        this.errorMessage = errorMessage;
        this.status = status;
        this.executionTime = executionTime;
        this.memoryUsage = memoryUsage;
        this.testCasesPassed = testCasesPassed;
        this.totalTestCases = totalTestCases;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Exercise getExercise() {
        return exercise;
    }
    
    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public SubmissionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }
    
    public Long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
    
    public Long getMemoryUsage() {
        return memoryUsage;
    }
    
    public void setMemoryUsage(Long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
    
    public Integer getTestCasesPassed() {
        return testCasesPassed;
    }
    
    public void setTestCasesPassed(Integer testCasesPassed) {
        this.testCasesPassed = testCasesPassed;
    }
    
    public Integer getTotalTestCases() {
        return totalTestCases;
    }
    
    public void setTotalTestCases(Integer totalTestCases) {
        this.totalTestCases = totalTestCases;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Submission{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", output='" + output + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", status=" + status +
                ", executionTime=" + executionTime +
                ", memoryUsage=" + memoryUsage +
                ", testCasesPassed=" + testCasesPassed +
                ", totalTestCases=" + totalTestCases +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    public enum SubmissionStatus {
        SUBMITTED("Submitted"), 
        PENDING("Pending"),
        RUNNING("Running"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        TIME_LIMIT_EXCEEDED("Time Limit Exceeded"),
        MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded"),
        COMPILATION_ERROR("Compilation Error"),
        RUNTIME_ERROR("Runtime Error");
        
        private final String displayName;
        
        SubmissionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
