package com.aicodementor.dto;

/**
 * Response DTO for generated exercise
 */
public record ExerciseGenerationResponse(
    String title,
    String detailedDescription,
    String difficulty,
    String concepts,
    String starterCode,
    String unitTests,
    String exampleSolution,
    String examples
) {}
