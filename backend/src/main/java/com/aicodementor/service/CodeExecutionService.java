package com.aicodementor.service;

import com.aicodementor.dto.TestExecutionResponse;
import com.aicodementor.entity.Exercise;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class CodeExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeExecutionService.class);
    
    @Autowired
    private LLMService llmService;
    
    /**
     * Execute student code against test cases
     */
    public TestExecutionResponse executeTests(Exercise exercise, String studentCode) {
        logger.info("Executing tests for exercise: {}", exercise.getId());
        
        TestExecutionResponse response = new TestExecutionResponse();
        List<TestExecutionResponse.TestResult> testResults = new ArrayList<>();
        
        try {
            // Create temporary directory for compilation
            Path tempDir = Files.createTempDirectory("code-execution");
            
            // Compile student code
            String compilationError = compileCode(studentCode, exercise.getUnitTests(), tempDir);
            if (compilationError != null) {
                response.setCompilationError(compilationError);
                response.setAllTestsPassed(false);
                response.setTotalTests(0);
                response.setPassedTests(0);
                response.setFailedTests(0);
                response.setTestResults(testResults);
                return response;
            }
            
            // Run tests
            testResults = runTests(tempDir, exercise, studentCode);
            
            // Calculate statistics
            int totalTests = testResults.size();
            int passedTests = (int) testResults.stream().filter(TestExecutionResponse.TestResult::isPassed).count();
            int failedTests = totalTests - passedTests;
            
            response.setAllTestsPassed(failedTests == 0);
            response.setTotalTests(totalTests);
            response.setPassedTests(passedTests);
            response.setFailedTests(failedTests);
            response.setTestResults(testResults);
            
            // Clean up
            cleanupTempDirectory(tempDir);
            
        } catch (Exception e) {
            logger.error("Error executing tests", e);
            response.setCompilationError("Erreur d'exécution: " + e.getMessage());
            response.setAllTestsPassed(false);
            response.setTotalTests(0);
            response.setPassedTests(0);
            response.setFailedTests(0);
        }
        
        return response;
    }
    
    private String compileCode(String studentCode, String testCode, Path outputDir) {
        try {
            // Write source files
            Path studentFile = outputDir.resolve("Solution.java");
            Path testFile = outputDir.resolve("SolutionTest.java");
            
            Files.writeString(studentFile, studentCode);
            Files.writeString(testFile, testCode);
            
            // Get Java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return "Java compiler not available. Ensure you're running on a JDK, not a JRE.";
            }
            
            // Setup diagnostics
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            
            // Set output directory
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir.toFile()));
            
            // Get compilation units
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(
                    studentFile.toFile(),
                    testFile.toFile()
            );
            
            // Compile
            StringWriter output = new StringWriter();
            JavaCompiler.CompilationTask task = compiler.getTask(
                    output,
                    fileManager,
                    diagnostics,
                    Arrays.asList("-cp", System.getProperty("java.class.path")),
                    null,
                    compilationUnits
            );
            
            boolean success = task.call();
            fileManager.close();
            
            if (!success) {
                StringBuilder errors = new StringBuilder("Erreurs de compilation:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errors.append(String.format("Ligne %d: %s\n",
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(Locale.FRENCH)));
                }
                return errors.toString();
            }
            
            return null; // No compilation errors
            
        } catch (IOException e) {
            logger.error("Error during compilation", e);
            return "Erreur lors de la compilation: " + e.getMessage();
        }
    }
    
    private List<TestExecutionResponse.TestResult> runTests(Path classPath, Exercise exercise, String studentCode) {
        List<TestExecutionResponse.TestResult> results = new ArrayList<>();
        
        try {
            // Create custom class loader
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{classPath.toUri().toURL()},
                    this.getClass().getClassLoader()
            );
            
            // Discover and run tests
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass("SolutionTest"))
                    .build();
            
            Launcher launcher = LauncherFactory.create();
            
            // Custom listener to capture test results
            TestResultListener listener = new TestResultListener(exercise, studentCode, llmService);
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);
            
            results = listener.getResults();
            
        } catch (Exception e) {
            logger.error("Error running tests", e);
            TestExecutionResponse.TestResult errorResult = new TestExecutionResponse.TestResult();
            errorResult.setTestName("Erreur d'exécution");
            errorResult.setPassed(false);
            errorResult.setMessage("Erreur lors de l'exécution des tests: " + e.getMessage());
            errorResult.setHint("Vérifiez que votre code ne contient pas d'erreurs à l'exécution.");
            results.add(errorResult);
        }
        
        return results;
    }
    
    private void cleanupTempDirectory(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            logger.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }
    
    /**
     * Custom test execution listener to capture results
     */
    private static class TestResultListener implements TestExecutionListener {
        private final List<TestExecutionResponse.TestResult> results = new ArrayList<>();
        private final Exercise exercise;
        private final String studentCode;
        private final LLMService llmService;
        private final Map<String, TestIdentifier> testMap = new HashMap<>();
        
        public TestResultListener(Exercise exercise, String studentCode, LLMService llmService) {
            this.exercise = exercise;
            this.studentCode = studentCode;
            this.llmService = llmService;
        }
        
        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if (testIdentifier.isTest()) {
                testMap.put(testIdentifier.getUniqueId(), testIdentifier);
            }
        }
        
        @Override
        public void executionFinished(TestIdentifier testIdentifier, org.junit.platform.engine.TestExecutionResult testExecutionResult) {
            if (testIdentifier.isTest()) {
                String testName = testIdentifier.getDisplayName();
                boolean passed = testExecutionResult.getStatus() == org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
                String message = "";
                String hint = "";
                
                if (!passed && testExecutionResult.getThrowable().isPresent()) {
                    Throwable throwable = testExecutionResult.getThrowable().get();
                    message = throwable.getMessage();
                    
                    // Generate hint using LLM for failed tests
                    try {
                        hint = llmService.generateHint(testName, exercise.getUnitTests(), studentCode, message);
                    } catch (Exception e) {
                        hint = "Relisez attentivement l'énoncé et vérifiez votre logique.";
                    }
                }
                
                TestExecutionResponse.TestResult result = new TestExecutionResponse.TestResult();
                result.setTestName(testName);
                result.setPassed(passed);
                result.setMessage(message);
                result.setHint(hint);
                results.add(result);
            }
        }
        
        public List<TestExecutionResponse.TestResult> getResults() {
            return results;
        }
    }
}








