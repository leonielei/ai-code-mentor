# Sudden Death 规则修复计划

## 需要修复的问题

### 1. 并发处理 (CRITICAL)
- **问题**: `LLMService.callLlamaAPI()` 使用同步的 `RestTemplate`
- **要求**: 必须支持多个LLM请求并行处理
- **修复**: 使用 `CompletableFuture` 或 `@Async` 实现异步调用

### 2. 方法长度 (CRITICAL)
- **问题**: 多个方法超过20行
  - `generateHint()`: ~100行
  - `generateExercise()`: ~70行
  - `generateSolutionCode()`: ~60行
  - `generateJUnitTests()`: ~50行
  - `removeCodeFromHint()`: ~50行
  - `generateStarterCodeFromSolution()`: ~60行
- **修复**: 将长方法拆分为多个小方法（每个≤20行）

### 3. 全局变量 (CRITICAL)
- **问题**: 
  - `@Value("${llm.llamacpp.base-url}") private String llamacppBaseUrl;`
  - `private final RestTemplate restTemplate = new RestTemplate();`
  - `private final ObjectMapper objectMapper = new ObjectMapper();`
- **要求**: 禁止全局变量，包括依赖注入的字段
- **修复**: 通过构造函数注入，移除字段注入

### 4. 接口设计
- **检查**: 需要检查所有接口是否符合要求（0或1个方法）

### 5. 代码死代码
- **检查**: 需要检查是否有未使用的方法

### 6. POM.xml 依赖
- **检查**: 确保所有依赖都在允许列表中

### 7. instanceof/if-else vs 多态
- **检查**: 需要检查是否有可以用多态替代的instanceof

### 8. protected 字段/方法
- **检查**: 确保没有protected成员

### 9. @SuppressWarnings
- **检查**: 确保所有@SuppressWarnings都有理由

### 10. Raw types
- **检查**: 确保没有raw types

## 修复优先级

1. **P0 (立即修复)**: 并发处理、全局变量、方法长度
2. **P1 (高优先级)**: 接口设计、代码死代码
3. **P2 (中优先级)**: instanceof检查、protected检查
4. **P3 (低优先级)**: 其他检查项


