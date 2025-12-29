package com.aicodementor.controller;

import com.aicodementor.dto.*;
import com.aicodementor.entity.Exercise;
import com.aicodementor.entity.User;
import com.aicodementor.repository.ExerciseRepository;
import com.aicodementor.repository.UserRepository;
import com.aicodementor.service.CodeExecutionService;
import com.aicodementor.service.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/llm")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class LLMController {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMController.class);
    
    @Autowired
    private LLMService llmService;
    
    @Autowired
    private CodeExecutionService codeExecutionService;
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Teacher workflow: Generate exercise from natural language description
     */
    @PostMapping("/generate-exercise")
    public ResponseEntity<ExerciseGenerationResponse> generateExercise(@RequestBody ExerciseGenerationRequest request,
                                               @RequestHeader(value = "Authorization", required = false) String authToken) {
        logger.info("Generating exercise from description");
        
        // In a real app, validate the teacher role from authToken
        // For now, we'll skip authentication
        
        ExerciseGenerationResponse response = llmService.generateExercise(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Teacher workflow: Save generated exercise (with possible modifications)
     */
    @PostMapping("/save-exercise")
    @Transactional
    public ResponseEntity<Exercise> saveExercise(@RequestBody ExerciseSaveRequest request,
                                          @RequestHeader(value = "Authorization", required = false) String authToken) {
        try {
            logger.info("Saving exercise - Title: {}, Description: {}, Difficulty: {}, Published: {}", 
                request.title(), 
                request.description() != null ? request.description().substring(0, Math.min(50, request.description().length())) : "null",
                request.difficulty(),
                request.published());
            
            // Validate required fields
            if (request.title() == null || request.title().trim().isEmpty()) {
                throw new IllegalArgumentException("Le titre de l'exercice est requis");
            }
            if (request.description() == null || request.description().trim().isEmpty()) {
                throw new IllegalArgumentException("La description de l'exercice est requise");
            }
            if (request.difficulty() == null || request.difficulty().trim().isEmpty()) {
                throw new IllegalArgumentException("Le niveau de difficulté est requis");
            }
            
            // Find teacher (for demo, use first user with TEACHER role, or create one if none exists)
            Optional<User> teacherOpt = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.TEACHER)
                    .findFirst();
            
            User teacher;
            if (teacherOpt.isEmpty()) {
                // Create a default teacher if none exists
                logger.warn("No teacher found, creating default teacher user");
                // Check if teacher user already exists by username or email
                Optional<User> existingTeacher = userRepository.findByUsername("teacher");
                if (existingTeacher.isEmpty()) {
                    existingTeacher = userRepository.findByEmail("teacher@demo.com");
                }
                
                if (existingTeacher.isPresent()) {
                    teacher = existingTeacher.get();
                    // Update role if needed
                    if (teacher.getRole() != User.UserRole.TEACHER) {
                        teacher.setRole(User.UserRole.TEACHER);
                        teacher = userRepository.save(teacher);
                    }
                    logger.info("Using existing teacher user: {}", teacher.getId());
                } else {
                    teacher = new User();
                    teacher.setUsername("teacher");
                    teacher.setEmail("teacher@demo.com");
                    teacher.setPassword("demo123");
                    teacher.setFullName("Prof. Demo");
                    teacher.setRole(User.UserRole.TEACHER);
                    teacher = userRepository.save(teacher);
                    logger.info("Created default teacher user: {}", teacher.getId());
                }
            } else {
                teacher = teacherOpt.get();
            }
            
            // Parse difficulty level with error handling
            Exercise.DifficultyLevel difficultyLevel;
            try {
                String difficultyUpper = request.difficulty().toUpperCase().trim();
                difficultyLevel = Exercise.DifficultyLevel.valueOf(difficultyUpper);
                logger.debug("Parsed difficulty level: {}", difficultyLevel);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid difficulty level: {} - Error: {}", request.difficulty(), e.getMessage());
                throw new IllegalArgumentException("Niveau de difficulté invalide: " + request.difficulty() + ". Valeurs acceptées: L1, L2, L3, M1, M2");
            }
            
            // Create and populate exercise entity
            Exercise exercise = new Exercise();
            exercise.setTitle(request.title().trim());
            exercise.setDescription(request.description() != null ? request.description().trim() : "");
            exercise.setTopic(request.topic() != null && !request.topic().trim().isEmpty() 
                    ? request.topic().trim() : "Général");
            exercise.setDifficulty(difficultyLevel);
            exercise.setProblemStatement(request.problemStatement() != null ? request.problemStatement().trim() : "");
            exercise.setStarterCode(request.starterCode() != null ? request.starterCode().trim() : "");
            exercise.setUnitTests(request.unitTests() != null ? request.unitTests().trim() : "");
            exercise.setSolution(request.solution() != null ? request.solution().trim() : "");
            exercise.setConcepts(request.concepts() != null ? request.concepts().trim() : "");
            exercise.setExamples(request.examples() != null ? request.examples().trim() : "");
            
            // Handle published field - ensure it's properly set
            boolean shouldPublish = request.published();
            exercise.setPublished(shouldPublish);
            logger.info("Exercise will be published: {} (from request.published())", shouldPublish);
            
            // Set the creator - JPA will automatically save the relationship in the database
            exercise.setCreator(teacher);
            
            // Save the exercise - this will persist the creator_id foreign key in the database
            Exercise savedExercise = exerciseRepository.save(exercise);
            logger.info("Exercise saved successfully with ID: {}, Title: {}, Published: {}, Creator ID: {}", 
                savedExercise.getId(),
                savedExercise.getTitle(),
                savedExercise.isPublished(),
                savedExercise.getCreator() != null ? savedExercise.getCreator().getId() : "null");
            
            // Flush to ensure the relationship is persisted immediately
            exerciseRepository.flush();
            
            // Verify the relationship was saved correctly by reloading
            Exercise verifiedExercise = exerciseRepository.findById(savedExercise.getId()).orElse(null);
            if (verifiedExercise != null) {
                logger.info("Exercise verification - ID: {}, Published: {}, Creator ID: {}", 
                    verifiedExercise.getId(),
                    verifiedExercise.isPublished(),
                    verifiedExercise.getCreator() != null ? verifiedExercise.getCreator().getId() : "null");
            } else {
                logger.error("Exercise verification failed - exercise not found after save!");
            }
            
            // Return the saved exercise with all fields
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExercise);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation when saving exercise: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument when saving exercise: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
    
    /**
     * Student workflow: Execute code against test cases
     */
    @PostMapping("/execute-tests")
    public ResponseEntity<TestExecutionResponse> executeTests(@RequestBody TestExecutionRequest request) {
        logger.info("Executing tests for exercise: {}", request.exerciseId());
        
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(request.exerciseId());
        if (exerciseOpt.isEmpty()) {
            throw new IllegalArgumentException("Exercice non trouvé");
        }
        
        Exercise exercise = exerciseOpt.get();
        TestExecutionResponse response = codeExecutionService.executeTests(exercise, request.code());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get a hint for a specific failed test
     */
    @PostMapping("/get-hint")
    public ResponseEntity<HintResponse> getHint(@RequestBody HintRequest request) {
        logger.info("Generating hint for test: {}", request.testName());
        
        String hint = llmService.generateHint(
            request.testName(),
            request.testCode(),
            request.studentCode(),
            request.errorMessage()
        );
        
        return ResponseEntity.ok(new HintResponse(hint != null && !hint.isEmpty() ? hint : "Relisez attentivement l'énoncé et vérifiez votre logique."));
    }
}

