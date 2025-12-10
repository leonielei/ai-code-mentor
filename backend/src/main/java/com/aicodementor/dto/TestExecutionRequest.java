package com.aicodementor.dto;

/**
 * Request DTO for executing student code against test cases
 */
public record TestExecutionRequest(
    Long exerciseId,
    String code,
    String language
) {}
