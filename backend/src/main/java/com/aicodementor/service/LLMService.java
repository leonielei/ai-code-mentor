package com.aicodementor.service;

import com.aicodementor.dto.ExerciseGenerationRequest;
import com.aicodementor.dto.ExerciseGenerationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.llamacpp.base-url:http://localhost:11435}")
    private String llamacppBaseUrl;

    private String callLlamaAPI(String prompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("n_predict", maxTokens);
            requestBody.put("temperature", 0.3);  // Lower temperature for more focused, deterministic output
            requestBody.put("top_p", 0.9);
            requestBody.put("top_k", 40);
            requestBody.put("repeat_penalty", 1.1);
            requestBody.put("stop", new String[]{"\n\n\n\n", "```"});  // Stop on excessive newlines or code blocks

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                llamacppBaseUrl + "/completion",
                request,
                String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode contentNode = jsonNode.get("content");
            String content = contentNode != null ? contentNode.asText() : "";
            if (content.isEmpty()) {
                logger.warn("LLM returned empty content");
                return "";
            }
            // Log full response for debugging (truncated to 500 chars)
            logger.debug("LLM Response (full, truncated): {}", 
                content.length() > 500 ? content.substring(0, 500) + "..." : content);
            return content;

        } catch (Exception e) {
            logger.error("Error calling llama.cpp API", e);
            return "";
        }
    }

    /**
     * Generate a complete exercise from a natural language description
     */
    public ExerciseGenerationResponse generateExercise(ExerciseGenerationRequest request) {
        logger.info("Generating exercise from description: {}",
                request.naturalLanguageDescription());
    
        String task = request.naturalLanguageDescription();
        String language = request.programmingLanguage();
    
        String title = "Exercice: " + task.substring(0, Math.min(50, task.length()));
    
        String starterCode;
        String unitTests;
        String solution;
        String description;
    
        if ("Java".equalsIgnoreCase(language)) {
            // Extract or generate class name from task
            String className = generateClassNameFromTask(task);
            logger.info("Generated class name: {}", className);
            
            // Step 1: Generate detailed description in English (clear explanation for students)
            logger.info("Step 1: Generating detailed description");
            String descriptionPrompt = 
                "Task: " + task + "\n" +
                "Difficulty: " + request.targetDifficulty() + "\n\n" +
                "Write a clear, complete exercise description in English for students.\n" +
                "Requirements:\n" +
                "- Write in complete, grammatically correct sentences\n" +
                "- Explain the problem clearly and concisely\n" +
                "- Include what the student needs to implement\n" +
                "- Provide one example with input and expected output\n" +
                "- Use simple, clear language suitable for students\n" +
                "- DO NOT include any code, class names, method signatures, or technical implementation details\n" +
                "- Output ONLY plain English text, no markdown, no code blocks, no special formatting\n" +
                "- Ensure every sentence is complete and makes sense";
            
            description = callLlamaAPI(descriptionPrompt, 200);
            description = removeCodeFromDescription(description.trim());
            
            // Clean up description - remove incomplete sentences and fragments
            description = cleanDescription(description);
            
            if (description.isEmpty() || description.length() < 30) {
                description = "Exercise: " + task + 
                    "\n\nYour task is to implement a solution for the problem described above. " +
                    "Make sure your implementation handles all the specified cases correctly.";
            }
            logger.info("Generated description: {} chars", description.length());
            
            // Step 2: Generate complete solution with consistent class name
            logger.info("Step 2: Generating complete solution with class name: {}", className);
            String solutionPrompt = 
                "Task: " + task + "\n" +
                "Class name MUST be: " + className + "\n\n" +
                "Write COMPLETE WORKING Java code:\n" +
                "- Class name: " + className + " (exactly this name)\n" +
                "- Public static method(s) with FULL implementation\n" +
                "- NO main method (unless task explicitly requires it)\n" +
                "- NO TODO, NO placeholders, NO comments\n" +
                "- Code must work correctly\n" +
                "- Output ONLY Java code, no markdown, no explanations";
            
            solution = callLlamaAPI(solutionPrompt, 500);
            solution = solution.trim();
            // Remove markdown code blocks if present
            solution = solution.replaceAll("(?i)```java", "").replaceAll("```", "").trim();
            
            // Remove main method if present (unless task requires it)
            if (!task.toLowerCase().contains("main") && !task.toLowerCase().contains("program")) {
                solution = removeMainMethod(solution);
            }
            
            // Ensure class name matches
            solution = ensureClassName(solution, className);
            
            // Validate and clean solution
            if (solution.isEmpty() || solution.length() < 30 || 
                solution.contains("TODO") || solution.contains("// TODO") || 
                solution.contains("non disponible") || solution.contains("Erreur")) {
                logger.warn("Solution validation failed, regenerating...");
                solutionPrompt = "Java code that solves: " + task + 
                    "\nClass name: " + className + "\n" +
                    "Complete working class with public static methods. NO main method. Output ONLY Java code.";
                solution = callLlamaAPI(solutionPrompt, 400);
                solution = solution.trim().replaceAll("(?i)```java", "").replaceAll("```", "").trim();
                solution = removeMainMethod(solution);
                solution = ensureClassName(solution, className);
            }
            
            // Final validation
            if (solution.isEmpty() || solution.length() < 30 || !solution.contains("class")) {
                logger.error("Failed to generate valid solution, using minimal fallback");
                solution = "public class " + className + " {\n" +
                    "    public static void solve() {\n" +
                    "        // TODO: Implement solution for: " + task + "\n" +
                    "    }\n" +
                    "}";
            }
            
            // Final class name check
            solution = ensureClassName(solution, className);
            
            logger.info("Generated solution: {} chars, class: {}", 
                solution.length(), extractClassName(solution));
            
            // Step 3: Generate starter code from solution (keep same class name and structure)
            logger.info("Step 3: Generating starter code from solution with class name: {}", className);
            if (!solution.isEmpty() && solution.length() > 20) {
                // Extract class structure and method signatures from solution
                starterCode = generateStarterCodeFromSolution(solution, className);
                
                // Ensure starter code uses correct class name
                starterCode = ensureClassName(starterCode, className);
                
                // Validate starter code has TODO
                if (starterCode.isEmpty() || !starterCode.contains("TODO")) {
                    logger.warn("Starter code generation failed, using fallback");
                    starterCode = createStarterCodeFallback(solution, className);
                }
            } else {
                starterCode = "public class " + className + " {\n    public static void solve() {\n        // TODO: Implement your solution here\n    }\n}";
            }
            
            // Final class name check for starter code
            starterCode = ensureClassName(starterCode, className);
            
            logger.info("Generated starter code: {} chars, class: {}", 
                starterCode.length(), extractClassName(starterCode));
            
            // Step 4: Generate JUnit tests
            logger.info("Step 4: Generating JUnit tests for class: {}", className);
            
            String solutionForPrompt = solution.length() > 600 
                ? solution.substring(0, 600) + "..." 
                : solution;
            
            String unitTestsPrompt = 
                "Solution class: " + className + "\n" +
                "Solution code:\n" + solutionForPrompt + "\n\n" +
                "Generate JUnit 5 tests:\n" +
                "- Test class name: " + className + "Test\n" +
                "- Import: org.junit.jupiter.api.Test, static org.junit.jupiter.api.Assertions.*\n" +
                "- 3 @Test methods: testBasicCase, testEdgeCase, testComplexCase\n" +
                "- Call " + className + " static methods and assert results\n" +
                "- Output ONLY Java test code, no markdown, no explanations";
            
            unitTests = callLlamaAPI(unitTestsPrompt, 350);
            unitTests = unitTests.trim();
            unitTests = unitTests.replaceAll("(?i)```java", "").replaceAll("```", "").trim();
            
            // Ensure test class name matches
            String expectedTestClassName = className + "Test";
            if (!unitTests.contains(expectedTestClassName)) {
                unitTests = unitTests.replaceAll("class \\w+Test", "class " + expectedTestClassName);
            }
            
            if (unitTests.isEmpty() || !unitTests.contains("@Test")) {
                unitTests = createDefaultTests(className);
            }
            logger.info("Generated unit tests: {} chars", unitTests.length());
    
        } else {
            // Only Java is supported
            logger.error("Unsupported programming language: {}. Only Java is supported.", language);
            description = "Unsupported programming language. Only Java is supported.";
            starterCode = "// Unsupported language";
            unitTests = "// Unsupported language";
            solution = "// Unsupported language";
        }
    
        return new ExerciseGenerationResponse(
            title,
            description,
            request.targetDifficulty(),
            language + ", " + request.targetDifficulty(),
            starterCode,
            unitTests,
            solution,
            "Input: 'test' -> Output: see problem statement"
        );
    }
    
    /**
     * Generate a meaningful class name from task description
     */
    private String generateClassNameFromTask(String task) {
        if (task == null || task.trim().isEmpty()) {
            return "Solution";
        }
        
        // Extract key words from task
        String cleanTask = task.trim();
        
        // Try to find a meaningful noun or verb
        String[] words = cleanTask.split("\\s+");
        StringBuilder className = new StringBuilder();
        
        // Look for action words or key nouns
        for (int i = 0; i < Math.min(words.length, 3); i++) {
            String word = words[i].replaceAll("[^a-zA-Z0-9]", "");
            if (word.length() > 2 && !word.equalsIgnoreCase("the") && 
                !word.equalsIgnoreCase("a") && !word.equalsIgnoreCase("an") &&
                !word.equalsIgnoreCase("and") && !word.equalsIgnoreCase("or")) {
                if (className.length() > 0) {
                    className.append(capitalize(word));
                } else {
                    className.append(capitalize(word));
                }
            }
        }
        
        if (className.length() == 0) {
            // Fallback: use first meaningful word
            for (String word : words) {
                String clean = word.replaceAll("[^a-zA-Z0-9]", "");
                if (clean.length() > 2) {
                    className.append(capitalize(clean));
                    break;
                }
            }
        }
        
        // Ensure it's a valid Java class name
        String result = className.toString();
        if (result.isEmpty() || !Character.isLetter(result.charAt(0))) {
            result = "Solution";
        }
        
        // Ensure it starts with uppercase
        if (result.length() > 0 && Character.isLowerCase(result.charAt(0))) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }
        
        return result.length() > 0 ? result : "Solution";
    }
    
    /**
     * Capitalize first letter of a word
     */
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
    
    /**
     * Extract class name from Java code
     */
    private String extractClassName(String code) {
        if (code == null || code.isEmpty()) {
            return "Solution";
        }
        
        // Try to find "public class ClassName"
        int classIdx = code.indexOf("public class ");
        if (classIdx >= 0) {
            int nameStart = classIdx + "public class ".length();
            int nameEnd = nameStart;
            while (nameEnd < code.length() && 
                   (Character.isLetterOrDigit(code.charAt(nameEnd)) || code.charAt(nameEnd) == '_')) {
                nameEnd++;
            }
            if (nameEnd > nameStart) {
                return code.substring(nameStart, nameEnd);
            }
        }
        
        // Try "class ClassName"
        classIdx = code.indexOf("class ");
        if (classIdx >= 0) {
            int nameStart = classIdx + "class ".length();
            int nameEnd = nameStart;
            while (nameEnd < code.length() && 
                   (Character.isLetterOrDigit(code.charAt(nameEnd)) || code.charAt(nameEnd) == '_')) {
                nameEnd++;
            }
            if (nameEnd > nameStart) {
                return code.substring(nameStart, nameEnd);
            }
        }
        
        return "Solution";
    }
    
    /**
     * Ensure code uses the specified class name
     */
    private String ensureClassName(String code, String expectedClassName) {
        if (code == null || code.isEmpty() || expectedClassName == null) {
            return code;
        }
        
        String currentClassName = extractClassName(code);
        if (currentClassName.equals(expectedClassName)) {
            return code;
        }
        
        // Replace class name
        code = code.replaceAll("public class " + currentClassName, "public class " + expectedClassName);
        code = code.replaceAll("class " + currentClassName + " ", "class " + expectedClassName + " ");
        code = code.replaceAll("class " + currentClassName + "\\{", "class " + expectedClassName + "{");
        code = code.replaceAll("class " + currentClassName + "\n", "class " + expectedClassName + "\n");
        
        return code;
    }
    
    /**
     * Remove main method from code (unless it's required by the task)
     */
    private String removeMainMethod(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        
        // Pattern to match main method: public static void main(String[] args) { ... }
        // This is a simplified removal - we'll remove the entire main method
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inMainMethod = false;
        int braceDepth = 0;
        boolean foundMainSignature = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Detect main method signature
            if (trimmed.contains("public static void main") && trimmed.contains("String[]")) {
                inMainMethod = true;
                foundMainSignature = true;
                // Count opening brace if on same line
                if (trimmed.contains("{")) {
                    braceDepth = 1;
                } else {
                    braceDepth = 0;
                }
                continue; // Skip the main method signature line
            }
            
            // If we're in main method, track braces
            if (inMainMethod) {
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                
                // If we've closed all braces, we're done with main method
                if (braceDepth <= 0 && foundMainSignature) {
                    inMainMethod = false;
                    foundMainSignature = false;
                    braceDepth = 0;
                }
                continue; // Skip main method body
            }
            
            // Keep all other lines
            result.append(line).append("\n");
        }
        
        return result.toString().trim();
    }
    
    /**
     * Generate starter code from solution, keeping the same class structure
     * This method extracts the class structure and method signatures, then replaces method bodies with TODO
     */
    private String generateStarterCodeFromSolution(String solution, String expectedClassName) {
        if (solution == null || solution.isEmpty()) {
            String className = expectedClassName != null ? expectedClassName : "Solution";
            return "public class " + className + " {\n    // TODO: Implement your solution here\n}";
        }
        
        StringBuilder starter = new StringBuilder();
        String[] lines = solution.split("\n");
        boolean inMethodBody = false;
        int braceDepth = 0;
        boolean foundClass = false;
        String currentMethodSignature = null;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Keep imports and package
            if (trimmed.startsWith("import ") || trimmed.startsWith("package ")) {
                starter.append(line).append("\n");
                continue;
            }
            
            // Keep class declaration
            if (trimmed.contains("class ") && !foundClass) {
                starter.append(line).append("\n");
                foundClass = true;
                continue;
            }
            
            // Detect method signature (public/private static method with parameters)
            if (!inMethodBody && (trimmed.contains("public static") || trimmed.contains("private static")) && 
                trimmed.contains("(") && trimmed.contains(")")) {
                // This is a method signature
                currentMethodSignature = trimmed;
                starter.append("    ").append(trimmed);
                
                // Check if opening brace is on same line
                if (trimmed.contains("{")) {
                    starter.append("\n");
                    starter.append("        // TODO: Implement your solution here\n");
                    // Determine return type
                    if (trimmed.contains("void")) {
                        starter.append("    }\n");
                    } else if (trimmed.contains("int") || trimmed.contains("long") || trimmed.contains("double") || trimmed.contains("float")) {
                        starter.append("        return 0;\n    }\n");
                    } else if (trimmed.contains("boolean")) {
                        starter.append("        return false;\n    }\n");
                    } else if (trimmed.contains("String")) {
                        starter.append("        return \"\";\n    }\n");
                    } else {
                        starter.append("        return null;\n    }\n");
                    }
                    currentMethodSignature = null;
                } else {
                    // Opening brace on next line
                    inMethodBody = true;
                    braceDepth = 0;
                    starter.append(" {\n");
                    starter.append("        // TODO: Implement your solution here\n");
                }
                continue;
            }
            
            // Handle method body
            if (inMethodBody && currentMethodSignature != null) {
                // Count braces
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                
                // If we've closed all braces, end the method
                if (braceDepth <= 0 && line.contains("}")) {
                    // Add return statement based on method signature
                    if (!currentMethodSignature.contains("void")) {
                        if (currentMethodSignature.contains("int") || currentMethodSignature.contains("long") || 
                            currentMethodSignature.contains("double") || currentMethodSignature.contains("float")) {
                            starter.append("        return 0;\n");
                        } else if (currentMethodSignature.contains("boolean")) {
                            starter.append("        return false;\n");
                        } else if (currentMethodSignature.contains("String")) {
                            starter.append("        return \"\";\n");
                        } else {
                            starter.append("        return null;\n");
                        }
                    }
                    starter.append("    }\n");
                    inMethodBody = false;
                    currentMethodSignature = null;
                    braceDepth = 0;
                }
                // Skip the original method body lines
                continue;
            }
            
            // Keep class closing brace
            if (foundClass && trimmed.equals("}") && !inMethodBody) {
                starter.append(line).append("\n");
            }
        }
        
        String result = starter.toString().trim();
        
        // Ensure correct class name
        result = ensureClassName(result, expectedClassName);
        
        // Validate result
        if (result.isEmpty() || !result.contains("TODO")) {
            return createStarterCodeFallback(solution, expectedClassName);
        }
        
        return result;
    }
    
    /**
     * Fallback method to create starter code from solution
     */
    private String createStarterCodeFallback(String solution, String expectedClassName) {
        if (solution == null || solution.isEmpty()) {
            return "public class " + expectedClassName + " {\n    public static void solve() {\n        // TODO: Implement your solution here\n    }\n}";
        }
        
        // Extract imports
        StringBuilder starter = new StringBuilder();
        String[] lines = solution.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("import ") || line.trim().startsWith("package ")) {
                starter.append(line).append("\n");
            }
        }
        
        // Use expected class name
        String className = expectedClassName != null ? expectedClassName : extractClassName(solution);
        
        // Extract method signatures
        starter.append("public class ").append(className).append(" {\n");
        
        // Find method signatures - look for complete method signatures
        for (String line : lines) {
            String trimmed = line.trim();
            // Match method signatures: public/private static ReturnType methodName(...)
            if ((trimmed.contains("public static") || trimmed.contains("private static")) && 
                trimmed.contains("(") && trimmed.contains(")") && 
                !trimmed.contains("{") && !trimmed.contains("}")) {
                // Method signature line - keep it and add TODO body
                starter.append("    ").append(trimmed).append(" {\n");
                starter.append("        // TODO: Implement your solution here\n");
                
                // Add placeholder return based on return type
                if (trimmed.contains("void")) {
                    starter.append("    }\n");
                } else if (trimmed.contains("int") || trimmed.contains("long") || trimmed.contains("double") || trimmed.contains("float")) {
                    starter.append("        return 0;\n    }\n");
                } else if (trimmed.contains("boolean")) {
                    starter.append("        return false;\n    }\n");
                } else if (trimmed.contains("String")) {
                    starter.append("        return \"\";\n    }\n");
                } else {
                    starter.append("        return null;\n    }\n");
                }
            }
        }
        
        starter.append("}\n");
        return starter.toString();
    }
    
    /**
     * Create default JUnit tests
     */
    private String createDefaultTests(String className) {
        return 
            "import org.junit.jupiter.api.Test;\n" +
            "import static org.junit.jupiter.api.Assertions.*;\n\n" +
            "public class " + className + "Test {\n" +
            "    @Test\n" +
            "    void testCasBasique() {\n" +
            "        // TODO: appeler " + className + ".nomMethode(...) et vérifier le résultat\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testCasLimite() {\n" +
            "        // TODO: cas limites (vide, null, bornes...)\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testCasComplexe() {\n" +
            "        // TODO: scénario plus complexe\n" +
            "    }\n" +
            "}";
    }
    
    /**
     * Clean description text - remove incomplete sentences and fragments
     */
    private String cleanDescription(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // Split into sentences
        String[] sentences = text.split("[.!?]+");
        StringBuilder cleaned = new StringBuilder();
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Skip very short fragments (likely incomplete)
            if (trimmed.length() < 10) {
                continue;
            }
            
            // Skip lines that are just single words or fragments
            String[] words = trimmed.split("\\s+");
            if (words.length < 3) {
                continue;
            }
            
            // Skip lines that look like code fragments
            if (trimmed.matches(".*[a-zA-Z]+\\s*\\(.*\\)\\s*.*") && trimmed.length() < 50) {
                continue;
            }
            
            // Capitalize first letter if needed
            if (trimmed.length() > 0 && Character.isLowerCase(trimmed.charAt(0))) {
                trimmed = Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
            }
            
            cleaned.append(trimmed);
            
            // Add period if sentence doesn't end with punctuation
            if (!trimmed.endsWith(".") && !trimmed.endsWith("!") && !trimmed.endsWith("?")) {
                cleaned.append(".");
            }
            
            cleaned.append(" ");
        }
        
        return cleaned.toString().trim();
    }
    
    /**
     * Remove code blocks from description text
     */
    private String removeCodeFromDescription(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // Remove code blocks (```java ... ```, ``` ... ```)
        text = text.replaceAll("```[\\s\\S]*?```", "");
        
        // Remove lines that look like code (contain class, public static, etc.)
        String[] lines = text.split("\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip lines that look like code
            if (trimmed.startsWith("public class") ||
                trimmed.startsWith("class ") ||
                trimmed.startsWith("public static") ||
                trimmed.startsWith("private static") ||
                trimmed.startsWith("import ") ||
                trimmed.startsWith("package ") ||
                trimmed.contains("public void main") ||
                trimmed.contains("static void main") ||
                (trimmed.contains("{") && trimmed.contains("}") && trimmed.length() < 100) ||
                (trimmed.matches(".*[a-zA-Z]+\\s*\\(.*\\)\\s*\\{.*") && trimmed.length() < 150)) {
                continue;
            }
            // Skip lines that are just braces
            if (trimmed.equals("{") || trimmed.equals("}") || trimmed.equals("{}")) {
                continue;
            }
            cleaned.append(line).append("\n");
        }
        
        return cleaned.toString().trim();
    }

    /**
     * Generate a hint for a failed test case
     */
    public String generateHint(String testName, String testCode, String studentCode, String errorMessage) {
        logger.info("Generating hint for failed test: {}", testName);

        String prompt = "The test '" + testName + "' failed with error: " + errorMessage + "\n\n" +
            "Student code:\n" + studentCode + "\n\n" +
            "Test code:\n" + testCode + "\n\n" +
            "Give a helpful, short hint (2-3 sentences) to guide the student:";

        String hint = callLlamaAPI(prompt, 150);
        if (hint == null || hint.trim().isEmpty() || hint.trim().length() < 10) {
            return "Vérifiez votre logique et assurez-vous que votre code gère tous les cas de test.";
        }
        return hint.trim();
    }
}
