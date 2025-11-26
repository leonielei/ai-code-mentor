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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
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
            logger.info("Saving exercise: {}", request.title());
            
            // Validate required fields
            if (request.title() == null || request.title().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le titre de l'exercice est requis");
            }
            if (request.description() == null || request.description().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("La description de l'exercice est requise");
            }
            if (request.difficulty() == null || request.difficulty().trim().isEmpty()) {
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
                difficultyLevel = Exercise.DifficultyLevel.valueOf(request.difficulty().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid difficulty level: {}", request.difficulty());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Niveau de difficulté invalide. Valeurs acceptées: L1, L2, L3, M1, M2");
            }
            
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
            // Use published value from request, default to true if not provided
            boolean shouldPublish = request.published();
            exercise.setPublished(shouldPublish);
            logger.info("Exercise will be published: {}", shouldPublish);
            
            // Set the creator - JPA will automatically save the relationship in the database
            exercise.setCreator(teacher);
            
            // Save the exercise - this will persist the creator_id foreign key in the database
            Exercise savedExercise = exerciseRepository.save(exercise);
            logger.info("Exercise saved successfully with ID: {}, Creator ID: {}", 
                savedExercise.getId(), 
                savedExercise.getCreator() != null ? savedExercise.getCreator().getId() : "null");
            
            // Flush to ensure the relationship is persisted immediately
            exerciseRepository.flush();
            
            // Verify the relationship was saved correctly by reloading
            Exercise verifiedExercise = exerciseRepository.findById(savedExercise.getId()).orElse(null);
            if (verifiedExercise != null && verifiedExercise.getCreator() != null) {
                logger.info("Exercise creator relationship verified in database: User ID {}", 
                    verifiedExercise.getCreator().getId());
            } else {
                logger.warn("Exercise creator relationship verification failed!");
            }
            
            return ResponseEntity.ok(savedExercise);
            
        } catch (Exception e) {
            logger.error("Error saving exercise", e);
            String errorMessage = "Erreur lors de la sauvegarde de l'exercice";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
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
     */
    @PostMapping("/get-hint")
    public ResponseEntity<?> getHint(@RequestBody HintRequest request) {
        try {
            logger.info("Generating hint for test: {}", request.testName());
            
            String hint = llmService.generateHint(
                    request.testName(),
                    request.testCode(),
                    request.studentCode(),
                    request.errorMessage()
            );
            
            return ResponseEntity.ok(new HintResponse(hint != null && !hint.isEmpty() ? hint : "Relisez attentivement l'énoncé et vérifiez votre logique."));
            
        } catch (Exception e) {
            logger.error("Error generating hint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la génération de l'indice: " + e.getMessage());
        }
    }
}

