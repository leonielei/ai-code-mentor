package com.aicodementor.repository;

import com.aicodementor.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    /**
     * Find knowledge base entries by exercise ID
     */
    List<KnowledgeBase> findByExerciseId(Long exerciseId);
    
    /**
     * Find knowledge base entries by content type
     */
    List<KnowledgeBase> findByContentType(String contentType);
    
    /**
     * Find knowledge base entries by exercise ID and content type
     */
    List<KnowledgeBase> findByExerciseIdAndContentType(Long exerciseId, String contentType);
}


