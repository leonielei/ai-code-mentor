package com.aicodementor.service;

import com.aicodementor.dto.ExerciseGenerationRequest;
import com.aicodementor.dto.ExerciseGenerationResponse;
import com.aicodementor.entity.Exercise;
import com.aicodementor.entity.KnowledgeBase;
import com.aicodementor.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.llamacpp.base-url:http://localhost:11435}")
    private String llamacppBaseUrl;
    
    @Autowired(required = false)
    private SemanticSearchService semanticSearchService;
    
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    
    @Autowired(required = false)
    private KnowledgeBaseRepository knowledgeBaseRepository;

    // ============================================================
    // 1) 通用：调用 llama.cpp /completion
    // ============================================================
    private String callLlamaAPI(String prompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("n_predict", maxTokens);
            requestBody.put("temperature", 0.2);  // 降低temperature提高准确性
            requestBody.put("top_p", 0.95);  // 提高top_p
            requestBody.put("top_k", 40);
            requestBody.put("repeat_penalty", 1.15);  // 提高重复惩罚
            requestBody.put("stop", new String[]{"```", "\n\n\n\n", "//", "/*"});

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                    llamacppBaseUrl + "/completion",
                    request,
                    String.class
            );

            if (response == null) {
                logger.warn("LLM returned null response");
                return "";
            }

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
            logger.error("Error calling llama.cpp API", e);
            return "";
        }
    }

    // ============================================================
    // 2) 主入口：从自然语言生成完整练习
    // ============================================================
    public ExerciseGenerationResponse generateExercise(ExerciseGenerationRequest request) {
        logger.info("Generating exercise from description: {}",
                request.naturalLanguageDescription());

        String task = request.naturalLanguageDescription();
        String language = request.programmingLanguage() == null
                ? "Java"
                : request.programmingLanguage();
        String difficulty = request.targetDifficulty() == null
                ? "L1"
                : request.targetDifficulty();

        String coreTask = extractCoreTask(task);
        String title = buildTitleFromTask(coreTask, language);

        String starterCode;
        String unitTests;
        String solution;
        String description;
        String concepts;
        String examples;

        if (!"java".equalsIgnoreCase(language)) {
            logger.error("Unsupported language: {}", language);
            description = "Langage non supporté. Pour l'instant, seul Java est pris en charge.";
            starterCode = "// Unsupported language";
            unitTests = "// Unsupported language";
            solution = "// Unsupported language";
            concepts = "Langage non supporté";
            examples = "N/A";
        } else {
            // 类名根据题目生成
            String className = generateClassNameFromTask(coreTask);
            logger.info("Generated class name: {}", className);

            // Description / Sujet / Exemples 不再用 LLM
            description = buildDescriptionFromTask(coreTask);

            // Solution 用 LLM - 使用简单直接的提示词，不使用RAG
            solution = generateSolutionCode(coreTask, className);
            logger.info("Generated solution length = {}", solution.length());

            // Starter code 从 solution 提取
            starterCode = generateStarterCodeFromSolution(solution, className);
            logger.info("Generated starter length = {}", starterCode.length());

            // Tests JUnit - 使用简单直接的提示词，不使用RAG
            unitTests = generateJUnitTests(coreTask, solution, className);
            logger.info("Generated tests length = {}", unitTests.length());

            // Concepts / Topic
            concepts = detectConceptsFromTask(coreTask, difficulty);

            // Exemples d'utilisation - 使用简单直接的方法
            examples = generateExamplesFromTask(coreTask, className);
        }

        return new ExerciseGenerationResponse(
                title,
                description,
                difficulty,
                concepts,       // Sujet / Concepts
                starterCode,
                unitTests,
                solution,
                examples        // Exemples d'utilisation
        );
    }

    // ============================================================
    // 3) 从题目中抽“核心任务” + 标题 + 描述（不用 LLM）
    // ============================================================
    private String extractCoreTask(String task) {
        if (task == null) {
            return "";
        }
        String core = task.trim();

        core = core.replaceFirst("(?i)^exercice\\s*:?\\s*", "");
        core = core.replaceFirst("(?i)^cr[ée]er? un exercice o[uù] les [ée]tudiants doivent\\s*", "");
        core = core.replaceFirst("(?i)^cr[ée]er? un exercice o[uù] les [ée]tudiants\\s*", "");
        core = core.replaceFirst("(?i)^cr[ée]er? un exercice\\s*", "");
        core = core.replaceFirst("(?i)^write an exercise where students (must|have to|need to)\\s*", "");

        core = core.trim();
        return core.isEmpty() ? task.trim() : core;
    }

    private String buildTitleFromTask(String coreTask, String language) {
        if (coreTask == null || coreTask.isBlank()) {
            return "Exercice de programmation (" + language + ")";
        }

        String firstSentence = coreTask.split("[.?!]")[0].trim();
        if (firstSentence.length() > 70) {
            firstSentence = firstSentence.substring(0, 70).trim();
        }

        firstSentence = firstSentence.replaceFirst("(?i)^impl[ée]menter\\s+une?\\s+fonction\\s+qui\\s*", "");
        firstSentence = firstSentence.replaceFirst("(?i)^[ée]crire\\s+une?\\s+fonction\\s+qui\\s*", "");
        firstSentence = firstSentence.replaceFirst("(?i)^[ée]crire\\s+un\\s+programme\\s+qui\\s*", "");
        firstSentence = firstSentence.trim();

        if (!firstSentence.isEmpty()
                && Character.isLowerCase(firstSentence.charAt(0))) {
            firstSentence = Character.toUpperCase(firstSentence.charAt(0))
                    + firstSentence.substring(1);
        }

        return "Exercice : " + firstSentence + " (" + language + ")";
    }

    private String buildDescriptionFromTask(String coreTask) {
        if (coreTask == null || coreTask.isBlank()) {
            return "Dans cet exercice, vous devez écrire une fonction en Java qui respecte la consigne donnée. "
                   + "Votre code doit être clair, correct et gérer les cas simples ainsi que quelques cas limites.";
        }

        String sentence = coreTask.trim();
        if (!sentence.endsWith(".")) {
            sentence += ".";
        }

        return "Dans cet exercice, vous devez " + sentence + "\n"
             + "Vous écrirez une ou plusieurs méthodes en Java respectant cette consigne.\n"
             + "Votre code doit être clair, correctement indenté et gérer les cas simples ainsi que quelques cas limites.";
    }

    // ============================================================
    // 4) Solution Java：通过 llama.cpp 生成 + 补大括号
    // ============================================================
    private String generateSolutionCode(String task, String className) {
        if (task == null || task.isBlank()) {
            task = "implémenter une fonction utilitaire en Java.";
        }

        // 增强提示词，更详细和结构化
        String prompt =
                "Tu es un expert en programmation Java. Génère du code Java de haute qualité pour l'exercice suivant.\n\n" +
                "=== EXERCICE ===\n" +
                task + "\n\n" +
                "=== CONTRAINTES STRICTES ===\n" +
                "1. Nom de classe EXACT : " + className + " (sans espaces, sans caractères spéciaux)\n" +
                "2. Méthodes : public static avec implémentation COMPLÈTE et CORRECTE\n" +
                "3. Structure : classe bien formée avec toutes les accolades fermées\n" +
                "4. Interdictions :\n" +
                "   - PAS de méthode main (sauf si explicitement demandé)\n" +
                "   - PAS de commentaires dans le code\n" +
                "   - PAS de TODO ou de code incomplet\n" +
                "   - PAS de markdown (pas de ```java)\n" +
                "   - PAS d'explications textuelles\n" +
                "5. Qualité :\n" +
                "   - Code compilable sans erreur\n" +
                "   - Gestion des cas limites (null, vide, valeurs extrêmes)\n" +
                "   - Logique correcte et efficace\n" +
                "   - Indentation propre (4 espaces)\n\n" +
                "=== FORMAT DE RÉPONSE ===\n" +
                "Réponds UNIQUEMENT avec le code Java brut, sans explications, sans markdown, sans préambule.\n" +
                "Commence directement par 'public class " + className + " {'\n\n" +
                "Code Java :";

        String code = callLlamaAPI(prompt, 800);
        code = cleanJavaSnippet(code);

        // 验证和修复
        if (!code.contains("class ")) {
            logger.warn("Solution has no 'class', using fallback.");
            code = "public class " + className + " {\n" +
                   "    public static void solve() {\n" +
                   "        // TODO: implémenter la logique ici\n" +
                   "    }\n" +
                   "}";
        }

        if (!needsMain(task)) {
            code = removeMainMethod(code);
        }

        code = ensureClassName(code, className);
        code = fixBraces(code);

        if (code.length() < 40) {
            logger.error("Solution too short, fallback minimal template.");
            code = "public class " + className + " {\n" +
                   "    public static void solve() {\n" +
                   "        // TODO: implémenter la logique ici\n" +
                   "    }\n" +
                   "}";
        }

        return code.trim();
    }
    
    /**
     * 使用 RAG 生成解决方案代码
     */
    private String generateSolutionCodeWithRAG(String task, String className, String augmentedContext) {
        if (task == null || task.isBlank()) {
            task = "implémenter une fonction utilitaire en Java.";
        }

        String prompt = augmentedContext + "\n\n" +
                "Consigne de l'exercice : " + task + "\n" +
                "Tu dois écrire le code Java COMPLET qui résout cet exercice.\n" +
                "Contraintes :\n" +
                "- Le nom de la classe DOIT être exactement : " + className + "\n" +
                "- Fournis une ou plusieurs méthodes 'public static' avec une implémentation complète.\n" +
                "- NE PAS écrire de méthode main, sauf si la consigne le demande explicitement.\n" +
                "- NE PAS écrire de commentaires, ni de TODO.\n" +
                "- Le code doit être compilable tel quel.\n" +
                "- Réponds UNIQUEMENT avec le code Java, sans explications, sans markdown.";

        String code = callLlamaAPI(prompt, 600);
        code = cleanJavaSnippet(code);

        if (!code.contains("class ")) {
            logger.warn("Solution has no 'class', using fallback.");
            code = "public class " + className + " {\n" +
                   "    public static void solve() {\n" +
                   "        // TODO: implémenter la logique ici\n" +
                   "    }\n" +
                   "}";
        }

        if (!needsMain(task)) {
            code = removeMainMethod(code);
        }

        code = ensureClassName(code, className);
        code = fixBraces(code);

        if (code.length() < 40) {
            logger.error("Solution too short, fallback minimal template.");
            code = "public class " + className + " {\n" +
                   "    public static void solve() {\n" +
                   "        // TODO: implémenter la logique ici\n" +
                   "    }\n" +
                   "}";
        }

        return code.trim();
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
    // 5) Starter code：从 solution 抽结构 + TODO
    // ============================================================
    private String generateStarterCodeFromSolution(String solution, String expectedClassName) {
        if (solution == null || solution.isBlank()) {
            return "public class " + expectedClassName + " {\n" +
                   "    public static void solve() {\n" +
                   "        // TODO: Implémentez votre solution ici\n" +
                   "    }\n" +
                   "}";
        }

        StringBuilder starter = new StringBuilder();
        String[] lines = solution.split("\n");
        boolean inClass = false;

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

            if ((trimmed.startsWith("public static") || trimmed.startsWith("private static")) &&
                trimmed.contains("(") && trimmed.contains(")") &&
                trimmed.endsWith("{")) {

                starter.append("    ").append(trimmed).append("\n");
                starter.append("        // TODO: Implémentez votre solution ici\n");

                if (trimmed.contains(" void ")) {
                    starter.append("    }\n");
                } else if (trimmed.contains(" int ")
                        || trimmed.contains(" long ")
                        || trimmed.contains(" double ")
                        || trimmed.contains(" float ")) {
                    starter.append("        return 0;\n    }\n");
                } else if (trimmed.contains(" boolean ")) {
                    starter.append("        return false;\n    }\n");
                } else if (trimmed.contains(" String ")) {
                    starter.append("        return \"\";\n    }\n");
                } else {
                    starter.append("        return null;\n    }\n");
                }
                continue;
            }
        }

        if (!inClass || !starter.toString().contains("TODO")) {
            return createStarterCodeFallback(solution, expectedClassName);
        }

        starter.append("}\n");
        return starter.toString().trim();
    }

    private String createStarterCodeFallback(String solution, String expectedClassName) {
        return "public class " + expectedClassName + " {\n" +
               "    public static void solve() {\n" +
               "        // TODO: Implémentez votre solution ici\n" +
               "    }\n" +
               "}";
    }

    // ============================================================
    // 6) JUnit 5 测试生成
    // ============================================================
    private String generateJUnitTests(String task, String solution, String className) {
        String snippet = (solution == null) ? "" : solution;
        if (snippet.length() > 600) {
            snippet = snippet.substring(0, 600) + "...";
        }

        String expectedTestClassName = className + "Test";

        // 提取方法签名而不是完整代码
        String methodSignatures = extractMethodSignatures(solution);
        String codeToTest = methodSignatures.isEmpty() ? snippet : methodSignatures;
        if (codeToTest.length() > 300) {
            codeToTest = codeToTest.substring(0, 300);
        }
        
        String prompt =
                "Tu es un expert en tests unitaires Java. Génère des tests JUnit 5 complets et robustes.\n\n" +
                "=== CLASSE À TESTER ===\n" +
                "Classe : " + className + "\n" +
                "Méthodes disponibles :\n" + codeToTest + "\n\n" +
                "=== CONTRAINTES STRICTES ===\n" +
                "1. Nom de classe de test EXACT : " + expectedTestClassName + "\n" +
                "2. Imports requis :\n" +
                "   - import org.junit.jupiter.api.Test;\n" +
                "   - import static org.junit.jupiter.api.Assertions.*;\n" +
                "3. Tests requis (minimum 3) :\n" +
                "   - testBasicCase() : cas d'usage normal et typique\n" +
                "   - testEdgeCase() : cas limites (null, vide, valeurs extrêmes)\n" +
                "   - testComplexCase() : scénario plus complexe avec plusieurs cas\n" +
                "4. Qualité des tests :\n" +
                "   - Chaque test doit être indépendant et isolé\n" +
                "   - Utiliser assertEquals, assertTrue, assertFalse selon le besoin\n" +
                "   - Tester les cas limites et les erreurs potentielles\n" +
                "   - Noms de tests descriptifs et clairs\n" +
                "5. Interdictions :\n" +
                "   - PAS de markdown (pas de ```java)\n" +
                "   - PAS d'explications textuelles\n" +
                "   - PAS de commentaires dans les tests\n\n" +
                "=== FORMAT DE RÉPONSE ===\n" +
                "Réponds UNIQUEMENT avec le code Java brut des tests, sans explications, sans markdown.\n" +
                "Commence directement par 'import org.junit.jupiter.api.Test;'\n\n" +
                "Code des tests JUnit 5 :";

        String tests = callLlamaAPI(prompt, 400);
        tests = cleanJavaTestSnippet(tests, expectedTestClassName);

        if (!tests.contains("@Test")) {
            logger.warn("No @Test found, using default tests");
            tests = createDefaultTests(className);
        }
        return tests.trim();
    }
    
    


    private String createDefaultTests(String className) {
        String testClassName = className + "Test";
        return "import org.junit.jupiter.api.Test;\n" +
               "import static org.junit.jupiter.api.Assertions.*;\n\n" +
               "public class " + testClassName + " {\n" +
               "    @Test\n" +
               "    void testCasBasique() {\n" +
               "        // TODO: appeler " + className + ".methode(...) et vérifier le résultat\n" +
               "    }\n\n" +
               "    @Test\n" +
               "    void testCasLimite() {\n" +
               "        // TODO: cas limites (valeur nulle, vide, bornes...)\n" +
               "    }\n\n" +
               "    @Test\n" +
               "    void testCasComplexe() {\n" +
               "        // TODO: scénario plus complexe\n" +
               "    }\n" +
               "}";
    }

    // ============================================================
    // 7) Hint 生成
    // ============================================================
    public String generateHint(String testName, String testCode,
                               String studentCode, String errorMessage) {
        return generateHint(testName, testCode, studentCode, errorMessage, null);
    }
    
    public String generateHint(String testName, String testCode,
                               String studentCode, String errorMessage, String problemStatement) {
        logger.info("Generating hint for failed test: {}", testName);

        // Extract more context from test code
        String testExpectation = "";
        if (testCode != null && !testCode.isEmpty()) {
            // Try to extract what the test expects
            if (testCode.contains("assertEquals")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "assertEquals\\(([^,]+),\\s*([^)]+)\\)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(testCode);
                if (matcher.find()) {
                    testExpectation = "Le test attend que la méthode retourne " + matcher.group(1) + 
                                    " quand on appelle avec " + matcher.group(2) + ".";
                }
            }
        }
        
        // Use problem statement if available, otherwise use default
        String exerciseContext = (problemStatement != null && !problemStatement.trim().isEmpty()) 
            ? problemStatement 
            : "L'étudiant doit implémenter une fonction qui inverse une chaîne de caractères.";
        
        // Analyze student code logic more deeply
        String logicIssue = "";
        if (studentCode != null && !studentCode.isEmpty()) {
            // Check for common logic issues
            if (studentCode.contains("return \"\";") || studentCode.contains("return null;")) {
                logicIssue = "Le code retourne toujours une valeur vide au lieu d'implémenter la logique. ";
            }
            if (studentCode.contains("// TODO")) {
                logicIssue += "La logique n'est pas implémentée (présence de TODO). ";
            }
            // Check if method has any logic (beyond return statement)
            String methodBody = extractMethodBody(studentCode);
            if (methodBody != null && (methodBody.trim().isEmpty() || 
                methodBody.trim().equals("return \"\";") || 
                methodBody.trim().equals("return null;"))) {
                logicIssue += "Le corps de la méthode est vide ou ne fait que retourner une valeur par défaut. ";
            }
        }
        
        String prompt =
                "Tu es un professeur de programmation Java. Analyse le code de l'étudiant et donne un indice LOGIQUE et ACTIONNABLE.\n\n" +
                
                "=== EXERCICE ===\n" +
                exerciseContext + "\n\n" +
                
                "=== CODE DE L'ÉTUDIANT ===\n" +
                (studentCode != null ? studentCode : "") + "\n\n" +
                
                "=== PROBLÈME ===\n" +
                "Test: " + testName + "\n" +
                "Erreur: " + (errorMessage != null && !errorMessage.isEmpty() ? errorMessage : "Le test échoue") + "\n" +
                (testExpectation.isEmpty() ? "" : "Attendu: " + testExpectation + "\n") +
                (logicIssue.isEmpty() ? "" : "Problème détecté: " + logicIssue + "\n") +
                "\n" +
                
                (testCode != null && !testCode.isEmpty() ? "=== TEST (pour comprendre ce qui est attendu) ===\n" + 
                testCode.substring(0, Math.min(testCode.length(), 600)) + "\n\n" : "") +
                
                "=== INSTRUCTIONS STRICTES ===\n" +
                "1. Analyse la LOGIQUE du code: que fait-il actuellement vs ce qu'il devrait faire?\n" +
                "2. Identifie le PROBLÈME SPÉCIFIQUE (pas juste 'ça ne marche pas')\n" +
                "3. Donne un indice CONCRET sur COMMENT corriger (quelle approche, quelle structure)\n" +
                "4. Sois PRÉCIS: mentionne les variables, les boucles, les conditions si pertinent\n" +
                "5. INTERDICTION ABSOLUE: Ne JAMAIS inclure de code Java, ni de snippets, ni d'exemples de code\n" +
                "6. Utilise uniquement des descriptions textuelles et des explications conceptuelles\n" +
                "7. Si tu mentionnes une méthode ou une syntaxe, décris-la avec des mots, pas avec du code\n\n" +
                
                "=== FORMAT ===\n" +
                "Réponds en 2-3 phrases maximum, directement et sans préambule.\n" +
                "Exemple CORRECT: 'Tu retournes une chaîne vide. Pour inverser une chaîne, parcours-la de la fin (index length()-1) vers le début (index 0) et construis la nouvelle chaîne caractère par caractère.'\n" +
                "Exemple INCORRECT (à éviter): 'Utilise: return new StringBuilder(str).reverse().toString();'\n\n" +
                
                "=== RAPPEL FINAL ===\n" +
                "AUCUN CODE JAVA. Seulement des explications textuelles et des conseils conceptuels.\n\n" +
                
                "Indice:";

        String hint = callLlamaAPI(prompt, 300); // Increased token limit for better hints
        
        // Post-process: Remove any code blocks or code snippets that might have been generated
        if (hint != null && !hint.trim().isEmpty()) {
            hint = removeCodeFromHint(hint);
        }
        
        if (hint == null || hint.trim().length() < 20) {
            // Provide a more helpful default hint
            if (errorMessage != null && errorMessage.contains("NullPointerException")) {
                return "Il semble y avoir une NullPointerException. Vérifie que tous les objets sont initialisés avant d'être utilisés, et que les méthodes ne retournent pas null.";
            } else if (errorMessage != null && errorMessage.contains("ArrayIndexOutOfBoundsException")) {
                return "Il y a une erreur d'index de tableau. Vérifie que les indices utilisés sont valides (entre 0 et la longueur du tableau - 1).";
            } else if (errorMessage != null && errorMessage.contains("StringIndexOutOfBoundsException")) {
                return "Il y a une erreur d'index de chaîne. Vérifie que les indices utilisés pour accéder aux caractères sont valides.";
            } else {
                return "Vérifie attentivement ta logique. Assure-toi que :\n1. Toutes les variables sont correctement initialisées\n2. Les conditions et boucles sont correctement écrites\n3. Les valeurs retournées correspondent à ce qui est attendu par le test";
            }
        }
        return hint.trim();
    }
    
    /**
     * Remove code blocks and code snippets from hint text
     * This ensures hints are conceptual explanations only, not code solutions
     */
    private String removeCodeFromHint(String hint) {
        if (hint == null || hint.isEmpty()) {
            return hint;
        }
        
        String cleaned = hint;
        
        // Remove code blocks with triple backticks
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");
        
        // Remove code blocks with single backticks (inline code that might be full snippets)
        // But keep short inline references like "length()" or "charAt()"
        cleaned = cleaned.replaceAll("`([^`]{20,})`", "$1"); // Remove backticks from long code snippets
        
        // Remove lines that look like Java code (containing common Java keywords and syntax)
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip lines that look like code (contain method calls, assignments, etc.)
            if (trimmed.contains("public ") && trimmed.contains("(") ||
                trimmed.contains("private ") && trimmed.contains("(") ||
                trimmed.contains("return ") && trimmed.contains(";") ||
                (trimmed.contains("=") && trimmed.contains(";") && trimmed.length() > 30) ||
                trimmed.startsWith("import ") ||
                trimmed.startsWith("package ") ||
                trimmed.matches(".*\\{[^}]*\\}.*") && trimmed.length() > 50) {
                // Skip this line as it looks like code
                continue;
            }
            result.append(line).append("\n");
        }
        cleaned = result.toString();
        
        // Remove any remaining code-like patterns
        cleaned = cleaned.replaceAll("public\\s+\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("return\\s+[^;]+;", "");
        
        // Clean up extra whitespace
        cleaned = cleaned.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");
        cleaned = cleaned.trim();
        
        // If we removed too much and hint is now too short, return original with just backticks removed
        if (cleaned.length() < 30 && hint.length() > 50) {
            cleaned = hint.replaceAll("```[\\s\\S]*?```", "");
            cleaned = cleaned.replaceAll("`([^`]+)`", "$1");
        }
        
        return cleaned.trim();
    }
    
    /**
     * 使用 RAG 生成提示（新方法）
     */
    public String generateHintWithRAG(String userQuestion, String testName, 
                                      String testCode, String studentCode, 
                                      String errorMessage, Exercise exercise) {
        logger.info("Generating hint with RAG for: {}", userQuestion);
        
        // 如果 RAG 服务不可用，回退到原始方法
        if (semanticSearchService == null || exercise == null) {
            logger.warn("RAG services not available, falling back to standard hint generation");
            return generateHint(testName, testCode, studentCode, errorMessage, 
                exercise != null ? exercise.getProblemStatement() : null);
        }
        
        try {
            // 使用语义搜索构建增强提示词
            String augmentedPrompt = semanticSearchService.buildAugmentedPrompt(
                userQuestion != null ? userQuestion : "Comment corriger cette erreur?",
                studentCode,
                errorMessage,
                exercise
            );
            
            // 调用 LLM 生成回答
            String hint = callLlamaAPI(augmentedPrompt, 300);
            
            // 保存生成的提示到知识库（用于未来检索）
            if (hint != null && !hint.trim().isEmpty() && knowledgeBaseRepository != null) {
                saveToKnowledgeBase(hint, "hint", exercise, studentCode, errorMessage, userQuestion);
            }
            
            return hint != null && !hint.trim().isEmpty() 
                ? hint.trim() 
                : generateFallbackHint(errorMessage);
                
        } catch (Exception e) {
            logger.error("Error generating hint with RAG, falling back", e);
            return generateHint(testName, testCode, studentCode, errorMessage,
                exercise != null ? exercise.getProblemStatement() : null);
        }
    }
    
    /**
     * 保存到知识库
     */
    private void saveToKnowledgeBase(String content, String contentType, 
                                     Exercise exercise, String studentCode, 
                                     String errorMessage, String userQuestion) {
        try {
            if (embeddingService == null || knowledgeBaseRepository == null) {
                return;
            }
            
            KnowledgeBase kb = new KnowledgeBase();
            kb.setContent(content);
            kb.setContentType(contentType);
            kb.setExercise(exercise);
            
            // 生成并存储嵌入向量
            String textForEmbedding = content + " " + 
                (errorMessage != null ? errorMessage : "") + " " +
                (userQuestion != null ? userQuestion : "");
            float[] embedding = embeddingService.generateEmbedding(textForEmbedding);
            List<Float> embeddingList = new ArrayList<>();
            for (float f : embedding) {
                embeddingList.add(f);
            }
            kb.setEmbeddingJson(objectMapper.writeValueAsString(embeddingList));
            
            // 保存元数据
            Map<String, String> metadata = new HashMap<>();
            if (studentCode != null) {
                metadata.put("studentCode", studentCode.substring(0, Math.min(100, studentCode.length())));
            }
            if (errorMessage != null) {
                metadata.put("errorMessage", errorMessage);
            }
            if (userQuestion != null) {
                metadata.put("userQuestion", userQuestion);
            }
            kb.setMetadata(objectMapper.writeValueAsString(metadata));
            
            knowledgeBaseRepository.save(kb);
            logger.info("Saved knowledge base entry: {}", kb.getId());
        } catch (Exception e) {
            logger.error("Error saving to knowledge base", e);
        }
    }
    
    /**
     * 生成回退提示
     */
    private String generateFallbackHint(String errorMessage) {
        if (errorMessage != null && errorMessage.contains("NullPointerException")) {
            return "Il semble y avoir une NullPointerException. Vérifie que tous les objets sont initialisés avant d'être utilisés, et que les méthodes ne retournent pas null.";
        } else if (errorMessage != null && errorMessage.contains("ArrayIndexOutOfBoundsException")) {
            return "Il y a une erreur d'index de tableau. Vérifie que les indices utilisés sont valides (entre 0 et la longueur du tableau - 1).";
        } else if (errorMessage != null && errorMessage.contains("StringIndexOutOfBoundsException")) {
            return "Il y a une erreur d'index de chaîne. Vérifie que les indices utilisés pour accéder aux caractères sont valides.";
        } else {
            return "Vérifie attentivement ta logique. Assure-toi que :\n1. Toutes les variables sont correctement initialisées\n2. Les conditions et boucles sont correctement écrites\n3. Les valeurs retournées correspondent à ce qui est attendu par le test";
        }
    }
    

    
    /**
     * Extract method body from student code
     */
    private String extractMethodBody(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        
        // Try to find method body between first { and matching }
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
    // 8) Sujet / Concepts / Exemples
    // ============================================================
    private String detectConceptsFromTask(String task, String difficulty) {
        StringBuilder sb = new StringBuilder();
        sb.append("Java, ").append(difficulty == null ? "L1" : difficulty);

        if (task != null) {
            String t = task.toLowerCase(Locale.ROOT);
            if (t.contains("tableau") || t.contains("array") || t.contains("liste")) {
                sb.append("; Tableaux");
            }
            if (t.contains("mot") || t.contains("string") || t.contains("chaîne")) {
                sb.append("; Chaînes de caractères");
            }
            if (t.contains("tri") || t.contains("trier") || t.contains("sort")) {
                sb.append("; Algorithmes de tri");
            }
            if (t.contains("recherche") || t.contains("chercher") || t.contains("find")) {
                sb.append("; Recherche");
            }
            if (t.contains("récurs") || t.contains("recurs")) {
                sb.append("; Récursivité");
            }
        }
        return sb.toString();
    }

    private String generateExamplesFromTask(String task, String className) {
        if (task == null || task.isBlank()) {
            return "Voir l'énoncé pour les exemples d'entrées / sorties.";
        }
    


        String prompt =
            "Génère 2-3 exemples pour : " + task + "\n\n" +
            "Format : Entrée : ... → Sortie : ...\n" +
            "Une ligne par exemple, en français, sans code ni markdown";
    
        String examples = callLlamaAPI(prompt, 180);
        examples = sanitize(examples);
    

        if (examples == null ) {
            return "Exemples d'utilisation :\n"
                 + "- Choisissez quelques entrées adaptées à la consigne (cas simple, cas limite) et indiquez la sortie attendue.\n"
                 + "- Par exemple : Entrée : valeur simple → Sortie : résultat conforme à la description.";
        }
    
        return examples.trim();
    }
    
    /**
     * 使用 RAG 生成示例
     */
    private String generateExamplesWithRAG(String task, String className, 
                                          String fullTask, String language, String difficulty) {
        // 1. 搜索相似的示例
        String query = task + " " + language + " examples";
        List<KnowledgeBase> similarExamples = semanticSearchService.semanticSearch(query, 3, null);
        
        // 2. 构建增强提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("=== EXEMPLES D'UTILISATION SIMILAIRES ===\n\n");
        
        if (similarExamples.isEmpty()) {
            promptBuilder.append("Aucun exemple similaire trouvé.\n\n");
        } else {
            for (int i = 0; i < similarExamples.size(); i++) {
                KnowledgeBase kb = similarExamples.get(i);
                // 查找包含示例的知识库条目
                if (kb.getContent().contains("Entrée") || kb.getContent().contains("Sortie") || 
                    kb.getContent().contains("Exemple")) {
                    promptBuilder.append(String.format("[Exemple %d]\n", i + 1));
                    String exampleContent = kb.getContent();
                    if (exampleContent.length() > 500) {
                        exampleContent = exampleContent.substring(0, 500) + "...";
                    }
                    promptBuilder.append(exampleContent);
                    promptBuilder.append("\n\n");
                }
            }
        }
        
        promptBuilder.append("=== NOUVELLE TÂCHE ===\n");
        promptBuilder.append("Consigne de l'exercice (en langage naturel) : ").append(fullTask).append("\n\n");
        promptBuilder.append("=== INSTRUCTIONS ===\n");
        promptBuilder.append("En t'inspirant des exemples ci-dessus, génère 2 ou 3 exemples d'utilisation en FRANÇAIS pour cet exercice.\n");
        promptBuilder.append("Chaque exemple doit être sur une seule ligne, sous la forme :\n");
        promptBuilder.append("Entrée : ... → Sortie : ...\n");
        promptBuilder.append("Ne donne PAS de code, PAS de signature de fonction, PAS de markdown.\n");
        promptBuilder.append("Réponds uniquement avec les lignes d'exemples, rien d'autre.");
        
        String examples = callLlamaAPI(promptBuilder.toString(), 180);
        examples = sanitize(examples);
        
        if (examples == null || examples.trim().isEmpty()) {
            return "Exemples d'utilisation :\n"
                 + "- Choisissez quelques entrées adaptées à la consigne (cas simple, cas limite) et indiquez la sortie attendue.\n"
                 + "- Par exemple : Entrée : valeur simple → Sortie : résultat conforme à la description.";
        }
        
        return examples.trim();
    }

    // ============================================================
    // 9) 类名生成：按题目来
    // ============================================================
    private String generateClassNameFromTask(String task) {
        if (task == null || task.trim().isEmpty()) {
            return "Solution";
        }
        String t = task.toLowerCase(Locale.ROOT);

        if (t.contains("compter") && t.contains("mot")) {
            return "CountWords";
        }
        if ((t.contains("somme") || t.contains("sum")) &&
            (t.contains("tableau") || t.contains("array") || t.contains("liste"))) {
            return "ArraySum";
        }
        if ((t.contains("maximum") || t.contains("plus grand") || t.contains("max")) &&
            (t.contains("tableau") || t.contains("array") || t.contains("liste"))) {
            return "ArrayMax";
        }
        if (t.contains("palindrome")) {
            return "PalindromeChecker";
        }

        String clean = task.trim();
        String[] words = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < Math.min(words.length, 4); i++) {
            String w = words[i].replaceAll("[^a-zA-Z0-9]", "");
            if (w.length() <= 2) continue;
            String lower = w.toLowerCase(Locale.ROOT);
            if (lower.equals("les") || lower.equals("des") || lower.equals("une") ||
                lower.equals("pour") || lower.equals("avec") || lower.equals("dans") ||
                lower.equals("où") || lower.equals("que") || lower.equals("the") ||
                lower.equals("and") || lower.equals("students") || lower.equals("étudiants")) {
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

    // ============================================================
    // 10) 各种工具函数
    // ============================================================
    private String sanitize(String text) {
        if (text == null) return "";
        String t = text;

        t = t.replaceAll("(?i)```[a-zA-Z]*", "");
        t = t.replaceAll("```", "");
        t = t.replaceAll("(?i)^\\s*example\\s*:\\s*", "");
        t = t.replaceAll("(?i)^\\s*solution\\s*:\\s*", "");
        t = t.replaceAll("(?i)^model:.*", "");
        t = t.trim();

        if (t.toLowerCase(Locale.ROOT).startsWith("voici")) {
            int idx = t.indexOf(':');
            if (idx > 0 && idx + 1 < t.length()) {
                t = t.substring(idx + 1).trim();
            }
        }
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() > 2) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t.trim();
    }

    /**
     * 从解决方案代码中提取方法签名
     */
    private String extractMethodSignatures(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        StringBuilder signatures = new StringBuilder();
        String[] lines = code.split("\n");
        boolean inMethod = false;
        StringBuilder currentMethod = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            // 检测方法开始
            if (trimmed.startsWith("public static") && trimmed.contains("(")) {
                if (inMethod) {
                    // 保存上一个方法
                    signatures.append(currentMethod.toString().trim()).append("\n");
                }
                currentMethod = new StringBuilder();
                inMethod = true;
                // 提取到方法签名结束
                int endIndex = trimmed.indexOf(')');
                if (endIndex > 0) {
                    currentMethod.append(trimmed.substring(0, endIndex + 1));
                } else {
                    currentMethod.append(trimmed);
                }
            } else if (inMethod && trimmed.contains(")")) {
                // 方法签名可能跨行
                currentMethod.append(" ").append(trimmed);
                int endIndex = trimmed.indexOf(')');
                if (endIndex >= 0) {
                    signatures.append(currentMethod.toString().trim()).append("\n");
                    currentMethod = new StringBuilder();
                    inMethod = false;
                }
            } else if (inMethod && !trimmed.isEmpty() && !trimmed.startsWith("//")) {
                currentMethod.append(" ").append(trimmed);
            }
        }
        if (inMethod && currentMethod.length() > 0) {
            signatures.append(currentMethod.toString().trim()).append("\n");
        }
        return signatures.toString();
    }
    
    private String cleanJavaSnippet(String code) {
        if (code == null) return "";
        String cleaned = code.trim();
        // 移除markdown代码块标记
        cleaned = cleaned.replaceAll("(?i)```java", "");
        cleaned = cleaned.replaceAll("```", "");
        // 移除多余的说明文字
        cleaned = cleaned.replaceAll("(?i)^.*?public\\s+class", "public class");
        cleaned = cleaned.replaceAll("(?i)^.*?class\\s+", "class ");

        StringBuilder sb = new StringBuilder();
        String[] lines = cleaned.split("\n");
        boolean codeStarted = false;
        for (String line : lines) {
            String trim = line.trim();
            // 跳过package声明
            if (trim.startsWith("package ")) {
                continue;
            }
            // 检测代码开始
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

        int start = -1;
        if (idxImport >= 0 && idxClass >= 0) {
            start = Math.min(idxImport, idxClass);
        } else if (idxImport >= 0) {
            start = idxImport;
        } else if (idxClass >= 0) {
            start = idxClass;
        }

        if (start > 0) {
            cleaned = cleaned.substring(start);
        }

        if (expectedTestClassName != null && !expectedTestClassName.isBlank()) {
            cleaned = cleaned.replaceAll("class\\s+\\w+Test", "class " + expectedTestClassName);
        }

        return cleaned.trim();
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
            int start = idx + "public class ".length();
            int end = start;
            while (end < code.length() &&
                   (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
                end++;
            }
            if (end > start) {
                return code.substring(start, end);
            }
        }

        idx = code.indexOf("class ");
        if (idx >= 0) {
            int start = idx + "class ".length();
            int end = start;
            while (end < code.length() &&
                   (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
                end++;
            }
            if (end > start) {
                return code.substring(start, end);
            }
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
                if (trimmed.contains("{")) {
                    braceDepth = 1;
                } else {
                    braceDepth = 0;
                }
                continue;
            }

            if (inMain) {
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
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
}
