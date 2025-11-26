package com.aicodementor.repository;

import com.aicodementor.entity.Submission;
import com.aicodementor.entity.User;
import com.aicodementor.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
    List<Submission> findByUser(User user);
    
    List<Submission> findByExercise(Exercise exercise);
    
    List<Submission> findByUserAndExercise(User user, Exercise exercise);
    
    @Query("SELECT s FROM Submission s WHERE s.user = :user AND s.exercise = :exercise ORDER BY s.createdAt DESC")
    List<Submission> findLatestSubmissionsByUserAndExercise(@Param("user") User user, @Param("exercise") Exercise exercise);
    
    @Query("SELECT s FROM Submission s WHERE s.exercise = :exercise ORDER BY s.createdAt DESC")
    Page<Submission> findByExerciseOrderByCreatedAtDesc(@Param("exercise") Exercise exercise, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user = :user AND s.status = 'COMPLETED'")
    Long countCompletedSubmissionsByUser(@Param("user") User user);
    
    long countByStatus(Submission.SubmissionStatus status);
    
    long countByUserIdAndStatus(Long userId, Submission.SubmissionStatus status);
}









