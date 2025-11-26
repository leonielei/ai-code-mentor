package com.aicodementor.dto;

/**
 * Request DTO for saving an exercise
 */
public record ExerciseSaveRequest(
    String title,
    String description,
    String topic,
    String difficulty,
    String problemStatement,
    String starterCode,
    String unitTests,
    String solution,
    String concepts,
    String examples,
    boolean published
) {}
