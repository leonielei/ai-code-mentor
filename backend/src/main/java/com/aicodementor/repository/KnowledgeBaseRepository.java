package com.aicodementor.repository;

import com.aicodementor.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    /**
     * 根据练习ID查找知识库条目
     */
    List<KnowledgeBase> findByExerciseId(Long exerciseId);
    
    /**
     * 根据内容类型查找
     */
    List<KnowledgeBase> findByContentType(String contentType);
    
    /**
     * 根据练习ID和内容类型查找
     */
    List<KnowledgeBase> findByExerciseIdAndContentType(Long exerciseId, String contentType);
}


