package com.aicodementor.repository;

import com.aicodementor.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    
    Optional<Exercise> findByTitle(String title);
    
    Page<Exercise> findByTopic(String topic, Pageable pageable);
    
    Page<Exercise> findByDifficulty(Exercise.DifficultyLevel difficulty, Pageable pageable);
    
    @Query("SELECT e FROM Exercise e WHERE e.title LIKE %:keyword% OR e.description LIKE %:keyword% OR e.topic LIKE %:keyword%")
    Page<Exercise> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT DISTINCT e.topic FROM Exercise e ORDER BY e.topic")
    List<String> findAllTopics();
    
    @Query("SELECT e FROM Exercise e ORDER BY e.createdAt DESC")
    Page<Exercise> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT e FROM Exercise e WHERE e.isPublished = true ORDER BY e.createdAt DESC")
    Page<Exercise> findByIsPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT e FROM Exercise e WHERE e.isPublished = true AND e.creator IS NOT NULL ORDER BY e.createdAt DESC")
    Page<Exercise> findByIsPublishedTrueAndCreatorNotNullOrderByCreatedAtDesc(Pageable pageable);
    
    // Use separate count query to avoid issues with JOIN FETCH in pagination
    @Query(value = "SELECT e FROM Exercise e LEFT JOIN FETCH e.creator WHERE e.creator IS NOT NULL ORDER BY e.createdAt DESC",
           countQuery = "SELECT COUNT(e) FROM Exercise e WHERE e.creator IS NOT NULL")
    Page<Exercise> findByCreatorNotNullOrderByCreatedAtDesc(Pageable pageable);
    
    @Query(value = "SELECT DISTINCT e FROM Exercise e LEFT JOIN FETCH e.creator WHERE (e.title LIKE %:keyword% OR e.description LIKE %:keyword% OR e.topic LIKE %:keyword%) AND e.creator IS NOT NULL ORDER BY e.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT e) FROM Exercise e WHERE (e.title LIKE %:keyword% OR e.description LIKE %:keyword% OR e.topic LIKE %:keyword%) AND e.creator IS NOT NULL")
    Page<Exercise> findByKeywordAndCreatorNotNull(@Param("keyword") String keyword, Pageable pageable);
    
    @Query(value = "SELECT DISTINCT e FROM Exercise e LEFT JOIN FETCH e.creator WHERE e.topic = :topic AND e.creator IS NOT NULL ORDER BY e.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT e) FROM Exercise e WHERE e.topic = :topic AND e.creator IS NOT NULL")
    Page<Exercise> findByTopicAndCreatorNotNull(@Param("topic") String topic, Pageable pageable);
    
    @Query(value = "SELECT DISTINCT e FROM Exercise e LEFT JOIN FETCH e.creator WHERE e.difficulty = :difficulty AND e.creator IS NOT NULL ORDER BY e.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT e) FROM Exercise e WHERE e.difficulty = :difficulty AND e.creator IS NOT NULL")
    Page<Exercise> findByDifficultyAndCreatorNotNull(@Param("difficulty") Exercise.DifficultyLevel difficulty, Pageable pageable);
    
    List<Exercise> findByCreatorIsNull();
}

