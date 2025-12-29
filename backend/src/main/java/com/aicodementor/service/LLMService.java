package com.aicodementor.service;

import com.aicodementor.dto.ExerciseGenerationRequest;
import com.aicodementor.dto.ExerciseGenerationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String llamacppBaseUrl;
    private final ExecutorService executorService;

    public LLMService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                     String llamacppBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.llamacppBaseUrl = llamacppBaseUrl != null 
            ? llamacppBaseUrl 
            : "http://localhost:11435";
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
    }

    // ============================================================
    // 1) Generic: Call llama.cpp /completion endpoint (ASYNC)
    // ============================================================
    private CompletableFuture<String> callLlamaAPIAsync(String prompt, int maxTokens) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpHeaders headers = createHeaders();
                Map<String, Object> requestBody = createRequestBody(prompt, maxTokens);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                
                String response = restTemplate.postForObject(
                    llamacppBaseUrl + "/completion",
                    request,
                    String.class
                );
                
                return processResponse(response);
            } catch (Exception e) {
                logger.error("Error calling llama.cpp API", e);
                return "";
            }
        }, executorService);
    }
    
    private String callLlamaAPI(String prompt, int maxTokens) {
        try {
            return callLlamaAPIAsync(prompt, maxTokens).get();
        } catch (Exception e) {
            logger.error("Error in synchronous LLM call", e);
            return "";
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    private Map<String, Object> createRequestBody(String prompt, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("n_predict", maxTokens);
        requestBody.put("temperature", 0.2);
        requestBody.put("top_p", 0.95);
        requestBody.put("top_k", 40);
        requestBody.put("repeat_penalty", 1.15);
        // Only stop on code block markers or EOS token, not on comments or newlines
        // This prevents premature truncation that breaks code generation
        requestBody.put("stop", new String[]{"```", "</s>"});
        return requestBody;
    }
    
    private String processResponse(String response) {
        if (response == null) {
            logger.warn("LLM returned null response");
            return "";
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String content = "";
            
            // Multi-field compatibility: try content, completion, or choices[0].text
            if (jsonNode.hasNonNull("content")) {
                content = jsonNode.get("content").asText();
            } else if (jsonNode.hasNonNull("completion")) {
                content = jsonNode.get("completion").asText();
            } else if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && jsonNode.get("choices").size() > 0) {
                JsonNode c0 = jsonNode.get("choices").get(0);
                if (c0.hasNonNull("text")) {
                    content = c0.get("text").asText();
                } else if (c0.hasNonNull("message") && c0.get("message").hasNonNull("content")) {
                    content = c0.get("message").get("content").asText();
                }
            }
            
            if (content.isEmpty()) {
                logger.warn("LLM returned empty content. Response structure: {}", jsonNode.toPrettyString().substring(0, Math.min(500, jsonNode.toPrettyString().length())));
                return "";
            }
            
            content = sanitize(content);
            logger.debug("LLM response (truncated): {}",
                content.length() > 500 ? content.substring(0, 500) + "..." : content);
            return content;
        } catch (Exception e) {
            logger.error("Error processing LLM response", e);
            return "";
        }
    }

    // ============================================================
    // 2) Main entry: Generate complete exercise from natural language
    // ============================================================
    public ExerciseGenerationResponse generateExercise(ExerciseGenerationRequest request) {
        logger.info("Generating exercise from description: {}",
            request.naturalLanguageDescription());

        String task = request.naturalLanguageDescription();
        String language = extractLanguage(request);
        String difficulty = extractDifficulty(request);
        String coreTask = extractCoreTask(task);
        String title = buildTitleFromTask(coreTask, language);

        if (!"java".equalsIgnoreCase(language)) {
            return createUnsupportedLanguageResponse(language);
        }

        return generateJavaExercise(coreTask, title, difficulty);
    }
    
    private String extractLanguage(ExerciseGenerationRequest request) {
        return request.programmingLanguage() == null 
            ? "Java" 
            : request.programmingLanguage();
    }
    
    private String extractDifficulty(ExerciseGenerationRequest request) {
        return request.targetDifficulty() == null 
            ? "L1" 
            : request.targetDifficulty();
    }
    
    private ExerciseGenerationResponse createUnsupportedLanguageResponse(String language) {
        logger.error("Unsupported language: {}", language);
        return new ExerciseGenerationResponse(
            "Exercice (" + language + ")",
            "Langage non supporté. Pour l'instant, seul Java est pris en charge.",
            "L1",
            "Langage non supporté",
            "// Unsupported language",
            "// Unsupported language",
            "// Unsupported language",
            "N/A"
        );
    }
    
    private ExerciseGenerationResponse generateJavaExercise(String coreTask, 
                                                           String title, 
                                                           String difficulty) {
        String className = generateClassNameFromTask(coreTask);
        logger.info("Generated class name: {}", className);

        String description = buildDescriptionFromTask(coreTask);
        String solution = generateSolutionCode(coreTask, className);
        logger.info("Generated solution length = {}", solution.length());

        String starterCode = generateStarterCodeFromSolution(solution, className);
        logger.info("Generated starter length = {}", starterCode.length());

        String unitTests = generateJUnitTests(coreTask, solution, className);
        logger.info("Generated tests length = {}", unitTests.length());

        String concepts = detectConceptsFromTask(coreTask, difficulty);
        String examples = generateExamplesFromTask(coreTask, className, solution);

        return new ExerciseGenerationResponse(
            title, description, difficulty, concepts,
            starterCode, unitTests, solution, examples
        );
    }

    // ============================================================
    // 3) Extract "core task" from description + build title + description
    // ============================================================
    private String extractCoreTask(String task) {
        if (task == null) {
            return "";
        }
        String core = task.trim();
        core = removeExercisePrefixes(core);
        return core.isEmpty() ? task.trim() : core;
    }
    
    private String removeExercisePrefixes(String core) {
        core = core.replaceFirst("(?i)^exercice\\s*:?\\s*", "");
        core = core.replaceFirst("(?i)^cr[ée]er? un exercice o[uù] les [ée]tudiants doivent\\s*", "");
        core = core.replaceFirst("(?i)^cr[ée]er? un exercice o[uù] les [ée]tudiants\\s*", "");
        core = core.replaceFirst("(?i)^cr[ée]er? un exercice\\s*", "");
        core = core.replaceFirst("(?i)^write an exercise where students (must|have to|need to)\\s*", "");
        return core.trim();
    }

    private String buildTitleFromTask(String coreTask, String language) {
        if (coreTask == null || coreTask.isBlank()) {
            return "Exercice de programmation (" + language + ")";
        }

        String firstSentence = extractFirstSentence(coreTask);
        firstSentence = cleanTitleSentence(firstSentence);
        firstSentence = capitalizeFirstLetter(firstSentence);

        return "Exercice : " + firstSentence + " (" + language + ")";
    }
    
    private String extractFirstSentence(String coreTask) {
        String firstSentence = coreTask.split("[.?!]")[0].trim();
        return firstSentence.length() > 70 
            ? firstSentence.substring(0, 70).trim() 
            : firstSentence;
    }
    
    private String cleanTitleSentence(String sentence) {
        sentence = sentence.replaceFirst("(?i)^impl[ée]menter\\s+une?\\s+fonction\\s+qui\\s*", "");
        sentence = sentence.replaceFirst("(?i)^[ée]crire\\s+une?\\s+fonction\\s+qui\\s*", "");
        sentence = sentence.replaceFirst("(?i)^[ée]crire\\s+un\\s+programme\\s+qui\\s*", "");
        return sentence.trim();
    }
    
    private String capitalizeFirstLetter(String text) {
        if (!text.isEmpty() && Character.isLowerCase(text.charAt(0))) {
            return Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }
        return text;
    }

    private String buildDescriptionFromTask(String coreTask) {
        if (coreTask == null || coreTask.isBlank()) {
            return createDefaultDescription();
        }

        String sentence = ensureEndsWithPeriod(coreTask.trim());
        return buildDescriptionText(sentence);
    }
    
    private String createDefaultDescription() {
        return "Dans cet exercice, vous devez écrire une fonction en Java qui respecte la consigne donnée. "
            + "Votre code doit être clair, correct et gérer les cas simples ainsi que quelques cas limites.";
    }
    
    private String ensureEndsWithPeriod(String sentence) {
        return sentence.endsWith(".") ? sentence : sentence + ".";
    }
    
    private String buildDescriptionText(String sentence) {
        return "Dans cet exercice, vous devez " + sentence + "\n"
            + "Vous écrirez une ou plusieurs méthodes en Java respectant cette consigne.\n"
            + "Votre code doit être clair, correctement indenté et gérer les cas simples ainsi que quelques cas limites.";
    }

    // ============================================================
    // 4) Java Solution: Generate via llama.cpp + fix braces
    // ============================================================
    private String generateSolutionCode(String task, String className) {
        if (task == null || task.isBlank()) {
            task = "implémenter une fonction utilitaire en Java.";
        }

        String prompt = buildSolutionPrompt(task, className);
        // Increase n_predict to 1400 to prevent truncation of long solutions
        String code = callLlamaAPI(prompt, 1400);
        code = cleanJavaSnippet(code);
        return validateAndFixSolution(code, className, task);
    }
    
    private String buildSolutionPrompt(String task, String className) {
        return "Tu es un expert en programmation Java. Génère une solution COMPLÈTE et FONCTIONNELLE pour cet exercice.\n\n"
            + "=== EXERCICE ===\n" + task + "\n\n"
            + "=== EXIGENCES ABSOLUES ===\n"
            + "1. Nom de classe EXACT : " + className + "\n"
            + "2. Méthodes : public static avec implémentation COMPLÈTE (pas de TODO, pas de code vide)\n"
            + "3. Le code DOIT être compilable et fonctionnel immédiatement\n"
            + "4. Gestion OBLIGATOIRE des cas limites : null, tableaux vides, chaînes vides, valeurs négatives\n"
            + "5. Structure correcte : toutes les accolades fermées, indentation 4 espaces\n"
            + "6. PAS de méthode main (sauf si l'exercice le demande explicitement)\n"
            + "7. PAS de commentaires dans le code\n"
            + "8. PAS de markdown, PAS d'explications, UNIQUEMENT du code Java brut\n\n"
            + "=== EXEMPLE DE BONNE SOLUTION ===\n"
            + "Si l'exercice demande de calculer la somme d'un tableau :\n"
            + "public class ArraySum {\n"
            + "    public static int sum(int[] array) {\n"
            + "        if (array == null || array.length == 0) return 0;\n"
            + "        int total = 0;\n"
            + "        for (int num : array) {\n"
            + "            total += num;\n"
            + "        }\n"
            + "        return total;\n"
            + "    }\n"
            + "}\n\n"
            + "=== FORMAT DE RÉPONSE ===\n"
            + "Commence DIRECTEMENT par 'public class " + className + " {'\n"
            + "Réponds UNIQUEMENT avec le code Java complet et fonctionnel, rien d'autre.\n\n"
            + "Code Java :";
    }
    
    private String validateAndFixSolution(String code, String className, String task) {
        if (code == null || code.isBlank()) {
            logger.warn("Solution is null or empty, retrying...");
            return retrySolutionGeneration(task, className);
        }
        
        if (!code.contains("class ")) {
            logger.warn("Solution has no 'class', retrying...");
            return retrySolutionGeneration(task, className);
        }

        // Check for structural completeness - ensure class has closing brace
        if (!code.contains("}")) {
            logger.warn("Solution seems truncated (missing closing brace), retrying...");
            return retrySolutionGeneration(task, className);
        }
        
        // Check if method body is complete (has opening and closing braces)
        if (code.contains("public static") && !code.contains("return ")) {
            // If there's a method signature but no return statement, might be truncated
            int methodCount = countOccurrences(code, "public static");
            int returnCount = countOccurrences(code, "return ");
            if (methodCount > returnCount && methodCount > 0) {
                logger.warn("Solution seems incomplete (methods without return statements), retrying...");
                return retrySolutionGeneration(task, className);
            }
        }

        if (code.contains("TODO") || code.contains("todo") || code.contains("// TODO")) {
            logger.warn("Solution contains TODO, retrying...");
            return retrySolutionGeneration(task, className);
        }

        if (code.length() < 100 || !hasRealImplementation(code)) {
            logger.warn("Solution too short or no real implementation, retrying...");
            return retrySolutionGeneration(task, className);
        }

        if (!needsMain(task)) {
            code = removeMainMethod(code);
        }

        code = ensureClassName(code, className);
        code = fixBraces(code);

        if (code.contains("TODO") || !hasRealImplementation(code)) {
            logger.error("Solution still invalid after fixes, using intelligent fallback.");
            return createIntelligentFallback(className, task);
        }

        return code.trim();
    }
    
    private int countOccurrences(String text, String pattern) {
        if (text == null || pattern == null) return 0;
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    private String retrySolutionGeneration(String task, String className) {
        logger.info("Retrying solution generation with enhanced prompt...");
        String enhancedPrompt = buildEnhancedSolutionPrompt(task, className);
        // Use same high n_predict as initial generation to prevent truncation
        String code = callLlamaAPI(enhancedPrompt, 1400);
        code = cleanJavaSnippet(code);
        
        if (code != null && code.contains("class ") && !code.contains("TODO") 
            && code.length() > 100 && hasRealImplementation(code)) {
            return code.trim();
        }
        
        return createIntelligentFallback(className, task);
    }
    
    private String buildEnhancedSolutionPrompt(String task, String className) {
        return "URGENT: Génère du code Java COMPLET et FONCTIONNEL. PAS de TODO, PAS de code vide.\n\n"
            + "=== EXERCICE ===\n" + task + "\n\n"
            + "=== CODE REQUIS ===\n"
            + "1. Classe : " + className + "\n"
            + "2. Méthode public static avec CODE COMPLET (pas de TODO)\n"
            + "3. Le code DOIT fonctionner immédiatement\n"
            + "4. Gère null, tableaux vides, chaînes vides\n\n"
            + "=== EXEMPLE CONCRET ===\n"
            + "Si exercice = 'calculer somme tableau' :\n"
            + "public class ArraySum {\n"
            + "    public static int sum(int[] array) {\n"
            + "        if (array == null || array.length == 0) return 0;\n"
            + "        int total = 0;\n"
            + "        for (int num : array) {\n"
            + "            total += num;\n"
            + "        }\n"
            + "        return total;\n"
            + "    }\n"
            + "}\n\n"
            + "Génère le code COMPLET pour : " + task + "\n"
            + "Commence par 'public class " + className + " {'\n"
            + "CODE COMPLET SANS TODO :";
    }
    
    private boolean hasRealImplementation(String code) {
        if (code == null || code.isBlank()) return false;
        
        String lower = code.toLowerCase();
        if (lower.contains("todo") || lower.contains("// todo")) {
            return false;
        }
        
        boolean hasReturn = code.contains("return ");
        boolean hasLoop = code.contains("for ") || code.contains("while ");
        boolean hasCondition = code.contains("if ") || code.contains("else");
        boolean hasAssignment = code.contains("=") && !code.contains("//");
        boolean hasMethodCall = code.contains("(") && code.contains(")") 
            && !code.contains("public static") && !code.contains("private static");
        
        return hasReturn || hasLoop || hasCondition || (hasAssignment && hasMethodCall);
    }
    
    private String createIntelligentFallback(String className, String task) {
        String taskLower = task.toLowerCase();
        
        if (taskLower.contains("somme") || taskLower.contains("sum")) {
            return "public class " + className + " {\n"
                + "    public static int sum(int[] array) {\n"
                + "        if (array == null || array.length == 0) return 0;\n"
                + "        int total = 0;\n"
                + "        for (int num : array) {\n"
                + "            total += num;\n"
                + "        }\n"
                + "        return total;\n"
                + "    }\n"
                + "}";
        }
        
        if (taskLower.contains("invers") || taskLower.contains("reverse")) {
            return "public class " + className + " {\n"
                + "    public static String reverse(String str) {\n"
                + "        if (str == null || str.isEmpty()) return str;\n"
                + "        StringBuilder reversed = new StringBuilder();\n"
                + "        for (int i = str.length() - 1; i >= 0; i--) {\n"
                + "            reversed.append(str.charAt(i));\n"
                + "        }\n"
                + "        return reversed.toString();\n"
                + "    }\n"
                + "}";
        }
        
        if (taskLower.contains("maximum") || taskLower.contains("max")) {
            return "public class " + className + " {\n"
                + "    public static int findMax(int[] array) {\n"
                + "        if (array == null || array.length == 0) throw new IllegalArgumentException();\n"
                + "        int max = array[0];\n"
                + "        for (int i = 1; i < array.length; i++) {\n"
                + "            if (array[i] > max) max = array[i];\n"
                + "        }\n"
                + "        return max;\n"
                + "    }\n"
                + "}";
        }
        
        if (taskLower.contains("compter") || taskLower.contains("count")) {
            return "public class " + className + " {\n"
                + "    public static int count(String str, char ch) {\n"
                + "        if (str == null) return 0;\n"
                + "        int count = 0;\n"
                + "        for (int i = 0; i < str.length(); i++) {\n"
                + "            if (str.charAt(i) == ch) count++;\n"
                + "        }\n"
                + "        return count;\n"
                + "    }\n"
                + "}";
        }
        
        return "public class " + className + " {\n"
            + "    public static int solve(int[] input) {\n"
            + "        if (input == null || input.length == 0) return 0;\n"
            + "        int result = 0;\n"
            + "        for (int value : input) {\n"
            + "            result += value;\n"
            + "        }\n"
            + "        return result;\n"
            + "    }\n"
            + "}";
    }
    
    private boolean needsMain(String task) {
        if (task == null) return false;
        String t = task.toLowerCase(Locale.ROOT);
        return t.contains("programme complet")
            || t.contains("méthode main")
            || t.contains("main(");
    }

    private String fixBraces(String code) {
        int open = 0, close = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') open++;
            else if (c == '}') close++;
        }
        String result = code;
        while (close < open) {
            result += "\n}";
            close++;
        }
        return result;
    }

    // ============================================================
    // 5) Starter code: Extract structure from solution + TODO
    // ============================================================
    private String generateStarterCodeFromSolution(String solution, String expectedClassName) {
        if (solution == null || solution.isBlank() || solution.contains("TODO")) {
            return createStarterCodeFallback(null, expectedClassName);
        }

        String methodSignature = extractFirstMethodSignature(solution);
        if (methodSignature == null || methodSignature.isEmpty()) {
            return createStarterCodeFallback(solution, expectedClassName);
        }

        StringBuilder starter = new StringBuilder();
        String[] lines = solution.split("\n");
        boolean inClass = false;
        boolean methodFound = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") || trimmed.startsWith("import ")) {
                starter.append(line).append("\n");
                continue;
            }
            if (trimmed.contains("class ") && !inClass) {
                starter.append("public class ").append(expectedClassName).append(" {\n");
                inClass = true;
                continue;
            }
            if (isMethodSignature(trimmed) && !methodFound) {
                appendMethodStub(starter, trimmed);
                methodFound = true;
                continue;
            }
        }

        if (!inClass || !methodFound) {
            return createStarterCodeFromSignature(methodSignature, expectedClassName);
        }

        starter.append("}\n");
        return starter.toString().trim();
    }
    
    private String extractFirstMethodSignature(String solution) {
        if (solution == null || solution.isBlank()) {
            return null;
        }
        
        String[] lines = solution.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static"))
                && trimmed.contains("(") && trimmed.contains(")")) {
                int openParen = trimmed.indexOf('(');
                int closeParen = trimmed.indexOf(')');
                if (openParen > 0 && closeParen > openParen) {
                    String signature = trimmed.substring(0, closeParen + 1);
                    if (!signature.endsWith("{")) {
                        signature += " {";
                    }
                    return signature;
                }
            }
        }
        return null;
    }
    
    private String createStarterCodeFromSignature(String methodSignature, String expectedClassName) {
        if (methodSignature == null || methodSignature.isEmpty()) {
            return createStarterCodeFallback(null, expectedClassName);
        }
        
        StringBuilder starter = new StringBuilder();
        starter.append("public class ").append(expectedClassName).append(" {\n");
        starter.append("    ").append(methodSignature).append("\n");
        starter.append("        // TODO: Implémentez votre solution ici\n");
        starter.append(getReturnStatement(methodSignature));
        starter.append("}\n");
        return starter.toString().trim();
    }
    
    private boolean isMethodSignature(String trimmed) {
        return (trimmed.startsWith("public static") || trimmed.startsWith("private static"))
            && trimmed.contains("(") && trimmed.contains(")") && trimmed.endsWith("{");
    }
    
    private void appendMethodStub(StringBuilder starter, String methodSignature) {
        starter.append("    ").append(methodSignature).append("\n");
        starter.append("        // TODO: Implémentez votre solution ici\n");
        starter.append(getReturnStatement(methodSignature));
    }
    
    private String getReturnStatement(String methodSignature) {
        return ReturnStatementStrategy.findStrategy(methodSignature).getReturnStatement();
    }
    
    private enum ReturnStatementStrategy {
        VOID(sig -> sig.contains(" void "), "    }\n"),
        NUMERIC(sig -> sig.contains(" int ") || sig.contains(" long ") 
            || sig.contains(" double ") || sig.contains(" float "), "        return 0;\n    }\n"),
        BOOLEAN(sig -> sig.contains(" boolean "), "        return false;\n    }\n"),
        STRING(sig -> sig.contains(" String "), "        return \"\";\n    }\n"),
        DEFAULT(sig -> true, "        return null;\n    }\n");
        
        private final java.util.function.Predicate<String> matcher;
        private final String returnStatement;
        
        ReturnStatementStrategy(java.util.function.Predicate<String> matcher, String returnStatement) {
            this.matcher = matcher;
            this.returnStatement = returnStatement;
        }
        
        static ReturnStatementStrategy findStrategy(String methodSignature) {
            for (ReturnStatementStrategy strategy : values()) {
                if (strategy != DEFAULT && strategy.matcher.test(methodSignature)) {
                    return strategy;
                }
            }
            return DEFAULT;
        }
        
        String getReturnStatement() {
            return returnStatement;
        }
    }

    private String createStarterCodeFallback(String solution, String expectedClassName) {
        return "public class " + expectedClassName + " {\n"
            + "    public static void solve() {\n"
            + "        // TODO: Implémentez votre solution ici\n"
            + "    }\n"
            + "}";
    }

    // ============================================================
    // 6) JUnit 5 test generation
    // ============================================================
    private String generateJUnitTests(String task, String solution, String className) {
        String tests = null;
        
        // Try to generate from solution first
        if (solution != null && !solution.isBlank() && !solution.contains("TODO")) {
            String expectedTestClassName = className + "Test";
            String methodInfo = extractMethodInfoForTests(solution);
            String prompt = buildTestPrompt(className, methodInfo, expectedTestClassName, task);
            
            String rawTests = callLlamaAPI(prompt, 500);
            tests = cleanJavaTestSnippet(rawTests, expectedTestClassName);

            // Validate generated tests
            if (tests == null || tests.isBlank() || !tests.contains("@Test") 
                || tests.contains("TODO") || !hasRealTestAssertions(tests)) {
                logger.warn("LLM-generated tests are invalid, trying fallback");
                tests = null; // Will use fallback
            }
        }
        
        // Fallback: generate from solution code structure
        if (tests == null || tests.isBlank()) {
            if (solution != null && !solution.isBlank()) {
                logger.info("Generating tests from solution structure");
                tests = generateTestsFromSolution(solution, className);
            } else {
                logger.info("Generating tests from task description");
                tests = generateTestsFromTask(task, className);
            }
        }
        
        // Final validation: ensure we have valid tests
        if (tests == null || tests.isBlank() || !tests.contains("@Test")) {
            logger.warn("All test generation methods failed, using minimal fallback");
            tests = createMinimalTestFallback(className);
        }
        
        // Ensure tests use correct method name from solution
        if (solution != null && !solution.isBlank()) {
            String actualMethodName = extractMethodNameFromSolution(solution);
            if (actualMethodName != null && !actualMethodName.isEmpty()) {
                tests = fixMethodNameInTests(tests, className, actualMethodName);
            }
        }
        
        return tests.trim();
    }
    
    private String createMinimalTestFallback(String className) {
        return "import org.junit.jupiter.api.Test;\n"
            + "import static org.junit.jupiter.api.Assertions.*;\n\n"
            + "public class " + className + "Test {\n\n"
            + "    @Test\n"
            + "    void testCasBasique() {\n"
            + "        // TODO: Ajoutez vos tests ici\n"
            + "        assertTrue(true);\n"
            + "    }\n"
            + "}\n";
    }
    
    private String fixMethodNameInTests(String tests, String className, String correctMethodName) {
        if (tests == null || tests.isBlank() || correctMethodName == null || correctMethodName.isEmpty()) {
            return tests;
        }
        
        // Replace common incorrect method names with the correct one
        String[] incorrectNames = {"check", "solve", "test", "calculate"};
        String result = tests;
        for (String incorrect : incorrectNames) {
            if (!incorrect.equals(correctMethodName)) {
                // Replace className.incorrectMethod( with className.correctMethodName(
                result = result.replaceAll(
                    className + "\\." + incorrect + "\\(",
                    className + "." + correctMethodName + "("
                );
            }
        }
        return result;
    }
    
    private String extractMethodInfoForTests(String solution) {
        String methodSignature = extractFirstMethodSignature(solution);
        if (methodSignature != null) {
            return methodSignature;
        }
        
        String[] lines = solution.split("\n");
        StringBuilder info = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("public static") && trimmed.contains("(")) {
                int endIdx = trimmed.indexOf(')');
                if (endIdx > 0) {
                    info.append(trimmed.substring(0, endIdx + 1)).append("\n");
                }
            }
        }
        return info.toString().trim();
    }
    
    private boolean hasRealTestAssertions(String tests) {
        if (tests == null || tests.isBlank()) return false;
        return tests.contains("assertEquals") || tests.contains("assertTrue") 
            || tests.contains("assertFalse") || tests.contains("assertNotNull");
    }
    
    private String generateTestsFromTask(String task, String className) {
        String methodName = inferMethodNameFromTask(task);
        String returnType = inferReturnTypeFromTask(task);
        String paramType = inferParamTypeFromTask(task);
        
        // Ensure we have valid values
        if (methodName == null || methodName.isEmpty()) {
            methodName = "solve";
        }
        if (returnType == null || returnType.isEmpty()) {
            returnType = "int";
        }
        if (paramType == null || paramType.isEmpty()) {
            paramType = "int[]";
        }
        
        logger.info("Generating tests from task with methodName={}, returnType={}, paramType={}", 
            methodName, returnType, paramType);
        
        String tests = buildIntelligentTests(className, methodName, returnType, paramType);
        
        // Ensure tests are valid
        if (tests == null || tests.isBlank() || !tests.contains("@Test")) {
            logger.warn("buildIntelligentTests returned invalid tests, using minimal fallback");
            return createMinimalTestFallback(className);
        }
        
        return tests;
    }
    
    private String generateTestsFromSolution(String solution, String className) {
        if (solution == null || solution.isBlank()) {
            logger.warn("Solution is null or blank, using task-based generation");
            return generateTestsFromTask(null, className);
        }
        
        String methodName = extractMethodNameFromSolution(solution);
        String returnType = extractReturnTypeFromSolution(solution);
        String paramType = extractParamTypeFromSolution(solution);
        
        // Ensure we have valid values
        if (methodName == null || methodName.isEmpty()) {
            logger.warn("Could not extract method name from solution, using 'solve' as fallback");
            methodName = "solve";
        }
        if (returnType == null || returnType.isEmpty()) {
            logger.warn("Could not extract return type from solution, using 'int' as fallback");
            returnType = "int";
        }
        if (paramType == null || paramType.isEmpty()) {
            logger.warn("Could not extract parameter type from solution, using 'int[]' as fallback");
            paramType = "int[]";
        }
        
        logger.info("Generating tests with methodName={}, returnType={}, paramType={}", 
            methodName, returnType, paramType);
        
        String tests = buildIntelligentTests(className, methodName, returnType, paramType);
        
        // Ensure tests are valid
        if (tests == null || tests.isBlank() || !tests.contains("@Test")) {
            logger.warn("buildIntelligentTests returned invalid tests, using minimal fallback");
            return createMinimalTestFallback(className);
        }
        
        return tests;
    }
    
    private String extractMethodNameFromSolution(String solution) {
        if (solution == null || solution.isBlank()) {
            return null;
        }
        
        String[] lines = solution.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static"))
                && trimmed.contains("(") && trimmed.contains(")")) {
                // Extract method name: public static int methodName(...)
                int startIdx = trimmed.indexOf("static");
                if (startIdx >= 0) {
                    String afterStatic = trimmed.substring(startIdx + "static".length()).trim();
                    // Find the method name (word before the opening parenthesis)
                    int openParen = afterStatic.indexOf('(');
                    if (openParen > 0) {
                        String beforeParen = afterStatic.substring(0, openParen).trim();
                        String[] parts = beforeParen.split("\\s+");
                        if (parts.length > 0) {
                            return parts[parts.length - 1]; // Last word before (
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private String extractReturnTypeFromSolution(String solution) {
        if (solution == null || solution.isBlank()) {
            return "int";
        }
        
        String[] lines = solution.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static"))
                && trimmed.contains("(")) {
                // Extract return type: public static ReturnType methodName(...)
                int staticIdx = trimmed.indexOf("static");
                if (staticIdx >= 0) {
                    String afterStatic = trimmed.substring(staticIdx + "static".length()).trim();
                    int openParen = afterStatic.indexOf('(');
                    if (openParen > 0) {
                        String beforeParen = afterStatic.substring(0, openParen).trim();
                        String[] parts = beforeParen.split("\\s+");
                        if (parts.length >= 2) {
                            return parts[0]; // Return type is before method name
                        } else if (parts.length == 1) {
                            return "void";
                        }
                    }
                }
            }
        }
        return "int";
    }
    
    private String extractParamTypeFromSolution(String solution) {
        if (solution == null || solution.isBlank()) {
            return "int[]";
        }
        
        String[] lines = solution.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static"))
                && trimmed.contains("(") && trimmed.contains(")")) {
                // Extract parameter type from method signature
                int openParen = trimmed.indexOf('(');
                int closeParen = trimmed.indexOf(')');
                if (openParen > 0 && closeParen > openParen) {
                    String params = trimmed.substring(openParen + 1, closeParen).trim();
                    if (params.isEmpty()) {
                        return "int[]";
                    }
                    // Extract first parameter type
                    String[] paramParts = params.split("\\s+");
                    if (paramParts.length > 0) {
                        String paramType = paramParts[0];
                        // Handle array types like int[]
                        if (params.contains("[]")) {
                            return paramType + "[]";
                        }
                        return paramType;
                    }
                }
            }
        }
        return "int[]";
    }
    
    private String inferMethodNameFromTask(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("somme") || lower.contains("sum")) return "sum";
        if (lower.contains("invers") || lower.contains("reverse")) return "reverse";
        if (lower.contains("maximum") || lower.contains("max")) return "findMax";
        if (lower.contains("compter") || lower.contains("count")) return "count";
        if (lower.contains("vérif") || lower.contains("check")) return "check";
        return "solve";
    }
    
    private String inferReturnTypeFromTask(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("chaîne") || lower.contains("string") || lower.contains("mot")) return "String";
        if (lower.contains("vérif") || lower.contains("check") || lower.contains("est")) return "boolean";
        return "int";
    }
    
    private String inferParamTypeFromTask(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("tableau") || lower.contains("array") || lower.contains("liste")) return "int[]";
        if (lower.contains("chaîne") || lower.contains("string") || lower.contains("mot")) return "String";
        return "int[]";
    }
    
    private String buildIntelligentTests(String className, String methodName, String returnType, String paramType) {
        StringBuilder tests = new StringBuilder();
        tests.append("import org.junit.jupiter.api.Test;\n");
        tests.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        tests.append("public class ").append(className).append("Test {\n\n");
        
        tests.append("    @Test\n");
        tests.append("    void testCasBasique() {\n");
        if ("int[]".equals(paramType)) {
            tests.append("        int[] array = {1, 2, 3, 4, 5};\n");
            if ("int".equals(returnType)) {
                tests.append("        assertEquals(15, ").append(className).append(".").append(methodName).append("(array));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(array));\n");
            } else if ("String".equals(returnType)) {
                tests.append("        assertNotNull(").append(className).append(".").append(methodName).append("(array));\n");
            }
        } else if ("String".equals(paramType)) {
            tests.append("        String input = \"hello\";\n");
            if ("String".equals(returnType)) {
                tests.append("        assertEquals(\"olleh\", ").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("int".equals(returnType)) {
                tests.append("        assertEquals(5, ").append(className).append(".").append(methodName).append("(input));\n");
            }
        } else if ("int".equals(paramType)) {
            tests.append("        int input = 42;\n");
            if ("int".equals(returnType)) {
                tests.append("        assertEquals(42, ").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(input));\n");
            }
        }
        tests.append("    }\n\n");
        
        tests.append("    @Test\n");
        tests.append("    void testCasLimite() {\n");
        if ("int[]".equals(paramType)) {
            tests.append("        int[] empty = {};\n");
            if ("int".equals(returnType)) {
                tests.append("        assertEquals(0, ").append(className).append(".").append(methodName).append("(empty));\n");
                tests.append("        assertEquals(0, ").append(className).append(".").append(methodName).append("(null));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertFalse(").append(className).append(".").append(methodName).append("(empty));\n");
                tests.append("        assertFalse(").append(className).append(".").append(methodName).append("(null));\n");
            }
        } else if ("String".equals(paramType)) {
            if ("String".equals(returnType)) {
                tests.append("        assertEquals(\"\", ").append(className).append(".").append(methodName).append("(\"\"));\n");
                tests.append("        assertNull(").append(className).append(".").append(methodName).append("(null));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertFalse(").append(className).append(".").append(methodName).append("(\"\"));\n");
                tests.append("        assertFalse(").append(className).append(".").append(methodName).append("(null));\n");
            } else if ("int".equals(returnType)) {
                tests.append("        assertEquals(0, ").append(className).append(".").append(methodName).append("(\"\"));\n");
                tests.append("        assertEquals(0, ").append(className).append(".").append(methodName).append("(null));\n");
            }
        }
        tests.append("    }\n\n");
        
        tests.append("    @Test\n");
        tests.append("    void testCasComplexe() {\n");
        if ("int[]".equals(paramType)) {
            tests.append("        int[] array = {10, 20, 30, 40, 50};\n");
            if ("int".equals(returnType)) {
                tests.append("        assertEquals(150, ").append(className).append(".").append(methodName).append("(array));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(array));\n");
            }
        } else if ("String".equals(paramType)) {
            tests.append("        String input = \"hello world\";\n");
            if ("String".equals(returnType)) {
                tests.append("        assertEquals(\"dlrow olleh\", ").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("int".equals(returnType)) {
                tests.append("        assertEquals(11, ").append(className).append(".").append(methodName).append("(input));\n");
            }
        } else if ("int".equals(paramType)) {
            tests.append("        int input = 100;\n");
            if ("int".equals(returnType)) {
                tests.append("        assertEquals(100, ").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(input));\n");
            }
        }
        tests.append("    }\n");
        tests.append("}\n");
        
        return tests.toString();
    }
    
    private String buildTestPrompt(String className, String codeToTest, String expectedTestClassName, String task) {
        // Extract method name from codeToTest to ensure correct usage
        String methodName = extractMethodNameFromSignature(codeToTest);
        String methodNameHint = methodName != null && !methodName.isEmpty() 
            ? " (nom de méthode EXACT : " + methodName + ")" 
            : "";
        
        return "URGENT: Génère des tests JUnit 5 COMPLETS avec des ASSERTIONS RÉELLES. PAS de TODO.\n\n"
            + "=== EXERCICE ===\n" + (task != null ? task : "") + "\n\n"
            + "=== CLASSE À TESTER ===\n"
            + "Classe : " + className + "\n"
            + "Méthode(s) :\n" + codeToTest + "\n"
            + (methodName != null && !methodName.isEmpty() 
                ? "Nom de méthode à utiliser : " + methodName + "\n" 
                : "") + "\n"
            + "=== EXIGENCES ABSOLUES ===\n"
            + "1. Nom de classe de test EXACT : " + expectedTestClassName + "\n"
            + "2. Imports OBLIGATOIRES :\n"
            + "   import org.junit.jupiter.api.Test;\n"
            + "   import static org.junit.jupiter.api.Assertions.*;\n"
            + "3. Minimum 3 tests avec ASSERTIONS RÉELLES (PAS de TODO) :\n"
            + "   - testCasBasique() : valeurs normales avec assertEquals concret\n"
            + "   - testCasLimite() : null, tableaux vides, chaînes vides avec assertions\n"
            + "   - testCasComplexe() : cas multiples avec assertions\n"
            + "4. Chaque test DOIT appeler " + className + "." 
            + (methodName != null && !methodName.isEmpty() ? methodName : "méthode") 
            + "(...) et vérifier avec assertEquals/assertTrue/assertFalse\n"
            + "5. Utilise EXACTEMENT le nom de méthode de la signature ci-dessus" + methodNameHint + "\n"
            + "6. INTERDICTION ABSOLUE : PAS de TODO, PAS de code vide\n"
            + "7. PAS de markdown, UNIQUEMENT du code de test fonctionnel\n\n"
            + "=== EXEMPLE CONCRET ===\n"
            + "Si méthode = " + (methodName != null && !methodName.isEmpty() ? methodName : "sum") + "(int[] array) :\n"
            + "@Test\n"
            + "void testCasBasique() {\n"
            + "    int[] array = {1, 2, 3};\n"
            + "    assertEquals(6, " + className + "." 
            + (methodName != null && !methodName.isEmpty() ? methodName : "sum") + "(array));\n"
            + "}\n\n"
            + "@Test\n"
            + "void testCasLimite() {\n"
            + "    assertEquals(0, " + className + "." 
            + (methodName != null && !methodName.isEmpty() ? methodName : "sum") + "(new int[]{}));\n"
            + "    assertEquals(0, " + className + "." 
            + (methodName != null && !methodName.isEmpty() ? methodName : "sum") + "(null));\n"
            + "}\n\n"
            + "=== FORMAT ===\n"
            + "Commence par 'import org.junit.jupiter.api.Test;'\n"
            + "Génère des tests COMPLETS avec assertions RÉELLES pour : " + (task != null ? task : className) + "\n"
            + "Utilise le nom de méthode EXACT de la signature fournie.\n\n"
            + "Code des tests :";
    }
    
    private String extractMethodNameFromSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            return null;
        }
        
        // Look for method name pattern: returnType methodName(...)
        String[] lines = signature.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static"))
                && trimmed.contains("(") && trimmed.contains(")")) {
                int openParen = trimmed.indexOf('(');
                if (openParen > 0) {
                    String beforeParen = trimmed.substring(0, openParen).trim();
                    String[] parts = beforeParen.split("\\s+");
                    if (parts.length >= 2) {
                        return parts[parts.length - 1]; // Last word before (
                    }
                }
            }
        }
        return null;
    }

    // ============================================================
    // 7) Hint generation
    // ============================================================
    /**
     * Generates a hint for a failed test (overload without problem statement).
     */
    public String generateHint(String testName, String testCode,
                               String studentCode, String errorMessage) {
        return generateHint(testName, testCode, studentCode, errorMessage, null);
    }

    /**
     * Main hint generator.
     * Key improvements:
     * - Only sends the relevant student class + (optionally) the most relevant method body.
     * - Forces the LLM to output STRICT JSON on a single line.
     * - Parses JSON and rebuilds a clean human-readable hint (keeps newlines for code snippet).
     * - Avoids collapsing whitespace (which previously made hints unreadable).
     */
    public String generateHint(String testName, String testCode,
                               String studentCode, String errorMessage, String problemStatement) {
        logger.info("Generating hint for failed test: {}", testName);

        // Build a strong context for the LLM (but keep it short)
        String exerciseContext = buildExerciseContext(problemStatement);

        // Extract likely method name from the test (ClassName.methodName(...))
        String targetMethod = extractMethodNameFromTest(testCode);

        // Keep only the relevant student code: first class + optionally the target method body
        String sanitizedStudentCode = sanitizeStudentCodeForHint(studentCode);
        String focusedStudentCode = focusStudentCodeOnMethod(sanitizedStudentCode, targetMethod);

        // Build prompt with strict JSON output contract
        String prompt = buildHintPromptJson(
            testName,
            testCode,
            focusedStudentCode,
            errorMessage,
            exerciseContext,
            targetMethod
        );

        // Call LLM with retry (but accept only valid JSON)
        String raw = generateHintWithRetryJson(prompt, 2);

        // Convert JSON -> clean hint text
        String hint = postProcessHintJson(raw);

        // Fallback if JSON is missing/invalid
        if (hint != null && hint.trim().length() >= 10) {
            return hint.trim();
        }
        return getDefaultHint(errorMessage);
    }
    
    /**
     * Calls LLM and returns the raw response ONLY if it contains a valid JSON object.
     * Otherwise retries up to maxRetries times.
     */
    private String generateHintWithRetryJson(String prompt, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String raw = callLlamaAPIForHintJson(prompt, 320); // Lower token budget keeps it short
            if (raw != null && !raw.isBlank()) {
                String json = extractFirstJsonObject(raw);
                if (json != null) {
                    logger.info("Hint JSON extracted on attempt {}: {}",
                        attempt + 1, json.length() > 200 ? json.substring(0, 200) + "..." : json);
                    return json;
                }
            }
            logger.warn("Hint JSON not found on attempt {}", attempt + 1);
        }
        return "";
    }

    /**
     * LLM call for hint generation (JSON-only).
     * Important changes:
     * - Uses stop tokens that prevent long rambling.
     * - Keeps temperature moderate.
     */
    private String callLlamaAPIForHintJson(String prompt, int maxTokens) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("n_predict", maxTokens);
            requestBody.put("temperature", 0.25);
            requestBody.put("top_p", 0.9);
            requestBody.put("top_k", 40);
            requestBody.put("repeat_penalty", 1.12);

            // Stop early when it starts adding separators or extra text.
            // IMPORTANT: we do NOT stop on "===" in the prompt because we no longer need "===" in the output.
            requestBody.put("stop", new String[]{"\n\n", "```", "</s>", "=== "});

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                llamacppBaseUrl + "/completion",
                request,
                String.class
            );

            return processResponse(response);
        } catch (Exception e) {
            logger.error("Error calling LLM for hint generation (JSON)", e);
            return "";
        }
    }
    
    private String extractTestExpectation(String testCode) {
        if (testCode == null || testCode.isEmpty() || !testCode.contains("assertEquals")) {
            return "";
        }
        Pattern pattern = Pattern.compile("assertEquals\\(([^,]+),\\s*([^)]+)\\)");
        Matcher matcher = pattern.matcher(testCode);
        if (matcher.find()) {
            return "Le test attend que la méthode retourne " + matcher.group(1) 
                + " quand on appelle avec " + matcher.group(2) + ".";
        }
        return "";
    }
    
    private String buildExerciseContext(String problemStatement) {
        return (problemStatement != null && !problemStatement.trim().isEmpty())
            ? problemStatement
            : "L'étudiant doit implémenter une fonction qui inverse une chaîne de caractères.";
    }
    
    private String analyzeStudentCode(String studentCode) {
        if (studentCode == null || studentCode.isEmpty()) {
            return "";
        }
        StringBuilder logicIssue = new StringBuilder();
        if (studentCode.contains("return \"\";") || studentCode.contains("return null;")) {
            logicIssue.append("Le code retourne toujours une valeur vide au lieu d'implémenter la logique. ");
        }
        if (studentCode.contains("// TODO")) {
            logicIssue.append("La logique n'est pas implémentée (présence de TODO). ");
        }
        String methodBody = extractMethodBody(studentCode);
        if (isMethodBodyEmpty(methodBody)) {
            logicIssue.append("Le corps de la méthode est vide ou ne fait que retourner une valeur par défaut. ");
        }
        return logicIssue.toString();
    }
    
    private boolean isMethodBodyEmpty(String methodBody) {
        if (methodBody == null) return false;
        String trimmed = methodBody.trim();
        return trimmed.isEmpty() 
            || trimmed.equals("return \"\";") 
            || trimmed.equals("return null;");
    }
    
    /**
     * Builds a JSON-only prompt that prevents messy "essay" outputs.
     */
    private String buildHintPromptJson(String testName,
                                      String testCode,
                                      String focusedStudentCode,
                                      String errorMessage,
                                      String exerciseContext,
                                      String targetMethodName) {

        String shortTest = trimToMax(testCode, 450);  // Keep it short to avoid confusion
        String err = (errorMessage == null || errorMessage.isBlank()) ? "Test failed" : errorMessage;

        String methodLine = (targetMethodName == null || targetMethodName.isBlank())
            ? ""
            : "Target method name: " + targetMethodName + "\n";

        return ""
            + "You are a Java programming teacher.\n"
            + "Your job: give a short, actionable hint.\n\n"
            + "CONTEXT (exercise):\n"
            + exerciseContext + "\n\n"
            + "FAILURE:\n"
            + "Test name: " + safeOneLine(testName) + "\n"
            + "Error: " + safeOneLine(err) + "\n"
            + methodLine + "\n"
            + "STUDENT CODE (only relevant parts):\n"
            + focusedStudentCode + "\n\n"
            + "TEST (for expected behavior, truncated):\n"
            + shortTest + "\n\n"
            + "STRICT OUTPUT RULES:\n"
            + "Return ONLY ONE JSON object on ONE line. No extra text.\n"
            + "Format exactly:\n"
            + "{\"problem\":\"...\",\"fix\":\"...\",\"snippet\":\"...\"}\n"
            + "Rules:\n"
            + "- problem: 1 sentence, the precise mistake.\n"
            + "- fix: 1 sentence, what to change.\n"
            + "- snippet: either empty string \"\" or a tiny Java snippet (max 3 lines).\n"
            + "- NEVER include package/import/Spring/JPA/annotations.\n"
            + "- NEVER mention other classes/files.\n"
            + "- Do not use markdown.\n\n"
            + "JSON:";
    }
    
    /**
     * Converts the JSON object into a clean hint.
     * Keeps snippet formatting (newlines) intact.
     */
    private String postProcessHintJson(String json) {
        if (json == null || json.isBlank()) return null;

        try {
            JsonNode node = objectMapper.readTree(json);

            String problem = safeText(node, "problem");
            String fix = safeText(node, "fix");
            String snippet = safeText(node, "snippet");

            // Safety: reject if it looks like project/framework leakage
            if (looksLikeProjectLeak(problem) || looksLikeProjectLeak(fix) || looksLikeProjectLeak(snippet)) {
                logger.error("Hint rejected: looks like project/framework leak. json={}", trimToMax(json, 220));
                return null;
            }

            StringBuilder sb = new StringBuilder();

            // Build a short, clean hint
            if (!problem.isBlank()) sb.append(problem.trim());
            if (!fix.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(fix.trim());
            }

            // Add snippet on new lines if present
            if (!snippet.isBlank()) {
                String cleanedSnippet = cleanSnippet(snippet);
                if (!cleanedSnippet.isBlank()) {
                    sb.append("\n").append(cleanedSnippet);
                }
            }

            String result = sb.toString().trim();
            return result.isBlank() ? null : result;
        } catch (Exception e) {
            logger.warn("Failed to parse hint JSON: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Sanitize student code to only include the relevant class, not the entire project.
     * This prevents LLM from seeing and repeating entire Spring Boot project files.
     * Supports both "public class" and "class" declarations.
     */
    private String sanitizeStudentCodeForHint(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            return "";
        }
        
        // Try to find first class declaration (public or non-public)
        int publicIdx = studentCode.indexOf("public class ");
        int classIdx = studentCode.indexOf("class ");
        
        int idx = -1;
        if (publicIdx >= 0 && (classIdx < 0 || publicIdx < classIdx)) {
            idx = publicIdx;
        } else if (classIdx >= 0) {
            idx = classIdx;
        }
        
        if (idx >= 0) {
            studentCode = studentCode.substring(idx);
            // Find the end of this class (next "class " or end of string)
            int nextClassIdx = studentCode.indexOf("\nclass ", 1);
            if (nextClassIdx < 0) {
                nextClassIdx = studentCode.indexOf("\npublic class ", 1);
            }
            if (nextClassIdx > 0) {
                studentCode = studentCode.substring(0, nextClassIdx);
            }
        }
        
        // Prevent extremely long context from polluting LLM
        // If student code is too long, it's likely the entire project
        if (studentCode.length() > 2000) {
            logger.warn("Student code too long ({} chars), truncating to 2000 chars", studentCode.length());
            studentCode = studentCode.substring(0, 2000) + "\n// ... (code truncated)";
        }
        
        return studentCode.trim();
    }
    
    
    private String getDefaultHint(String errorMessage) {
        if (errorMessage == null) {
            return HintStrategy.DEFAULT.getHint();
        }
        return HintStrategy.findStrategy(errorMessage).getHint();
    }
    
    private enum HintStrategy {
        NULL_POINTER(msg -> msg.contains("NullPointerException"),
            "NullPointerException détectée. Vérifie que l'objet n'est pas null avant utilisation: if (obj == null) return ...; ou if (obj != null) { ... }"),
        ARRAY_INDEX(msg -> msg.contains("ArrayIndexOutOfBoundsException"),
            "Index de tableau invalide. Utilise des indices entre 0 et array.length-1. Vérifie: if (i >= 0 && i < array.length) avant d'accéder à array[i]"),
        STRING_INDEX(msg -> msg.contains("StringIndexOutOfBoundsException"),
            "Index de chaîne invalide. Utilise des indices entre 0 et str.length()-1. Vérifie: if (i >= 0 && i < str.length()) avant d'accéder à str.charAt(i)"),
        COMPILATION_ERROR(msg -> msg.contains("cannot find symbol") || msg.contains("cannot resolve"),
            "Erreur de compilation: symbole introuvable. Vérifie l'orthographe des variables et méthodes, et que toutes les classes sont importées correctement."),
        TYPE_MISMATCH(msg -> msg.contains("incompatible types") || msg.contains("type mismatch"),
            "Incompatibilité de types. Vérifie que les types correspondent: int avec int, String avec String, etc."),
        DEFAULT(msg -> true,
            "Vérifie que ton code implémente correctement la logique attendue. Compare le résultat retourné avec ce que le test attend.");
        
        private final java.util.function.Predicate<String> matcher;
        private final String hint;
        
        HintStrategy(java.util.function.Predicate<String> matcher, String hint) {
            this.matcher = matcher;
            this.hint = hint;
        }
        
        static HintStrategy findStrategy(String errorMessage) {
            for (HintStrategy strategy : values()) {
                if (strategy != DEFAULT && strategy.matcher.test(errorMessage)) {
                    return strategy;
                }
            }
            return DEFAULT;
        }
        
        String getHint() {
            return hint;
        }
    }
    
    private String extractMethodBody(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        
        int startIdx = code.indexOf('{');
        if (startIdx == -1) {
            return null;
        }
        
        int braceCount = 0;
        int endIdx = startIdx;
        for (int i = startIdx; i < code.length(); i++) {
            if (code.charAt(i) == '{') {
                braceCount++;
            } else if (code.charAt(i) == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIdx = i;
                    break;
                }
            }
        }
        
        if (endIdx > startIdx) {
            return code.substring(startIdx + 1, endIdx).trim();
        }
        
        return null;
    }
    
    // ============================================================
    // Helper methods for JSON-based hint generation
    // ============================================================
    
    /**
     * Extracts a likely method name from test code: looks for "X.yyyy(" patterns.
     * Returns null if not found.
     */
    private String extractMethodNameFromTest(String testCode) {
        if (testCode == null || testCode.isBlank()) return null;

        // Example: assertEquals(6, ArraySum.sum(array));
        Pattern p = Pattern.compile("\\b\\w+\\.(\\w+)\\s*\\(");
        Matcher m = p.matcher(testCode);
        if (m.find()) {
            String name = m.group(1);
            // Filter out obvious non-targets
            if (!"assertEquals".equals(name) && !"assertTrue".equals(name) && !"assertFalse".equals(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Keeps only:
     * - the class header
     * - the target method body if found
     * - closes the class brace
     *
     * If target method name is null or not found, returns the original sanitized code.
     */
    private String focusStudentCodeOnMethod(String sanitizedStudentCode, String targetMethodName) {
        if (sanitizedStudentCode == null || sanitizedStudentCode.isBlank()) return "";
        if (targetMethodName == null || targetMethodName.isBlank()) return sanitizedStudentCode;

        // Try to extract method block: "targetMethodName(...){ ... }"
        String methodBlock = extractMethodBlock(sanitizedStudentCode, targetMethodName);
        if (methodBlock == null || methodBlock.isBlank()) {
            return sanitizedStudentCode; // fallback: keep full class
        }

        // Extract class declaration line
        int classIdx = sanitizedStudentCode.indexOf("class ");
        if (classIdx < 0) return sanitizedStudentCode;

        int classBraceIdx = sanitizedStudentCode.indexOf("{", classIdx);
        if (classBraceIdx < 0) return sanitizedStudentCode;

        String classHeader = sanitizedStudentCode.substring(classIdx, classBraceIdx + 1).trim();

        // Build focused class: header + method block + closing brace
        return classHeader + "\n" + methodBlock.trim() + "\n}";
    }

    /**
     * Extracts the full method block including braces for a given method name.
     * Works for "public static", "private static", and regular methods.
     */
    private String extractMethodBlock(String code, String methodName) {
        if (code == null || methodName == null) return null;

        // Find the method signature line containing " methodName("
        int sigIdx = indexOfMethodSignature(code, methodName);
        if (sigIdx < 0) return null;

        // Find the opening brace '{' of the method
        int openBraceIdx = code.indexOf("{", sigIdx);
        if (openBraceIdx < 0) return null;

        // Walk until matching closing brace
        int depth = 0;
        for (int i = openBraceIdx; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0) {
                return code.substring(sigIdx, i + 1);
            }
        }
        return null;
    }

    /**
     * Finds an index where a method signature likely starts.
     */
    private int indexOfMethodSignature(String code, String methodName) {
        // Simple robust approach: search for " methodName(" with boundaries
        Pattern p = Pattern.compile("(?m)^\\s*(public|private|protected)?\\s*(static\\s+)?[\\w<>\\[\\]]+\\s+"
            + Pattern.quote(methodName) + "\\s*\\(");
        Matcher m = p.matcher(code);
        return m.find() ? m.start() : -1;
    }

    /**
     * Extracts the first JSON object {...} from a response.
     * This tolerates minor extra text around it.
     */
    private String extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) return null;

        // Find the first '{' and then parse brace depth until it closes
        int start = text.indexOf('{');
        if (start < 0) return null;

        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0) {
                return text.substring(start, i + 1).trim();
            }
        }
        return null;
    }

    /**
     * Reads a field from JSON safely.
     */
    private String safeText(JsonNode node, String field) {
        if (node == null || field == null) return "";
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? "" : v.asText("");
    }

    /**
     * Prevents multi-line injection in log/prompt fields.
     */
    private String safeOneLine(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]+", " ").trim();
    }

    /**
     * Trims text to max length.
     */
    private String trimToMax(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    /**
     * Cleans snippet while preserving newlines (max 3 lines recommended by prompt).
     * Also removes markdown fences if the LLM accidentally included them.
     */
    private String cleanSnippet(String snippet) {
        if (snippet == null) return "";
        String s = snippet.trim();
        s = s.replaceAll("(?i)```[a-zA-Z]*", "");
        s = s.replaceAll("```", "");
        // Keep at most 3 lines to match the contract
        String[] lines = s.split("\\R");
        if (lines.length <= 3) return s;
        return String.join("\n", Arrays.copyOf(lines, 3)).trim();
    }

    /**
     * Detects obvious project/framework leakage in hint fields.
     * This is intentionally strict because hints should not contain Spring/JPA.
     */
    private boolean looksLikeProjectLeak(String text) {
        if (text == null || text.isBlank()) return false;
        String t = text.toLowerCase(Locale.ROOT);

        // Strong framework indicators
        if (t.contains("org.springframework")) return true;
        if (t.contains("jakarta.persistence") || t.contains("javax.persistence")) return true;
        if (t.contains("@service") || t.contains("@entity") || t.contains("@controller")) return true;
        if (t.contains("crudrepository") || t.contains("jparepository")) return true;

        // Avoid package/import in hints
        if (t.contains("package ")) return true;
        if (t.contains("import ")) return true;

        return false;
    }

    // ============================================================
    // 8) Subject / Concepts / Examples
    // ============================================================
    private String detectConceptsFromTask(String task, String difficulty) {
        StringBuilder sb = new StringBuilder();
        sb.append("Java, ").append(difficulty == null ? "L1" : difficulty);

        if (task != null) {
            String t = task.toLowerCase(Locale.ROOT);
            appendConceptIfContains(sb, t, "tableau", "array", "liste", "Tableaux");
            appendConceptIfContains(sb, t, "mot", "string", "chaîne", "Chaînes de caractères");
            appendConceptIfContains(sb, t, "tri", "trier", "sort", "Algorithmes de tri");
            appendConceptIfContains(sb, t, "recherche", "chercher", "find", "Recherche");
            appendConceptIfContains(sb, t, "récurs", "recurs", "Récursivité");
        }
        return sb.toString();
    }
    
    private void appendConceptIfContains(StringBuilder sb, String text, 
                                        String... keywords) {
        String concept = keywords[keywords.length - 1];
        for (int i = 0; i < keywords.length - 1; i++) {
            if (text.contains(keywords[i])) {
                sb.append("; ").append(concept);
                break;
            }
        }
    }

    private String generateExamplesFromTask(String task, String className, String solution) {
        // Add diagnostic logging
        logger.info("[EXAMPLES] task='{}'", task);
        
        if (task == null || task.isBlank()) {
            logger.warn("[EXAMPLES] USING FALLBACK. task is blank");
            return createDefaultExamples();
        }

        // Try to extract method signature from solution for more accurate examples
        String methodInfo = extractMethodInfoForExamples(solution);
        logger.info("[EXAMPLES] methodInfo='{}'", methodInfo != null ? methodInfo : "null");
        
        // Build enhanced prompt with solution context
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Génère EXACTEMENT 3 exemples CONCRETS et VARIÉS pour cet exercice.\n\n");
        promptBuilder.append("=== EXERCICE ===\n").append(task).append("\n\n");
        
        if (methodInfo != null && !methodInfo.isBlank()) {
            promptBuilder.append("=== SIGNATURE DE LA MÉTHODE ===\n");
            promptBuilder.append(methodInfo).append("\n");
            promptBuilder.append("(Utilise cette signature pour comprendre les types d'entrée et de sortie)\n\n");
        }
        
        promptBuilder.append("=== EXIGENCES ABSOLUES ===\n");
        promptBuilder.append("1. RENVOIE UNIQUEMENT un JSON array de 3 objets, SANS texte, SANS explication, SANS markdown\n");
        promptBuilder.append("2. Format EXACT: [{\"input\":\"...\",\"output\":\"...\"},{\"input\":\"...\",\"output\":\"...\"},{\"input\":\"...\",\"output\":\"...\"}]\n");
        promptBuilder.append("3. Les exemples DOIVENT correspondre EXACTEMENT à l'exercice décrit ci-dessus\n");
        promptBuilder.append("4. Exemples VARIÉS :\n");
        promptBuilder.append("   - Premier exemple : cas simple avec valeurs normales\n");
        promptBuilder.append("   - Deuxième exemple : cas avec valeurs multiples/complexes\n");
        promptBuilder.append("   - Troisième exemple : cas limite (tableau vide, null, chaîne vide, valeur négative, etc.)\n");
        promptBuilder.append("5. Utilise des valeurs CONCRÈTES et RÉALISTES qui illustrent bien l'exercice\n");
        promptBuilder.append("6. Les valeurs d'entrée et de sortie doivent être cohérentes avec l'exercice\n\n");
        promptBuilder.append("=== FORMAT ===\n");
        promptBuilder.append("RENVOIE UNIQUEMENT:\n");
        promptBuilder.append("[{\"input\":\"...\",\"output\":\"...\"},{\"input\":\"...\",\"output\":\"...\"},{\"input\":\"...\",\"output\":\"...\"}]\n\n");
        promptBuilder.append("Génère UNIQUEMENT le JSON array de 3 objets pour cet exercice :");
    
        // Try LLM generation (with retry on failure)
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String raw = callLlamaAPI(promptBuilder.toString(), 300);
                
                // Log raw response for debugging
                logger.info("[EXAMPLES] rawLLM='{}'", raw != null && raw.length() > 300 ? raw.substring(0, 300) + "..." : raw);
                
                // Extract JSON from response
                String jsonStr = extractJsonFromResponse(raw);
                logger.info("[EXAMPLES] extractedJson='{}'", jsonStr != null && jsonStr.length() > 200 ? jsonStr.substring(0, 200) + "..." : jsonStr);
                
                if (jsonStr == null || jsonStr.equals("[]")) {
                    if (attempt == 0) {
                        logger.warn("[EXAMPLES] No JSON found in response, retrying...");
                        continue;
                    }
                    logger.warn("[EXAMPLES] USING FALLBACK. rawEmpty={}, jsonStr=null", raw == null || raw.isBlank());
                    throw new IllegalArgumentException("No JSON found in response after retry");
                }
                
                JsonNode node = objectMapper.readTree(jsonStr);
                
                if (!node.isArray() || node.size() < 3) {
                    if (attempt == 0) {
                        logger.warn("[EXAMPLES] Invalid JSON array size: {}, retrying...", node.size());
                        continue;
                    }
                    logger.warn("[EXAMPLES] USING FALLBACK. Invalid array size: {}", node.size());
                    throw new IllegalArgumentException("Invalid JSON array or wrong size: " + node.size());
                }

                StringBuilder sb = new StringBuilder();
                int validExamples = 0;
                for (int i = 0; i < node.size() && validExamples < 3; i++) {
                    JsonNode example = node.get(i);
                    if (!example.isObject()) continue;
                    
                    String input = example.has("input") ? example.get("input").asText().trim() : "";
                    String output = example.has("output") ? example.get("output").asText().trim() : "";
                    
                    if (input.isEmpty() && output.isEmpty()) continue;
                    
                    sb.append("Entrée : ").append(input).append(" → Sortie : ").append(output);
                    if (validExamples < 2) {
                        sb.append("\n");
                    }
                    validExamples++;
                }
                
                String result = sb.toString().trim();
                if (result.isEmpty() || validExamples < 2) {
                    if (attempt == 0) {
                        logger.warn("[EXAMPLES] Not enough valid examples: {}, retrying...", validExamples);
                        continue;
                    }
                    logger.warn("[EXAMPLES] USING FALLBACK. Not enough valid examples: {}", validExamples);
                    throw new IllegalArgumentException("Not enough valid examples: " + validExamples);
                }
                
                logger.info("[EXAMPLES] Successfully generated {} examples from LLM", validExamples);
                return result;
            } catch (Exception e) {
                if (attempt == 0) {
                    logger.warn("[EXAMPLES] Failed to parse examples JSON (attempt {}), retrying: {}", attempt + 1, e.getMessage());
                    continue;
                }
                logger.error("[EXAMPLES] Failed to generate examples from LLM after retry: {}", e.getMessage());
            }
        }
        
        // If all attempts failed, return simple default based on solution (not task keywords)
        logger.warn("[EXAMPLES] USING FALLBACK. All LLM attempts failed. solutionBlank={}", solution == null || solution.isBlank());
        return createSolutionBasedDefaultExamples(solution);
    }
    
    private String createSolutionBasedDefaultExamples(String solution) {
        // Simple fallback - just return default examples
        return createDefaultExamples();
    }
    
    private String extractMethodInfoForExamples(String solution) {
        if (solution == null || solution.isBlank()) {
            return null;
        }
        
        // Extract first method signature for context
        String[] lines = solution.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static"))
                && trimmed.contains("(") && trimmed.contains(")")) {
                // Return a simplified version for prompt context
                int openParen = trimmed.indexOf('(');
                int closeParen = trimmed.indexOf(')');
                if (openParen > 0 && closeParen > openParen) {
                    return trimmed.substring(0, Math.min(closeParen + 1, trimmed.length()));
                }
            }
        }
        return null;
    }
    
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        
        // Remove markdown code blocks if present
        String r = response.replaceAll("```json", "").replaceAll("```", "").trim();
        
        // Use regex to extract JSON array - don't require it to start at beginning
        // This handles cases where LLM returns text before the JSON like "Voici des exemples :"
        Pattern jsonPattern = Pattern.compile("\\[\\s*\\{.*?\\}\\s*(?:,\\s*\\{.*?\\}\\s*)*\\]", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(r);
        if (matcher.find()) {
            return matcher.group(0);
        }
        
        return null;
    }
    
    
    private String createDefaultExamples() {
        // Neutral generic template - no specific algorithm implied
        return "Entrée : (exemple 1) → Sortie : (résultat 1)\n"
            + "Entrée : (exemple 2) → Sortie : (résultat 2)\n"
            + "Entrée : (cas limite) → Sortie : (résultat)";
    }

    // ============================================================
    // 9) Class name generation: based on task
    // ============================================================
    private String generateClassNameFromTask(String task) {
        if (task == null || task.trim().isEmpty()) {
            return "Solution";
        }
        String t = task.toLowerCase(Locale.ROOT);

        String predefined = getPredefinedClassName(t);
        if (predefined != null) {
            return predefined;
        }

        return generateClassNameFromWords(task);
    }
    
    private String getPredefinedClassName(String task) {
        if (task.contains("compter") && task.contains("mot")) {
            return "CountWords";
        }
        if ((task.contains("somme") || task.contains("sum"))
            && (task.contains("tableau") || task.contains("array") || task.contains("liste"))) {
            return "ArraySum";
        }
        if ((task.contains("maximum") || task.contains("plus grand") || task.contains("max"))
            && (task.contains("tableau") || task.contains("array") || task.contains("liste"))) {
            return "ArrayMax";
        }
        if (task.contains("palindrome")) {
            return "PalindromeChecker";
        }
        return null;
    }
    
    private String normalizeAscii(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = DIACRITICS.matcher(n).replaceAll("");   // É -> E, é -> e
        n = n.replaceAll("[^A-Za-z0-9]", "");       // keep only letters/digits
        return n;
    }

    private boolean isNoiseLeadingWord(String w) {
        return switch (w) {
            case "ecrire", "ecris", "ecrit",
                 "implementer", "implement", "impl", "implementerune", 
                 "creer", "create", "write",
                 "exercice", "exercise" -> true;
            default -> isStopWord(w);
        };
    }

    private boolean isStopWord(String w) {
        return switch (w) {
            case "les","des","une","un","la","le","du","de","d",
                 "pour","avec","dans","ou","où","que","qui",
                 "the","and","a","an","to","of",
                 "students","etudiants",
                 "fonction","function","methode","method","classe","class","programme","program" -> true;
            default -> false;
        };
    }

    private String generateClassNameFromWords(String task) {
        String clean = task == null ? "" : task.trim();
        if (clean.isEmpty()) return "Solution";

        String[] rawWords = clean.split("\\s+");

        // 1) 先跳过开头噪音词（Écrire/Créer/Implémenter/Exercice...）
        int i = 0;
        while (i < rawWords.length) {
            String w = normalizeAscii(rawWords[i]).toLowerCase(Locale.ROOT);
            if (w.isEmpty() || isNoiseLeadingWord(w)) {
                i++;
                continue;
            }
            break;
        }

        // 2) 从后面开始取最多 3 个"有意义"的词来拼类名
        StringBuilder sb = new StringBuilder();
        int picked = 0;
        for (; i < rawWords.length && picked < 3; i++) {
            String wRaw = normalizeAscii(rawWords[i]);
            if (wRaw.isEmpty()) continue;

            String w = wRaw.toLowerCase(Locale.ROOT);
            if (isStopWord(w) || isNoiseLeadingWord(w)) continue;
            if (w.length() < 3) continue;

            sb.append(Character.toUpperCase(wRaw.charAt(0)))
              .append(wRaw.substring(1).toLowerCase(Locale.ROOT));
            picked++;
        }

        String result = sb.toString().replaceAll("[^A-Za-z0-9_]", "");
        if (result.isEmpty() || !Character.isLetter(result.charAt(0))) return "Solution";
        return result;
    }

    // ============================================================
    // 10) Various utility functions
    // ============================================================
    private String sanitize(String text) {
        if (text == null) return "";
        String t = text;

        t = removeMarkdown(t);
        t = removePrefixes(t);
        t = removeQuotes(t);
        return t.trim();
    }
    
    private String removeMarkdown(String t) {
        t = t.replaceAll("(?i)```[a-zA-Z]*", "");
        t = t.replaceAll("```", "");
        return t;
    }
    
    private String removePrefixes(String t) {
        t = t.replaceAll("(?i)^\\s*example\\s*:\\s*", "");
        t = t.replaceAll("(?i)^\\s*solution\\s*:\\s*", "");
        t = t.replaceAll("(?i)^model:.*", "");
        return t.trim();
    }
    
    private String removeQuotes(String t) {
        if (t.toLowerCase(Locale.ROOT).startsWith("voici")) {
            int idx = t.indexOf(':');
            if (idx > 0 && idx + 1 < t.length()) {
                t = t.substring(idx + 1).trim();
            }
        }
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() > 2) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private String cleanJavaSnippet(String code) {
        if (code == null) return "";
        String cleaned = code.trim();
        
        // Remove markdown code block markers
        cleaned = cleaned.replaceAll("(?i)```java", "");
        cleaned = cleaned.replaceAll("```", "");
        
        // Only extract from first "public class" onwards, don't use "class " as fallback
        // This prevents the second replaceAll from overwriting "public class" and causing class name issues
        int idx = cleaned.toLowerCase(Locale.ROOT).indexOf("public class");
        if (idx >= 0) {
            cleaned = cleaned.substring(idx);
        }

        // Don't drop imports (some solutions need them)
        return cleaned.trim();
    }

    private String cleanJavaTestSnippet(String code, String expectedTestClassName) {
        if (code == null) return "";
        
        // Remove duplicate sections (common LLM issue)
        code = removeDuplicateSections(code);
        
        // For test code, we need to preserve imports, so don't use cleanJavaSnippet
        // which starts from "public class" and removes imports
        String cleaned = code.trim();
        
        // Remove markdown code block markers
        cleaned = cleaned.replaceAll("(?i)```java", "");
        cleaned = cleaned.replaceAll("```", "");
        
        // Find import statements and class declaration
        int idxImport = indexOfIgnoreCase(cleaned, "import ");
        int idxClass = indexOfIgnoreCase(cleaned, "public class ");
        
        // If we have imports, start from the first import
        // Otherwise, start from the class declaration
        int start = 0;
        if (idxImport >= 0 && (idxClass < 0 || idxImport < idxClass)) {
            start = idxImport;
        } else if (idxClass >= 0) {
            start = idxClass;
        }
        
        if (start > 0) {
            cleaned = cleaned.substring(start);
        }
        
        // Ensure we have the required JUnit imports if they're missing
        if (!cleaned.contains("import org.junit.jupiter.api.Test")) {
            // Add imports at the beginning
            String imports = "import org.junit.jupiter.api.Test;\n"
                + "import static org.junit.jupiter.api.Assertions.*;\n\n";
            
            // Find where the class declaration starts
            int classIdx = indexOfIgnoreCase(cleaned, "public class ");
            if (classIdx >= 0) {
                cleaned = cleaned.substring(0, classIdx) + imports + cleaned.substring(classIdx);
            } else {
                // If no class found, prepend imports
                cleaned = imports + cleaned;
            }
        } else if (!cleaned.contains("import static org.junit.jupiter.api.Assertions")) {
            // Add static import if missing
            int testImportIdx = cleaned.indexOf("import org.junit.jupiter.api.Test");
            if (testImportIdx >= 0) {
                int nextLineIdx = cleaned.indexOf("\n", testImportIdx);
                if (nextLineIdx >= 0) {
                    cleaned = cleaned.substring(0, nextLineIdx + 1) 
                        + "import static org.junit.jupiter.api.Assertions.*;\n" 
                        + cleaned.substring(nextLineIdx + 1);
                }
            }
        }

        // Extract the actual class name being tested (before "Test")
        String className = expectedTestClassName != null && expectedTestClassName.endsWith("Test")
            ? expectedTestClassName.substring(0, expectedTestClassName.length() - 4)
            : null;

        // Fix test class name
        if (expectedTestClassName != null && !expectedTestClassName.isBlank()) {
            cleaned = cleaned.replaceAll("class\\s+\\w+Test", "class " + expectedTestClassName);
            cleaned = cleaned.replaceAll("class\\s+\\w+Test\\s*\\{", "class " + expectedTestClassName + " {");
        }

        // Fix incorrect class names in test assertions (e.g., "Crire" -> correct className)
        if (className != null && !className.isEmpty()) {
            // Remove common incorrect class names
            cleaned = cleaned.replaceAll("\\bCrire\\b", className);
            cleaned = cleaned.replaceAll("\\bCrireFonctionQui\\b", className);
            cleaned = cleaned.replaceAll("\\bCrirereverse\\b", className + ".reverse");
            // Fix any class name that doesn't match the expected pattern
            cleaned = fixIncorrectClassNames(cleaned, className);
        }

        // Remove any remaining duplicate test methods
        cleaned = removeDuplicateTestMethods(cleaned);

        return cleaned.trim();
    }
    
    private String removeDuplicateSections(String code) {
        if (code == null || code.length() < 100) return code;
        
        // Split by common delimiters that indicate duplicate sections
        String[] sections = code.split("===|---|```");
        
        // If we have multiple sections with similar content, keep only the first complete one
        if (sections.length > 3) {
            // Look for the first section that contains a complete test class
            for (int i = 0; i < sections.length; i++) {
                String section = sections[i];
                if (section.contains("public class") && section.contains("@Test") 
                    && section.contains("assertEquals")) {
                    return section;
                }
            }
        }
        
        // Remove repeated patterns (e.g., same test method appearing multiple times)
        return removeRepeatedPatterns(code);
    }
    
    private String removeRepeatedPatterns(String code) {
        if (code == null) return code;
        
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        Set<String> seenMethods = new HashSet<>();
        boolean inMethod = false;
        StringBuilder currentMethod = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Detect start of test method
            if (trimmed.startsWith("@Test") || (trimmed.startsWith("void test") && trimmed.contains("()"))) {
                if (inMethod && currentMethod.length() > 0) {
                    String methodKey = currentMethod.toString().trim();
                    if (!seenMethods.contains(methodKey)) {
                        result.append(currentMethod);
                        seenMethods.add(methodKey);
                    }
                    currentMethod.setLength(0);
                }
                inMethod = true;
                currentMethod.append(line).append("\n");
            } else if (inMethod) {
                currentMethod.append(line).append("\n");
                // Detect end of method
                if (trimmed.equals("}") && currentMethod.toString().contains("{")) {
                    String methodKey = currentMethod.toString().trim();
                    if (!seenMethods.contains(methodKey)) {
                        result.append(currentMethod);
                        seenMethods.add(methodKey);
                    }
                    currentMethod.setLength(0);
                    inMethod = false;
                }
            } else {
                result.append(line).append("\n");
            }
        }
        
        // Add last method if any
        if (currentMethod.length() > 0) {
            String methodKey = currentMethod.toString().trim();
            if (!seenMethods.contains(methodKey)) {
                result.append(currentMethod);
            }
        }
        
        return result.toString();
    }
    
    private String fixIncorrectClassNames(String code, String correctClassName) {
        if (code == null || correctClassName == null) return code;
        
        // Find all class references in assertions (e.g., "ClassName.methodName(")
        Pattern pattern = Pattern.compile("(\\w+)\\.(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(code);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String foundClassName = matcher.group(1);
            String methodName = matcher.group(2);
            
            // If the class name looks incorrect (too short, doesn't match pattern, etc.)
            if (isIncorrectClassName(foundClassName, correctClassName)) {
                matcher.appendReplacement(result, correctClassName + "." + methodName + "(");
            } else {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private boolean isIncorrectClassName(String foundName, String correctName) {
        if (foundName == null || foundName.isEmpty()) return true;
        if (foundName.equals(correctName)) return false;
        
        // Common incorrect patterns
        if (foundName.length() < 4) return true; // Too short (e.g., "Crire")
        if (foundName.toLowerCase().contains("crire")) return true;
        if (foundName.toLowerCase().startsWith("cr")) return true; // Common typo pattern
        
        return false;
    }
    
    private String removeDuplicateTestMethods(String code) {
        if (code == null) return code;
        
        String[] lines = code.split("\n");
        Map<String, Integer> methodSignatures = new LinkedHashMap<>();
        StringBuilder result = new StringBuilder();
        StringBuilder currentMethod = new StringBuilder();
        String currentSignature = null;
        boolean inMethod = false;
        int braceCount = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Detect test method start
            if (trimmed.startsWith("@Test") || (trimmed.startsWith("void test") && trimmed.contains("()"))) {
                if (inMethod && currentSignature != null) {
                    // Save previous method
                    methodSignatures.put(currentSignature, methodSignatures.getOrDefault(currentSignature, 0) + 1);
                    if (methodSignatures.get(currentSignature) == 1) {
                        result.append(currentMethod);
                    }
                    currentMethod.setLength(0);
                }
                inMethod = true;
                currentSignature = trimmed;
                currentMethod.append(line).append("\n");
                braceCount = 0;
            } else if (inMethod) {
                currentMethod.append(line).append("\n");
                // Count braces to detect method end
                for (char c : line.toCharArray()) {
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                }
                if (braceCount == 0 && currentMethod.toString().contains("}")) {
                    // Method ended
                    if (currentSignature != null) {
                        methodSignatures.put(currentSignature, methodSignatures.getOrDefault(currentSignature, 0) + 1);
                        if (methodSignatures.get(currentSignature) == 1) {
                            result.append(currentMethod);
                        }
                    }
                    currentMethod.setLength(0);
                    currentSignature = null;
                    inMethod = false;
                }
            } else {
                result.append(line).append("\n");
            }
        }
        
        // Add last method if any
        if (currentMethod.length() > 0 && currentSignature != null) {
            methodSignatures.put(currentSignature, methodSignatures.getOrDefault(currentSignature, 0) + 1);
            if (methodSignatures.get(currentSignature) == 1) {
                result.append(currentMethod);
            }
        }
        
        return result.toString();
    }
    
    private int indexOfIgnoreCase(String text, String token) {
        if (text == null || token == null) return -1;
        return text.toLowerCase(Locale.ROOT).indexOf(token.toLowerCase(Locale.ROOT));
    }

    private String extractClassName(String code) {
        if (code == null || code.isEmpty()) {
            return "Solution";
        }

        int idx = code.indexOf("public class ");
        if (idx >= 0) {
            return extractClassNameFromIndex(code, idx + "public class ".length());
        }

        idx = code.indexOf("class ");
        if (idx >= 0) {
            return extractClassNameFromIndex(code, idx + "class ".length());
        }

        return "Solution";
    }
    
    private String extractClassNameFromIndex(String code, int start) {
        int end = start;
        while (end < code.length() &&
               (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
            end++;
        }
        if (end > start) {
            return code.substring(start, end);
        }
        return "Solution";
    }

    private String ensureClassName(String code, String expectedClassName) {
        if (code == null || code.isEmpty() || expectedClassName == null) {
            return code;
        }

        String current = extractClassName(code);
        if (current.equals(expectedClassName)) {
            return code;
        }

        // Use more precise regex to only replace the class declaration line
        // This prevents accidental replacements in other parts of the code
        // Match: whitespace + public + whitespace + class + whitespace + classname + whitespace + {
        code = code.replaceFirst("(?m)^\\s*public\\s+class\\s+\\w+\\s*\\{",
                                 "public class " + expectedClassName + " {");
        
        // Also handle non-public class declarations
        code = code.replaceFirst("(?m)^\\s*class\\s+\\w+\\s*\\{",
                                 "class " + expectedClassName + " {");

        return code;
    }

    private String removeMainMethod(String code) {
        if (code == null || code.isEmpty()) return code;

        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inMain = false;
        int braceDepth = 0;
        boolean foundSignature = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.contains("public static void main") && trimmed.contains("String[]")) {
                inMain = true;
                foundSignature = true;
                braceDepth = trimmed.contains("{") ? 1 : 0;
                continue;
            }

            if (inMain) {
                braceDepth = updateBraceDepth(line, braceDepth);
                if (braceDepth <= 0 && foundSignature) {
                    inMain = false;
                    foundSignature = false;
                    braceDepth = 0;
                }
                continue;
            }

            result.append(line).append("\n");
        }

        return result.toString().trim();
    }
    
    private int updateBraceDepth(String line, int braceDepth) {
        for (char c : line.toCharArray()) {
            if (c == '{') braceDepth++;
            if (c == '}') braceDepth--;
        }
        return braceDepth;
    }
}
