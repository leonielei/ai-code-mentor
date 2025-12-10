package com.aicodementor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 嵌入向量生成服务
 * 由于 llama.cpp 不支持 embedding，我们使用以下方案：
 * 1. 优先尝试 HuggingFace Inference API（免费）
 * 2. 如果失败，使用基于文本特征的简单嵌入向量
 */
@Service
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${embedding.provider:huggingface}")
    private String embeddingProvider;
    
    @Value("${embedding.huggingface.api-url:https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/feature-extraction}")
    private String huggingfaceApiUrl;
    
    @Value("${embedding.huggingface.api-key:}")
    private String huggingfaceApiKey;
    
    @Value("${embedding.dimension:384}")
    private int embeddingDimension;
    
    /**
     * 生成文本的嵌入向量
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[embeddingDimension];
        }
        
        try {
            // 方法1: 尝试使用 HuggingFace API
            if ("huggingface".equalsIgnoreCase(embeddingProvider) && 
                huggingfaceApiUrl != null && !huggingfaceApiUrl.isEmpty()) {
                float[] embedding = generateEmbeddingWithHuggingFace(text);
                if (embedding != null && embedding.length > 0) {
                    return embedding;
                }
            }
            
            // 方法2: 使用简单的文本特征嵌入（降级方案）
            logger.debug("Using fallback embedding generation for text: {}", 
                text.length() > 50 ? text.substring(0, 50) + "..." : text);
            return generateSimpleEmbedding(text);
            
        } catch (Exception e) {
            logger.error("Error generating embedding, using fallback", e);
            return generateSimpleEmbedding(text);
        }
    }
    
    /**
     * 使用 HuggingFace Inference API 生成嵌入向量
     */
    private float[] generateEmbeddingWithHuggingFace(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + huggingfaceApiKey);
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", text);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // HuggingFace 返回的是二维数组，取第一个
            Object response = restTemplate.postForObject(
                huggingfaceApiUrl,
                request,
                Object.class
            );
            
            if (response instanceof List) {
                List<?> responseList = (List<?>) response;
                if (!responseList.isEmpty() && responseList.get(0) instanceof List) {
                    List<?> embeddingList = (List<?>) responseList.get(0);
                    float[] embedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        Object val = embeddingList.get(i);
                        if (val instanceof Number) {
                            embedding[i] = ((Number) val).floatValue();
                        }
                    }
                    return embedding;
                }
            }
            
            // 尝试解析 JSON 格式
            String responseStr = restTemplate.postForObject(
                huggingfaceApiUrl,
                request,
                String.class
            );
            
            if (responseStr != null) {
                JsonNode jsonNode = objectMapper.readTree(responseStr);
                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    JsonNode embeddingNode = jsonNode.get(0);
                    if (embeddingNode.isArray()) {
                        float[] embedding = new float[embeddingNode.size()];
                        for (int i = 0; i < embeddingNode.size(); i++) {
                            embedding[i] = (float) embeddingNode.get(i).asDouble();
                        }
                        return embedding;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("HuggingFace embedding API failed, using fallback: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 简单的嵌入向量生成（基于文本特征）
     * 这是一个降级方案，实际项目中建议使用专业嵌入模型
     */
    private float[] generateSimpleEmbedding(String text) {
        float[] embedding = new float[embeddingDimension];
        String lowerText = text.toLowerCase();
        
        // 提取关键词特征（Java 编程相关）
        String[] keywords = {
            "error", "exception", "null", "array", "string", "method", "class",
            "return", "loop", "condition", "if", "else", "for", "while",
            "public", "private", "static", "void", "int", "boolean", "double",
            "list", "map", "set", "collection", "stream", "lambda",
            "test", "assert", "fail", "pass", "compile", "runtime",
            "algorithm", "sort", "search", "binary", "recursion", "iteration"
        };
        
        // 计算关键词频率
        for (int i = 0; i < Math.min(keywords.length, embeddingDimension); i++) {
            int count = countOccurrences(lowerText, keywords[i]);
            embedding[i] = Math.min(1.0f, count / 10.0f); // 归一化到 0-1
        }
        
        // 文本长度特征
        if (keywords.length < embeddingDimension) {
            embedding[keywords.length] = Math.min(1.0f, text.length() / 1000.0f);
        }
        
        // 填充剩余维度（使用文本哈希）
        Random random = new Random(text.hashCode());
        for (int i = keywords.length + 1; i < embeddingDimension; i++) {
            embedding[i] = random.nextFloat() * 0.1f; // 小的随机值
        }
        
        // 归一化向量
        return normalize(embedding);
    }
    
    /**
     * 计算子字符串出现次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * 归一化向量（L2 归一化）
     */
    private float[] normalize(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm > 0.0) {
            float[] normalized = new float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                normalized[i] = (float) (vector[i] / norm);
            }
            return normalized;
        }
        
        return vector;
    }
    
    /**
     * 计算余弦相似度
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 批量生成嵌入向量（优化性能）
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }
}

