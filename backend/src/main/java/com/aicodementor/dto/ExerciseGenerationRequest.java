package com.aicodementor.dto;

/**
 * Request DTO for generating exercises from natural language description
 */
public record ExerciseGenerationRequest(
    String naturalLanguageDescription,
    String targetDifficulty, // L1, L2, L3, M1, M2
    String programmingLanguage // Java, Python, etc.
) {}
