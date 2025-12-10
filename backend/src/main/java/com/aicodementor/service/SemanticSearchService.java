package com.aicodementor.service;

import com.aicodementor.entity.Exercise;
import com.aicodementor.entity.KnowledgeBase;
import com.aicodementor.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义搜索服务
 * 实现 Top-K 语义搜索，用于 RAG（检索增强生成）
 */
@Service
public class SemanticSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchService.class);
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Top-K 语义搜索
     * @param query 查询文本
     * @param topK 返回前K个最相关的结果
     * @param exerciseId 可选的练习ID，用于过滤
     * @return 按相似度排序的知识库条目列表
     */
    public List<KnowledgeBase> semanticSearch(String query, int topK, Long exerciseId) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.debug("Performing semantic search for query: {}", 
            query.length() > 100 ? query.substring(0, 100) + "..." : query);
        
        // 1. 生成查询向量
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        
        // 2. 获取所有知识库条目（可优化为只获取相关exercise的）
        List<KnowledgeBase> allKnowledge = exerciseId != null
            ? knowledgeBaseRepository.findByExerciseId(exerciseId)
            : knowledgeBaseRepository.findAll();
        
        if (allKnowledge.isEmpty()) {
            logger.debug("No knowledge base entries found");
            return new ArrayList<>();
        }
        
        // 3. 计算相似度并排序
        List<SearchResult> results = allKnowledge.stream()
            .map(kb -> {
                try {
                    float[] kbEmbedding = parseEmbedding(kb.getEmbeddingJson());
                    if (kbEmbedding == null || kbEmbedding.length == 0) {
                        // 如果知识库条目没有嵌入向量，生成一个
                        kbEmbedding = embeddingService.generateEmbedding(kb.getContent());
                        // 保存生成的嵌入向量
                        try {
                            List<Float> embeddingList = new ArrayList<>();
                            for (float f : kbEmbedding) {
                                embeddingList.add(f);
                            }
                            kb.setEmbeddingJson(objectMapper.writeValueAsString(embeddingList));
                            knowledgeBaseRepository.save(kb);
                        } catch (Exception e) {
                            logger.warn("Failed to save embedding for KB {}", kb.getId(), e);
                        }
                    }
                    double similarity = embeddingService.cosineSimilarity(queryEmbedding, kbEmbedding);
                    return new SearchResult(kb, similarity);
                } catch (Exception e) {
                    logger.warn("Error computing similarity for KB {}", kb.getId(), e);
                    return new SearchResult(kb, 0.0);
                }
            })
            .filter(sr -> sr.similarity > 0.1) // 过滤低相似度（阈值可配置）
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(topK)
            .collect(Collectors.toList());
        
        logger.debug("Found {} relevant results (top {} requested)", results.size(), topK);
        
        return results.stream()
            .map(sr -> sr.knowledgeBase)
            .collect(Collectors.toList());
    }
    
    /**
     * 构建增强提示词（Augmented Prompt）
     * 将检索到的上下文与用户问题拼接，形成完整的提示词
     */
    public String buildAugmentedPrompt(String userQuestion, String studentCode, 
                                       String errorMessage, Exercise exercise) {
        // 1. 构建查询文本
        StringBuilder queryBuilder = new StringBuilder();
        if (userQuestion != null && !userQuestion.trim().isEmpty()) {
            queryBuilder.append(userQuestion).append(" ");
        }
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            queryBuilder.append(errorMessage).append(" ");
        }
        if (exercise != null && exercise.getProblemStatement() != null) {
            queryBuilder.append(exercise.getProblemStatement()).append(" ");
        }
        String query = queryBuilder.toString().trim();
        
        // 2. 语义搜索相关上下文
        Long exerciseId = exercise != null ? exercise.getId() : null;
        List<KnowledgeBase> relevantContexts = semanticSearch(query, 5, exerciseId);
        
        // 3. 构建上下文字符串
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("=== CONTEXTE PERTINENT (Recherche sémantique) ===\n\n");
        
        if (relevantContexts.isEmpty()) {
            contextBuilder.append("Aucun contexte similaire trouvé dans la base de connaissances.\n\n");
        } else {
            for (int i = 0; i < relevantContexts.size(); i++) {
                KnowledgeBase kb = relevantContexts.get(i);
                contextBuilder.append(String.format("[Contexte %d - Type: %s]\n", 
                    i + 1, kb.getContentType()));
                contextBuilder.append(kb.getContent());
                contextBuilder.append("\n\n");
            }
        }
        
        // 4. 添加当前练习信息
        if (exercise != null) {
            contextBuilder.append("=== EXERCICE ACTUEL ===\n");
            if (exercise.getTitle() != null) {
                contextBuilder.append("Titre: ").append(exercise.getTitle()).append("\n");
            }
            if (exercise.getProblemStatement() != null) {
                contextBuilder.append("Énoncé: ").append(exercise.getProblemStatement()).append("\n");
            }
            if (exercise.getConcepts() != null) {
                contextBuilder.append("Concepts: ").append(exercise.getConcepts()).append("\n");
            }
            contextBuilder.append("\n");
        }
        
        // 5. 添加学生代码和错误
        contextBuilder.append("=== CODE DE L'ÉTUDIANT ===\n");
        contextBuilder.append(studentCode != null ? studentCode : "").append("\n\n");
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            contextBuilder.append("=== ERREUR ===\n");
            contextBuilder.append(errorMessage).append("\n\n");
        }
        
        // 6. 添加用户问题
        if (userQuestion != null && !userQuestion.trim().isEmpty()) {
            contextBuilder.append("=== QUESTION DE L'ÉTUDIANT ===\n");
            contextBuilder.append(userQuestion).append("\n\n");
        }
        
        // 7. 添加指令
        contextBuilder.append("=== INSTRUCTIONS ===\n");
        contextBuilder.append("En utilisant le contexte pertinent ci-dessus, ");
        contextBuilder.append("donne une réponse précise et actionnable à la question de l'étudiant.\n");
        contextBuilder.append("Sois concis (2-3 phrases maximum) et orienté solution.\n");
        
        return contextBuilder.toString();
    }
    
    /**
     * 为练习生成构建增强提示词
     */
    public String buildAugmentedPromptForExerciseGeneration(String naturalLanguageDescription, 
                                                           String language, String difficulty) {
        // 1. 搜索相似的练习示例
        String query = naturalLanguageDescription + " " + language + " " + difficulty;
        List<KnowledgeBase> similarExercises = semanticSearch(query, 3, null);
        
        // 2. 构建提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("=== EXEMPLES D'EXERCICES SIMILAIRES ===\n\n");
        
        if (similarExercises.isEmpty()) {
            promptBuilder.append("Aucun exemple similaire trouvé.\n\n");
        } else {
            for (int i = 0; i < similarExercises.size(); i++) {
                KnowledgeBase kb = similarExercises.get(i);
                if ("exercise_example".equals(kb.getContentType()) || 
                    "solution".equals(kb.getContentType())) {
                    promptBuilder.append(String.format("[Exemple %d]\n", i + 1));
                    // 限制长度，避免提示词过长
                    String content = kb.getContent();
                    if (content.length() > 1000) {
                        content = content.substring(0, 1000) + "...";
                    }
                    promptBuilder.append(content);
                    promptBuilder.append("\n\n");
                }
            }
        }
        
        promptBuilder.append("=== NOUVELLE DEMANDE ===\n");
        promptBuilder.append("Description: ").append(naturalLanguageDescription).append("\n");
        promptBuilder.append("Langage: ").append(language).append("\n");
        promptBuilder.append("Difficulté: ").append(difficulty).append("\n\n");
        
        promptBuilder.append("=== INSTRUCTIONS ===\n");
        promptBuilder.append("Génère un exercice similaire aux exemples ci-dessus, ");
        promptBuilder.append("mais adapté à la nouvelle demande.\n");
        promptBuilder.append("Inspire-toi de la structure, du style et de la qualité des exemples.\n");
        
        return promptBuilder.toString();
    }
    
    /**
     * 为测试生成构建增强提示词
     */
    public String buildAugmentedPromptForTestGeneration(String task, String solution, 
                                                       String className, String language) {
        // 搜索相似的测试用例
        String query = task + " " + solution.substring(0, Math.min(200, solution.length())) + " test";
        List<KnowledgeBase> similarTests = semanticSearch(query, 3, null);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("=== EXEMPLES DE TESTS SIMILAIRES ===\n\n");
        
        if (similarTests.isEmpty()) {
            promptBuilder.append("Aucun exemple de test similaire trouvé.\n\n");
        } else {
            for (int i = 0; i < similarTests.size(); i++) {
                KnowledgeBase kb = similarTests.get(i);
                if (kb.getContent().contains("@Test") || kb.getContentType().contains("test")) {
                    promptBuilder.append(String.format("[Exemple de test %d]\n", i + 1));
                    String testContent = kb.getContent();
                    if (testContent.length() > 800) {
                        testContent = testContent.substring(0, 800) + "...";
                    }
                    promptBuilder.append(testContent);
                    promptBuilder.append("\n\n");
                }
            }
        }
        
        return promptBuilder.toString();
    }
    
    /**
     * 解析嵌入向量JSON字符串
     */
    private float[] parseEmbedding(String embeddingJson) {
        if (embeddingJson == null || embeddingJson.trim().isEmpty()) {
            return null;
        }
        
        try {
            List<Double> list = objectMapper.readValue(embeddingJson, 
                new TypeReference<List<Double>>() {});
            float[] embedding = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                embedding[i] = list.get(i).floatValue();
            }
            return embedding;
        } catch (Exception e) {
            logger.warn("Failed to parse embedding JSON", e);
            return null;
        }
    }
    
    /**
     * 内部类：搜索结果
     */
    private static class SearchResult {
        KnowledgeBase knowledgeBase;
        double similarity;
        
        SearchResult(KnowledgeBase kb, double sim) {
            this.knowledgeBase = kb;
            this.similarity = sim;
        }
    }
}

