package com.aicodementor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 原始内容（问题、解决方案、提示等）
    
    @Column(nullable = false, length = 50)
    private String contentType; // "solution", "hint", "error_pattern", "concept", "exercise_example"
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON格式的元数据
    
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embeddingJson; // 存储为JSON字符串的向量
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    @JsonIgnoreProperties({"submissions", "creator", "hibernateLazyInitializer", "handler"})
    private Exercise exercise;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public KnowledgeBase() {
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public String getEmbeddingJson() {
        return embeddingJson;
    }
    
    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }
    
    public Exercise getExercise() {
        return exercise;
    }
    
    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
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
}


