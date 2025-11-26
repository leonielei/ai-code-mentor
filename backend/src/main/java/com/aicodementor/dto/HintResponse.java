package com.aicodementor.dto;

/**
 * Response DTO for AI-generated hints
 * Used to provide helpful hints to students when their code fails tests
 */
public record HintResponse(
    String hint  // AI-generated hint to help the student fix their code
) {}
