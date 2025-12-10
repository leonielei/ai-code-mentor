# RAG (检索增强生成) 实现说明

## 概述

本项目已实现 RAG (Retrieval-Augmented Generation) 功能，通过以下步骤提升 LLM 回答质量：

1. **嵌入向量生成 (Embeddings)**: 将用户问题和知识库内容转换为向量
2. **Top-K 语义搜索**: 从知识库中检索最相关的上下文
3. **增强提示词 (Augmented Prompt)**: 将检索到的上下文与用户问题拼接
4. **调用 DeepSeekCoder**: 使用增强后的提示词生成最终回答

## 架构组件

### 1. KnowledgeBase 实体
- 存储知识库内容（解决方案、提示、错误模式等）
- 包含嵌入向量（JSON 格式）
- 关联到 Exercise 实体

### 2. EmbeddingService
- **主要功能**: 生成文本的嵌入向量
- **实现方式**:
  - 优先使用 HuggingFace Inference API (免费，无需 API key)
  - 降级方案: 基于文本特征的简单嵌入向量
- **配置**: `application.yml` 中的 `embedding` 部分

### 3. SemanticSearchService
- **主要功能**: Top-K 语义搜索
- **实现**:
  - 计算查询向量与知识库条目的余弦相似度
  - 返回最相关的 K 个结果
  - 构建增强提示词

### 4. LLMService 增强
- **新增方法**:
  - `generateHintWithRAG()`: 使用 RAG 生成提示
  - `generateSolutionCodeWithRAG()`: 使用 RAG 生成解决方案
- **自动保存**: 生成的提示和解决方案自动保存到知识库

## 配置

### application.yml

```yaml
embedding:
  provider: huggingface  # 或 "simple" 使用简单文本特征
  huggingface:
    api-url: https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2
    api-key:  # 可选：添加 API key 提高速率限制
  dimension: 384  # 嵌入向量维度
```

### 使用 HuggingFace API

1. **免费使用**（无需 API key）:
   - 直接使用配置的 URL
   - 有速率限制（约 30 请求/分钟）

2. **使用 API Key**（推荐）:
   - 在 [HuggingFace](https://huggingface.co/settings/tokens) 获取 API key
   - 添加到 `application.yml` 的 `embedding.huggingface.api-key`
   - 提高速率限制到 1000 请求/分钟

## API 使用

### 生成提示（带 RAG）

**请求**:
```json
POST /api/llm/get-hint
{
  "testName": "testBasicCase",
  "testCode": "...",
  "studentCode": "...",
  "errorMessage": "NullPointerException",
  "userQuestion": "为什么会出现这个错误？",  // 新增
  "exerciseId": 1  // 新增
}
```

**响应**: 包含相关上下文的增强提示

### 生成练习（自动使用 RAG）

如果 `SemanticSearchService` 可用，练习生成会自动使用 RAG：

```json
POST /api/llm/generate-exercise
{
  "naturalLanguageDescription": "实现一个反转字符串的函数",
  "programmingLanguage": "Java",
  "targetDifficulty": "L1"
}
```

系统会：
1. 搜索相似的练习示例
2. 使用这些示例构建增强提示词
3. 生成更高质量的练习
4. 自动保存到知识库

## 知识库构建

### 自动构建

系统会自动保存以下内容到知识库：
- 生成的提示 (`contentType: "hint"`)
- 生成的练习解决方案 (`contentType: "exercise_example"`)

### 手动构建（可选）

可以通过数据库直接插入知识库条目，或创建初始化服务：

```java
@PostConstruct
public void initializeKnowledgeBase() {
    // 为现有练习生成嵌入向量
    List<Exercise> exercises = exerciseRepository.findAll();
    for (Exercise exercise : exercises) {
        if (exercise.getSolution() != null) {
            // 保存到知识库
        }
    }
}
```

## 性能优化建议

1. **缓存嵌入向量**: 相同文本的嵌入向量可以缓存
2. **批量处理**: 使用 `EmbeddingService.generateEmbeddings()` 批量生成
3. **索引优化**: 对于大规模知识库，考虑使用向量数据库（如 Milvus、Qdrant）
4. **异步处理**: 嵌入向量生成可以异步执行

## 降级机制

如果 RAG 服务不可用，系统会自动降级：
- **Hint 生成**: 回退到标准 `generateHint()` 方法
- **练习生成**: 回退到标准 `generateSolutionCode()` 方法
- **嵌入向量**: 使用简单文本特征（`provider: simple`）

## 监控和调试

### 日志级别

设置日志级别查看详细信息：
```yaml
logging:
  level:
    com.aicodementor.service.EmbeddingService: DEBUG
    com.aicodementor.service.SemanticSearchService: DEBUG
```

### 检查知识库

通过 H2 控制台查看知识库内容：
```
http://localhost:8080/h2-console
```

查询：
```sql
SELECT id, content_type, LEFT(content, 100) as content_preview, 
       LENGTH(embedding) as embedding_size
FROM knowledge_base;
```

## 未来改进

1. **向量数据库集成**: 使用专门的向量数据库提高搜索性能
2. **更高质量的嵌入模型**: 使用更大的模型（如 all-mpnet-base-v2）
3. **混合搜索**: 结合关键词搜索和语义搜索
4. **知识库管理界面**: 允许教师管理知识库内容
5. **相似度阈值调整**: 根据实际效果调整相似度阈值

## 注意事项

1. **llama.cpp 不支持 embedding**: 因此使用外部 API 或简单文本特征
2. **HuggingFace API 速率限制**: 免费版有速率限制，建议使用 API key
3. **嵌入向量维度**: 确保与使用的模型匹配（all-MiniLM-L6-v2 使用 384 维）
4. **数据库存储**: 嵌入向量以 JSON 格式存储，对于大规模数据可能影响性能

## 测试

测试 RAG 功能：

1. **生成一个练习**: 系统会自动保存到知识库
2. **生成提示**: 使用 `exerciseId` 和 `userQuestion` 参数
3. **检查知识库**: 确认内容已保存并包含嵌入向量
4. **验证相似度搜索**: 使用相似的问题测试检索效果


