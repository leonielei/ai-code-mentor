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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    
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
        requestBody.put("stop", new String[]{"```", "\n\n\n\n", "//", "/*"});
        return requestBody;
    }
    
    private String processResponse(String response) {
        if (response == null) {
            logger.warn("LLM returned null response");
            return "";
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode contentNode = jsonNode.get("content");
            String content = (contentNode != null) ? contentNode.asText() : "";
            if (content.isEmpty()) {
                logger.warn("LLM returned empty content");
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
        String examples = generateExamplesFromTask(coreTask, className);

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
        String code = callLlamaAPI(prompt, 800);
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
    
    private String retrySolutionGeneration(String task, String className) {
        logger.info("Retrying solution generation with enhanced prompt...");
        String enhancedPrompt = buildEnhancedSolutionPrompt(task, className);
        String code = callLlamaAPI(enhancedPrompt, 1000);
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
        if (solution == null || solution.isBlank() || solution.contains("TODO")) {
            logger.warn("Solution is invalid, generating tests from task description");
            return generateTestsFromTask(task, className);
        }
        
        String expectedTestClassName = className + "Test";
        String methodInfo = extractMethodInfoForTests(solution);
        String prompt = buildTestPrompt(className, methodInfo, expectedTestClassName, task);
        
        String tests = callLlamaAPI(prompt, 500);
        tests = cleanJavaTestSnippet(tests, expectedTestClassName);

        if (!tests.contains("@Test") || tests.contains("TODO") || !hasRealTestAssertions(tests)) {
            logger.warn("Tests are invalid, generating from task");
            tests = generateTestsFromTask(task, className);
        }
        return tests.trim();
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
        
        return buildIntelligentTests(className, methodName, returnType, paramType);
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
            }
        } else if ("String".equals(paramType)) {
            tests.append("        String input = \"hello\";\n");
            if ("String".equals(returnType)) {
                tests.append("        assertEquals(\"olleh\", ").append(className).append(".").append(methodName).append("(input));\n");
            } else if ("boolean".equals(returnType)) {
                tests.append("        assertTrue(").append(className).append(".").append(methodName).append("(input));\n");
            }
        }
        tests.append("    }\n\n");
        
        tests.append("    @Test\n");
        tests.append("    void testCasLimite() {\n");
        if ("int[]".equals(paramType)) {
            tests.append("        int[] empty = {};\n");
            tests.append("        assertEquals(0, ").append(className).append(".").append(methodName).append("(empty));\n");
            tests.append("        assertEquals(0, ").append(className).append(".").append(methodName).append("(null));\n");
        } else if ("String".equals(paramType)) {
            tests.append("        assertEquals(\"\", ").append(className).append(".").append(methodName).append("(\"\"));\n");
            if ("String".equals(returnType)) {
                tests.append("        assertEquals(null, ").append(className).append(".").append(methodName).append("(null));\n");
            }
        }
        tests.append("    }\n\n");
        
        tests.append("    @Test\n");
        tests.append("    void testCasComplexe() {\n");
        if ("int[]".equals(paramType)) {
            tests.append("        int[] array = {10, 20, 30, 40, 50};\n");
            if ("int".equals(returnType)) {
                tests.append("        assertEquals(150, ").append(className).append(".").append(methodName).append("(array));\n");
            }
        } else if ("String".equals(paramType)) {
            tests.append("        String input = \"hello world\";\n");
            if ("String".equals(returnType)) {
                tests.append("        assertEquals(\"dlrow olleh\", ").append(className).append(".").append(methodName).append("(input));\n");
            }
        }
        tests.append("    }\n");
        tests.append("}\n");
        
        return tests.toString();
    }
    
    private String buildTestPrompt(String className, String codeToTest, String expectedTestClassName, String task) {
        return "URGENT: Génère des tests JUnit 5 COMPLETS avec des ASSERTIONS RÉELLES. PAS de TODO.\n\n"
            + "=== EXERCICE ===\n" + (task != null ? task : "") + "\n\n"
            + "=== CLASSE À TESTER ===\n"
            + "Classe : " + className + "\n"
            + "Méthode(s) :\n" + codeToTest + "\n\n"
            + "=== EXIGENCES ABSOLUES ===\n"
            + "1. Nom de classe de test EXACT : " + expectedTestClassName + "\n"
            + "2. Imports OBLIGATOIRES :\n"
            + "   import org.junit.jupiter.api.Test;\n"
            + "   import static org.junit.jupiter.api.Assertions.*;\n"
            + "3. Minimum 3 tests avec ASSERTIONS RÉELLES (PAS de TODO) :\n"
            + "   - testCasBasique() : valeurs normales avec assertEquals concret\n"
            + "   - testCasLimite() : null, tableaux vides, chaînes vides avec assertions\n"
            + "   - testCasComplexe() : cas multiples avec assertions\n"
            + "4. Chaque test DOIT appeler " + className + ".méthode(...) et vérifier avec assertEquals/assertTrue\n"
            + "5. INTERDICTION ABSOLUE : PAS de TODO, PAS de code vide\n"
            + "6. PAS de markdown, UNIQUEMENT du code de test fonctionnel\n\n"
            + "=== EXEMPLE CONCRET ===\n"
            + "Si méthode = sum(int[] array) :\n"
            + "@Test\n"
            + "void testCasBasique() {\n"
            + "    int[] array = {1, 2, 3};\n"
            + "    assertEquals(6, " + className + ".sum(array));\n"
            + "}\n\n"
            + "@Test\n"
            + "void testCasLimite() {\n"
            + "    assertEquals(0, " + className + ".sum(new int[]{}));\n"
            + "    assertEquals(0, " + className + ".sum(null));\n"
            + "}\n\n"
            + "=== FORMAT ===\n"
            + "Commence par 'import org.junit.jupiter.api.Test;'\n"
            + "Génère des tests COMPLETS avec assertions RÉELLES pour : " + (task != null ? task : className) + "\n\n"
            + "Code des tests :";
    }

    // ============================================================
    // 7) Hint generation
    // ============================================================
    public String generateHint(String testName, String testCode,
                               String studentCode, String errorMessage) {
        return generateHint(testName, testCode, studentCode, errorMessage, null);
    }
    
    public String generateHint(String testName, String testCode,
                               String studentCode, String errorMessage, String problemStatement) {
        logger.info("Generating hint for failed test: {}", testName);

        String testExpectation = extractTestExpectation(testCode);
        String exerciseContext = buildExerciseContext(problemStatement);
        String logicIssue = analyzeStudentCode(studentCode);
        String prompt = buildHintPrompt(testName, testCode, studentCode, 
                                       errorMessage, testExpectation, 
                                       exerciseContext, logicIssue);

        String hint = callLlamaAPI(prompt, 300);
        hint = postProcessHint(hint, errorMessage);
        return hint.trim();
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
    
    private String buildHintPrompt(String testName, String testCode, String studentCode,
                                   String errorMessage, String testExpectation,
                                   String exerciseContext, String logicIssue) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un professeur de programmation Java. Analyse le code de l'étudiant et donne un indice LOGIQUE et ACTIONNABLE.\n\n");
        prompt.append("=== EXERCICE ===\n").append(exerciseContext).append("\n\n");
        prompt.append("=== CODE DE L'ÉTUDIANT ===\n").append(studentCode != null ? studentCode : "").append("\n\n");
        prompt.append("=== PROBLÈME ===\n");
        prompt.append("Test: ").append(testName).append("\n");
        prompt.append("Erreur: ").append(errorMessage != null && !errorMessage.isEmpty() ? errorMessage : "Le test échoue").append("\n");
        if (!testExpectation.isEmpty()) {
            prompt.append("Attendu: ").append(testExpectation).append("\n");
        }
        if (!logicIssue.isEmpty()) {
            prompt.append("Problème détecté: ").append(logicIssue).append("\n");
        }
        prompt.append("\n");
        if (testCode != null && !testCode.isEmpty()) {
            prompt.append("=== TEST (pour comprendre ce qui est attendu) ===\n");
            prompt.append(testCode.substring(0, Math.min(testCode.length(), 600))).append("\n\n");
        }
        prompt.append(buildHintInstructions());
        return prompt.toString();
    }
    
    private String buildHintInstructions() {
        return "=== INSTRUCTIONS STRICTES ===\n"
            + "1. Analyse la LOGIQUE du code: que fait-il actuellement vs ce qu'il devrait faire?\n"
            + "2. Identifie le PROBLÈME SPÉCIFIQUE (pas juste 'ça ne marche pas')\n"
            + "3. Donne un indice CONCRET sur COMMENT corriger (quelle approche, quelle structure)\n"
            + "4. Sois PRÉCIS: mentionne les variables, les boucles, les conditions si pertinent\n"
            + "5. INTERDICTION ABSOLUE: Ne JAMAIS inclure de code Java, ni de snippets, ni d'exemples de code\n"
            + "6. Utilise uniquement des descriptions textuelles et des explications conceptuelles\n"
            + "7. Si tu mentionnes une méthode ou une syntaxe, décris-la avec des mots, pas avec du code\n\n"
            + "=== FORMAT ===\n"
            + "Réponds en 2-3 phrases maximum, directement et sans préambule.\n"
            + "Exemple CORRECT: 'Tu retournes une chaîne vide. Pour inverser une chaîne, parcours-la de la fin (index length()-1) vers le début (index 0) et construis la nouvelle chaîne caractère par caractère.'\n"
            + "Exemple INCORRECT (à éviter): 'Utilise: return new StringBuilder(str).reverse().toString();'\n\n"
            + "=== RAPPEL FINAL ===\n"
            + "AUCUN CODE JAVA. Seulement des explications textuelles et des conseils conceptuels.\n\n"
            + "Indice:";
    }
    
    private String postProcessHint(String hint, String errorMessage) {
        if (hint != null && !hint.trim().isEmpty()) {
            hint = removeCodeFromHint(hint);
        }
        
        if (hint == null || hint.trim().length() < 20) {
            return getDefaultHint(errorMessage);
        }
        return hint;
    }
    
    private String getDefaultHint(String errorMessage) {
        if (errorMessage == null) {
            return HintStrategy.DEFAULT.getHint();
        }
        return HintStrategy.findStrategy(errorMessage).getHint();
    }
    
    private enum HintStrategy {
        NULL_POINTER(msg -> msg.contains("NullPointerException"),
            "Il semble y avoir une NullPointerException. Vérifie que tous les objets sont initialisés avant d'être utilisés, et que les méthodes ne retournent pas null."),
        ARRAY_INDEX(msg -> msg.contains("ArrayIndexOutOfBoundsException"),
            "Il y a une erreur d'index de tableau. Vérifie que les indices utilisés sont valides (entre 0 et la longueur du tableau - 1)."),
        STRING_INDEX(msg -> msg.contains("StringIndexOutOfBoundsException"),
            "Il y a une erreur d'index de chaîne. Vérifie que les indices utilisés pour accéder aux caractères sont valides."),
        DEFAULT(msg -> true,
            "Vérifie attentivement ta logique. Assure-toi que :\n1. Toutes les variables sont correctement initialisées\n2. Les conditions et boucles sont correctement écrites\n3. Les valeurs retournées correspondent à ce qui est attendu par le test");
        
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
    
    private String removeCodeFromHint(String hint) {
        if (hint == null || hint.isEmpty()) {
            return hint;
        }
        
        String cleaned = removeCodeBlocks(hint);
        cleaned = filterCodeLines(cleaned);
        cleaned = removeCodePatterns(cleaned);
        cleaned = cleanupWhitespace(cleaned);
        
        if (cleaned.length() < 30 && hint.length() > 50) {
            return fallbackCleanHint(hint);
        }
        
        return cleaned.trim();
    }
    
    private String removeCodeBlocks(String hint) {
        String cleaned = hint.replaceAll("```[\\s\\S]*?```", "");
        cleaned = cleaned.replaceAll("`([^`]{20,})`", "$1");
        return cleaned;
    }
    
    private String filterCodeLines(String cleaned) {
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!isCodeLine(trimmed)) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }
    
    private boolean isCodeLine(String trimmed) {
        return (trimmed.contains("public ") && trimmed.contains("("))
            || (trimmed.contains("private ") && trimmed.contains("("))
            || (trimmed.contains("return ") && trimmed.contains(";"))
            || (trimmed.contains("=") && trimmed.contains(";") && trimmed.length() > 30)
            || trimmed.startsWith("import ")
            || trimmed.startsWith("package ")
            || (trimmed.matches(".*\\{[^}]*\\}.*") && trimmed.length() > 50);
    }
    
    private String removeCodePatterns(String cleaned) {
        cleaned = cleaned.replaceAll("public\\s+\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("return\\s+[^;]+;", "");
        return cleaned;
    }
    
    private String cleanupWhitespace(String cleaned) {
        cleaned = cleaned.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");
        return cleaned.trim();
    }
    
    private String fallbackCleanHint(String hint) {
        String cleaned = hint.replaceAll("```[\\s\\S]*?```", "");
        cleaned = cleaned.replaceAll("`([^`]+)`", "$1");
        return cleaned;
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

    private String generateExamplesFromTask(String task, String className) {
        if (task == null || task.isBlank()) {
            return "Voir l'énoncé pour les exemples d'entrées / sorties.";
        }

        String prompt = "Tu es un expert en pédagogie. Génère 3 exemples CONCRETS et VARIÉS pour cet exercice.\n\n"
            + "=== EXERCICE ===\n" + task + "\n\n"
            + "=== EXIGENCES ===\n"
            + "1. Génère EXACTEMENT 3 exemples\n"
            + "2. Format OBLIGATOIRE : 'Entrée : [valeur] → Sortie : [résultat]'\n"
            + "3. Chaque exemple sur une ligne séparée\n"
            + "4. Exemples VARIÉS : cas simple, cas avec valeurs multiples, cas limite\n"
            + "5. Utilise des valeurs CONCRÈTES et RÉALISTES\n"
            + "6. PAS de code, PAS de markdown, UNIQUEMENT le format Entrée → Sortie\n\n"
            + "=== EXEMPLE DE FORMAT ===\n"
            + "Si l'exercice demande de calculer la somme d'un tableau :\n"
            + "Entrée : [1, 2, 3] → Sortie : 6\n"
            + "Entrée : [10, 20, 30, 40] → Sortie : 100\n"
            + "Entrée : [] → Sortie : 0\n\n"
            + "=== RÉPONSE ===\n"
            + "Génère 3 exemples dans le format exact ci-dessus, une ligne par exemple :";
    
        String examples = callLlamaAPI(prompt, 200);
        examples = sanitize(examples);
        examples = formatExamples(examples);

        if (examples == null || examples.isBlank() || !examples.contains("→")) {
            return createDefaultExamples();
        }
    
        return examples.trim();
    }
    
    private String formatExamples(String examples) {
        if (examples == null || examples.isBlank()) {
            return examples;
        }
        
        String[] lines = examples.split("\n");
        StringBuilder formatted = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            
            if (trimmed.contains("→") || trimmed.contains("->") || trimmed.contains(":")) {
                trimmed = trimmed.replace("->", "→").replace(":", " :");
                if (!trimmed.startsWith("Entrée")) {
                    trimmed = "Entrée : " + trimmed;
                }
                formatted.append(trimmed).append("\n");
            } else if (trimmed.matches(".*\\[.*\\].*")) {
                formatted.append("Entrée : ").append(trimmed).append("\n");
            }
        }
        
        String result = formatted.toString().trim();
        if (result.isEmpty()) {
            return examples;
        }
        
        String[] resultLines = result.split("\n");
        if (resultLines.length > 3) {
            StringBuilder limited = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                limited.append(resultLines[i]).append("\n");
            }
            return limited.toString().trim();
        }
        
        return result;
    }
    
    private String createDefaultExamples() {
        return "Exemples d'utilisation :\n"
            + "- Choisissez quelques entrées adaptées à la consigne (cas simple, cas limite) et indiquez la sortie attendue.\n"
            + "- Par exemple : Entrée : valeur simple → Sortie : résultat conforme à la description.";
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
    
    private String generateClassNameFromWords(String task) {
        String clean = task.trim();
        String[] words = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < Math.min(words.length, 4); i++) {
            String w = words[i].replaceAll("[^a-zA-Z0-9]", "");
            if (w.length() <= 2) continue;
            String lower = w.toLowerCase(Locale.ROOT);
            if (isStopWord(lower)) {
                continue;
            }
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase());
        }

        String result = sb.toString();
        if (result.isEmpty() || !Character.isLetter(result.charAt(0))) {
            result = "Solution";
        }
        return result;
    }
    
    private boolean isStopWord(String word) {
        return word.equals("les") || word.equals("des") || word.equals("une")
            || word.equals("pour") || word.equals("avec") || word.equals("dans")
            || word.equals("où") || word.equals("que") || word.equals("the")
            || word.equals("and") || word.equals("students") || word.equals("étudiants");
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
        cleaned = cleaned.replaceAll("(?i)```java", "");
        cleaned = cleaned.replaceAll("```", "");
        cleaned = cleaned.replaceAll("(?i)^.*?public\\s+class", "public class");
        cleaned = cleaned.replaceAll("(?i)^.*?class\\s+", "class ");

        StringBuilder sb = new StringBuilder();
        String[] lines = cleaned.split("\n");
        boolean codeStarted = false;
        for (String line : lines) {
            String trim = line.trim();
            if (trim.startsWith("package ")) {
                continue;
            }
            if (trim.startsWith("import ") || trim.startsWith("public ") || trim.startsWith("class ")) {
                codeStarted = true;
            }
            if (codeStarted || !trim.isEmpty()) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String cleanJavaTestSnippet(String code, String expectedTestClassName) {
        if (code == null) return "";
        String cleaned = cleanJavaSnippet(code);

        int idxImport = indexOfIgnoreCase(cleaned, "import ");
        int idxClass = indexOfIgnoreCase(cleaned, "public class ");

        int start = findStartIndex(idxImport, idxClass);
        if (start > 0) {
            cleaned = cleaned.substring(start);
        }

        if (expectedTestClassName != null && !expectedTestClassName.isBlank()) {
            cleaned = cleaned.replaceAll("class\\s+\\w+Test", "class " + expectedTestClassName);
        }

        return cleaned.trim();
    }
    
    private int findStartIndex(int idxImport, int idxClass) {
        if (idxImport >= 0 && idxClass >= 0) {
            return Math.min(idxImport, idxClass);
        } else if (idxImport >= 0) {
            return idxImport;
        } else if (idxClass >= 0) {
            return idxClass;
        }
        return -1;
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

        code = code.replaceAll("public class " + current, "public class " + expectedClassName);
        code = code.replaceAll("class " + current + " ", "class " + expectedClassName + " ");
        code = code.replaceAll("class " + current + "\\{", "class " + expectedClassName + "{");
        code = code.replaceAll("class " + current + "\n", "class " + expectedClassName + "\n");

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
