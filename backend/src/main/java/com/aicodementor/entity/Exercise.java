package com.aicodementor.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "exercises")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Exercise {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "Description is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @NotBlank(message = "Topic is required")
    @Size(max = 100, message = "Topic must not exceed 100 characters")
    @Column(nullable = false)
    private String topic;
    
    @NotNull(message = "Difficulty level is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;
    
    @NotBlank(message = "Problem statement is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String problemStatement;
    
    @Column(columnDefinition = "TEXT")
    private String hints;
    
    @Column(columnDefinition = "TEXT")
    private String examples;
    
    @Column(columnDefinition = "TEXT")
    private String testCases;
    
    @Column(columnDefinition = "TEXT")
    private String solution;
    
    @Column(columnDefinition = "TEXT")
    private String starterCode;
    
    @Column(columnDefinition = "TEXT")
    private String unitTests;
    
    @Column(columnDefinition = "TEXT")
    private String concepts;
    
    @Column(name = "is_published")
    private boolean isPublished = false;
    
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @JsonIgnoreProperties({"createdExercises", "submissions", "password", "hibernateLazyInitializer", "handler"})
    private User creator;
    
    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Submission> submissions = new ArrayList<>();
    
    public Exercise() {
    }
    
    public Exercise(Long id, String title, String description, String topic, DifficultyLevel difficulty, String problemStatement, String hints, String examples, String testCases, String solution, String starterCode, String unitTests, String concepts, boolean isPublished, LocalDateTime createdAt, LocalDateTime updatedAt, User creator, List<Submission> submissions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.difficulty = difficulty;
        this.problemStatement = problemStatement;
        this.hints = hints;
        this.examples = examples;
        this.testCases = testCases;
        this.solution = solution;
        this.starterCode = starterCode;
        this.unitTests = unitTests;
        this.concepts = concepts;
        this.isPublished = isPublished;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.creator = creator;
        this.submissions = submissions != null ? submissions : new ArrayList<>();
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
    
    // Custom getter for JSON serialization
    @JsonProperty("published")
    public boolean getPublished() {
        return isPublished;
    }
    
    @JsonProperty("published")
    public void setPublished(boolean published) {
        this.isPublished = published;
    }
    
    @JsonProperty("isPublished")
    public void setIsPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }
    
    public boolean isPublished() {
        return isPublished;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public DifficultyLevel getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getProblemStatement() {
        return problemStatement;
    }
    
    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }
    
    public String getHints() {
        return hints;
    }
    
    public void setHints(String hints) {
        this.hints = hints;
    }
    
    public String getExamples() {
        return examples;
    }
    
    public void setExamples(String examples) {
        this.examples = examples;
    }
    
    public String getTestCases() {
        return testCases;
    }
    
    public void setTestCases(String testCases) {
        this.testCases = testCases;
    }
    
    public String getSolution() {
        return solution;
    }
    
    public void setSolution(String solution) {
        this.solution = solution;
    }
    
    public String getStarterCode() {
        return starterCode;
    }
    
    public void setStarterCode(String starterCode) {
        this.starterCode = starterCode;
    }
    
    public String getUnitTests() {
        return unitTests;
    }
    
    public void setUnitTests(String unitTests) {
        this.unitTests = unitTests;
    }
    
    public String getConcepts() {
        return concepts;
    }
    
    public void setConcepts(String concepts) {
        this.concepts = concepts;
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
    
    public User getCreator() {
        return creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
    }
    
    public List<Submission> getSubmissions() {
        return submissions;
    }
    
    public void setSubmissions(List<Submission> submissions) {
        this.submissions = submissions;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exercise exercise = (Exercise) o;
        return Objects.equals(id, exercise.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Exercise{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", topic='" + topic + '\'' +
                ", difficulty=" + difficulty +
                ", problemStatement='" + problemStatement + '\'' +
                ", hints='" + hints + '\'' +
                ", examples='" + examples + '\'' +
                ", testCases='" + testCases + '\'' +
                ", solution='" + solution + '\'' +
                ", starterCode='" + starterCode + '\'' +
                ", unitTests='" + unitTests + '\'' +
                ", concepts='" + concepts + '\'' +
                ", isPublished=" + isPublished +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    public enum DifficultyLevel {
        L1("L1 - Licence 1"),
        L2("L2 - Licence 2"),
        L3("L3 - Licence 3"),
        M1("M1 - Master 1"),
        M2("M2 - Master 2");
        
        private final String displayName;
        
        DifficultyLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
