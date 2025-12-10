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
    public ResponseEntity<?> generateExercise(@RequestBody ExerciseGenerationRequest request,
                                               @RequestHeader(value = "Authorization", required = false) String authToken) {
        try {
            logger.info("Generating exercise from description");
            
            // In a real app, validate the teacher role from authToken
            // For now, we'll skip authentication
            
            ExerciseGenerationResponse response = llmService.generateExercise(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating exercise", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la génération de l'exercice: " + e.getMessage());
        }
    }
    
    /**
     * Teacher workflow: Save generated exercise (with possible modifications)
     */
    @PostMapping("/save-exercise")
    @Transactional
    public ResponseEntity<?> saveExercise(@RequestBody ExerciseSaveRequest request,
                                          @RequestHeader(value = "Authorization", required = false) String authToken) {
        try {
            logger.info("Saving exercise - Title: {}, Description: {}, Difficulty: {}, Published: {}", 
                request.title(), 
                request.description() != null ? request.description().substring(0, Math.min(50, request.description().length())) : "null",
                request.difficulty(),
                request.published());
            
            // Validate required fields
            if (request.title() == null || request.title().trim().isEmpty()) {
                logger.warn("Save exercise failed: title is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le titre de l'exercice est requis");
            }
            if (request.description() == null || request.description().trim().isEmpty()) {
                logger.warn("Save exercise failed: description is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("La description de l'exercice est requise");
            }
            if (request.difficulty() == null || request.difficulty().trim().isEmpty()) {
                logger.warn("Save exercise failed: difficulty is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le niveau de difficulté est requis");
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Niveau de difficulté invalide: " + request.difficulty() + ". Valeurs acceptées: L1, L2, L3, M1, M2");
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
            
        } catch (Exception e) {
            logger.error("Error saving exercise: {}", e.getMessage(), e);
            e.printStackTrace();
            String errorMessage = "Erreur lors de la sauvegarde de l'exercice";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
            }
            // Include more details for debugging
            if (e.getCause() != null) {
                errorMessage += " (Cause: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMessage);
        }
    }
    
    /**
     * Student workflow: Execute code against test cases
     */
    @PostMapping("/execute-tests")
    public ResponseEntity<?> executeTests(@RequestBody TestExecutionRequest request) {
        try {
            logger.info("Executing tests for exercise: {}", request.exerciseId());
            
            Optional<Exercise> exerciseOpt = exerciseRepository.findById(request.exerciseId());
            if (exerciseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Exercice non trouvé");
            }
            
            Exercise exercise = exerciseOpt.get();
            TestExecutionResponse response = codeExecutionService.executeTests(exercise, request.code());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error executing tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'exécution des tests: " + e.getMessage());
        }
    }
    
    /**
     * Get a hint for a specific failed test
     * Supports both standard and RAG-based hint generation
     */
    @PostMapping("/get-hint")
    public ResponseEntity<?> getHint(@RequestBody HintRequest request) {
        try {
            logger.info("Generating hint for test: {}", request.testName());
            
            String hint;
            
            // 如果提供了 exerciseId 和 userQuestion，使用 RAG
            if (request.exerciseId() != null) {
                Optional<Exercise> exerciseOpt = exerciseRepository.findById(request.exerciseId());
                if (exerciseOpt.isPresent()) {
                    Exercise exercise = exerciseOpt.get();
                    hint = llmService.generateHintWithRAG(
                        request.userQuestion() != null ? request.userQuestion() : "Comment corriger cette erreur?",
                        request.testName(),
                        request.testCode(),
                        request.studentCode(),
                        request.errorMessage(),
                        exercise
                    );
                } else {
                    // Exercise not found, fall back to standard method
                    hint = llmService.generateHint(
                        request.testName(),
                        request.testCode(),
                        request.studentCode(),
                        request.errorMessage()
                    );
                }
            } else {
                // 使用标准方法
                hint = llmService.generateHint(
                    request.testName(),
                    request.testCode(),
                    request.studentCode(),
                    request.errorMessage()
                );
            }
            
            return ResponseEntity.ok(new HintResponse(hint != null && !hint.isEmpty() ? hint : "Relisez attentivement l'énoncé et vérifiez votre logique."));
            
        } catch (Exception e) {
            logger.error("Error generating hint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la génération de l'indice: " + e.getMessage());
        }
    }
}

