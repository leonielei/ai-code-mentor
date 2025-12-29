package com.aicodementor.controller;

import com.aicodementor.entity.Exercise;
import com.aicodementor.repository.ExerciseRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;

import java.lang.reflect.InaccessibleObjectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"})
public class ExerciseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExerciseController.class);
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<Exercise>> getAllExercises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Exercise.DifficultyLevel difficulty) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Exercise> exercises;
        
        // Only show real exercises (with creator), not demo data
        if (keyword != null && !keyword.trim().isEmpty()) {
            exercises = exerciseRepository.findByKeywordAndCreatorNotNull(keyword, pageable);
        } else if (topic != null && !topic.trim().isEmpty()) {
            exercises = exerciseRepository.findByTopicAndCreatorNotNull(topic, pageable);
        } else if (difficulty != null) {
            exercises = exerciseRepository.findByDifficultyAndCreatorNotNull(difficulty, pageable);
        } else {
            exercises = exerciseRepository.findByCreatorNotNullOrderByCreatedAtDesc(pageable);
        }
        
        logger.debug("Returning {} exercises", exercises.getContent().size());
        
        // Ensure all lazy-loaded associations are initialized before serialization
        // This must be done within the transaction
        exercises.getContent().forEach(ex -> {
            try {
                if (ex.getCreator() != null) {
                    // Force initialization of Hibernate proxy
                    Hibernate.initialize(ex.getCreator());
                    // Access creator fields to ensure it's loaded
                    ex.getCreator().getId();
                    ex.getCreator().getUsername();
                    ex.getCreator().getFullName(); // Ensure it's loaded
                    logger.debug("Exercise ID: {}, Title: {}, Creator ID: {}, Username: {}, Published: {}", 
                        ex.getId(), ex.getTitle(), ex.getCreator().getId(), 
                        ex.getCreator().getUsername(), ex.getPublished());
                } else {
                    logger.debug("Exercise ID: {}, Title: {}, Creator: null, Published: {}", 
                        ex.getId(), ex.getTitle(), ex.getPublished());
                }
            } catch (LazyInitializationException e) {
                logger.warn("Lazy initialization failed for exercise {} creator: {}", ex.getId(), e.getMessage());
            } catch (RuntimeException e) {
                logger.error("Error accessing creator for exercise {}", ex.getId(), e);
            }
        });
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(exercises);
    }
    
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Exercise> getExerciseById(@PathVariable Long id) {
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Exercise exercise = exerciseOpt.get();
        
        // Ensure lazy-loaded associations are initialized
        if (exercise.getCreator() != null) {
            // Force initialization of Hibernate proxy
            Hibernate.initialize(exercise.getCreator());
            // Access creator fields to ensure it's loaded
            exercise.getCreator().getId();
            exercise.getCreator().getUsername();
            exercise.getCreator().getFullName();
        }
        
        // Log exercise data for debugging (excluding large text fields)
        logger.debug("Exercise loaded - ID: {}, Title: {}, Has Solution: {}, Solution Length: {}, Has StarterCode: {}, Has Examples: {}", 
            exercise.getId(), exercise.getTitle(),
            exercise.getSolution() != null && !exercise.getSolution().isEmpty(),
            exercise.getSolution() != null ? exercise.getSolution().length() : 0,
            exercise.getStarterCode() != null && !exercise.getStarterCode().isEmpty(),
            exercise.getExamples() != null && !exercise.getExamples().isEmpty());
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(exercise);
    }
    
    @PostMapping
    public ResponseEntity<Exercise> createExercise(@Valid @RequestBody Exercise exercise) {
        try {
            Exercise savedExercise = exerciseRepository.save(exercise);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExercise);
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation when creating exercise: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (jakarta.validation.ConstraintViolationException e) {
            logger.error("Validation constraint violation when creating exercise: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Exercise> updateExercise(@PathVariable Long id, @Valid @RequestBody Exercise exerciseDetails) {
        Optional<Exercise> optionalExercise = exerciseRepository.findById(id);
        
        if (optionalExercise.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Exercise exercise = optionalExercise.get();
        exercise.setTitle(exerciseDetails.getTitle());
        exercise.setDescription(exerciseDetails.getDescription());
        exercise.setTopic(exerciseDetails.getTopic());
        exercise.setDifficulty(exerciseDetails.getDifficulty());
        exercise.setProblemStatement(exerciseDetails.getProblemStatement());
        exercise.setHints(exerciseDetails.getHints());
        exercise.setExamples(exerciseDetails.getExamples());
        exercise.setTestCases(exerciseDetails.getTestCases());
        exercise.setSolution(exerciseDetails.getSolution());
        exercise.setStarterCode(exerciseDetails.getStarterCode());
        exercise.setUnitTests(exerciseDetails.getUnitTests());
        exercise.setConcepts(exerciseDetails.getConcepts());
        
        // Update published status if provided
        // Jackson will deserialize "published" field to setPublished() method
        // or "isPublished" field to setIsPublished() method
        boolean shouldPublish = exerciseDetails.getPublished();
        
        // Also check if isPublished field was set directly (from "isPublished" JSON field)
        // This handles the case where Jackson calls setIsPublished() instead of setPublished()
        try {
            java.lang.reflect.Field field = exerciseDetails.getClass().getDeclaredField("isPublished");
            field.setAccessible(true);
            Object value = field.get(exerciseDetails);
            if (value instanceof Boolean && (Boolean) value) {
                shouldPublish = true;
                logger.debug("Found isPublished=true via reflection");
            }
        } catch (NoSuchFieldException e) {
            // Field doesn't exist, that's okay - use getPublished() result
            logger.debug("isPublished field not found via reflection, using getPublished()");
        } catch (IllegalAccessException | InaccessibleObjectException | SecurityException e) {
            // Field access failed, that's okay - use getPublished() result
            logger.debug("Could not access isPublished field via reflection: {}", e.getMessage());
        }
        
        logger.debug("Exercise {} - getPublished()={}, current exercise.isPublished={}", 
            exercise.getId(), shouldPublish, exercise.getPublished());
        
        if (shouldPublish) {
            exercise.setPublished(true);
            logger.debug("Setting exercise {} to published", exercise.getId());
        } else {
            logger.debug("Exercise {} will remain unpublished", exercise.getId());
        }
        
        Exercise updatedExercise = exerciseRepository.save(exercise);
        
        // Flush to ensure the update is persisted immediately
        exerciseRepository.flush();
        
        // Reload to verify the update
        Exercise verifiedExercise = exerciseRepository.findById(updatedExercise.getId()).orElse(updatedExercise);
        logger.debug("Exercise {} published status: {}", verifiedExercise.getId(), verifiedExercise.getPublished());
        
        // Ensure lazy-loaded associations are initialized
        if (verifiedExercise.getCreator() != null) {
            Hibernate.initialize(verifiedExercise.getCreator());
            verifiedExercise.getCreator().getId();
            verifiedExercise.getCreator().getUsername();
            verifiedExercise.getCreator().getFullName();
        }
        
        return ResponseEntity.ok(verifiedExercise);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExercise(@PathVariable Long id) {
        if (exerciseRepository.existsById(id)) {
            exerciseRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getAllTopics() {
        List<String> topics = exerciseRepository.findAllTopics();
        return ResponseEntity.ok(topics);
    }
    
    /**
     * Delete all exercises (clears demo data and real data)
     * Use this to remove all demo data and start fresh
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAllExercises() {
        long count = exerciseRepository.count();
        exerciseRepository.deleteAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tous les exercices ont été supprimés (y compris les données de démonstration)");
        response.put("deletedCount", count);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete only demo exercises (exercises without creator)
     * Keeps real exercises created by teachers
     */
    @DeleteMapping("/demo")
    public ResponseEntity<Map<String, Object>> deleteDemoExercises() {
        List<Exercise> demoExercises = exerciseRepository.findByCreatorIsNull();
        long count = demoExercises.size();
        exerciseRepository.deleteAll(demoExercises);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Données de démonstration supprimées, données réelles conservées");
        response.put("deletedCount", count);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get only published exercises (for students)
     * Only shows real exercises (with creator), not demo data
     */
    @GetMapping("/published")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<Exercise>> getPublishedExercises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Exercise> exercises = exerciseRepository.findByIsPublishedTrueAndCreatorNotNullOrderByCreatedAtDesc(pageable);
        
        // Ensure all lazy-loaded associations are initialized
        exercises.getContent().forEach(ex -> {
            if (ex.getCreator() != null) {
                // Force initialization of Hibernate proxy
                Hibernate.initialize(ex.getCreator());
                ex.getCreator().getId();
                ex.getCreator().getUsername();
                ex.getCreator().getFullName();
            }
        });
        
        return ResponseEntity.ok(exercises);
    }
}









