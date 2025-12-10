package com.aicodementor.dto;

/**
 * Request DTO for generating hints when a test fails
 * Used by students to get AI-generated hints for failed test cases
 */
public record HintRequest(
    String testName,        // Name of the failed test
    String testCode,        // The test code that failed
    String studentCode,     // Student's submitted code
    String errorMessage,    // Error message from the test failure
    String userQuestion,    // User's question (for RAG)
    Long exerciseId         // Exercise ID (for RAG context)
) {}
