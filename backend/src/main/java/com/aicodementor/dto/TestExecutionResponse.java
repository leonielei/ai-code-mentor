package com.aicodementor.dto;

import java.util.List;
import java.util.Objects;

public class TestExecutionResponse {
    private boolean allTestsPassed;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private List<TestResult> testResults;
    private String compilationError;
    
    public TestExecutionResponse() {
    }
    
    public TestExecutionResponse(boolean allTestsPassed, int totalTests, int passedTests, int failedTests, List<TestResult> testResults, String compilationError) {
        this.allTestsPassed = allTestsPassed;
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.testResults = testResults;
        this.compilationError = compilationError;
    }
    
    // Getters and Setters
    public boolean isAllTestsPassed() {
        return allTestsPassed;
    }
    
    public void setAllTestsPassed(boolean allTestsPassed) {
        this.allTestsPassed = allTestsPassed;
    }
    
    public int getTotalTests() {
        return totalTests;
    }
    
    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }
    
    public int getPassedTests() {
        return passedTests;
    }
    
    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }
    
    public int getFailedTests() {
        return failedTests;
    }
    
    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }
    
    public List<TestResult> getTestResults() {
        return testResults;
    }
    
    public void setTestResults(List<TestResult> testResults) {
        this.testResults = testResults;
    }
    
    public String getCompilationError() {
        return compilationError;
    }
    
    public void setCompilationError(String compilationError) {
        this.compilationError = compilationError;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestExecutionResponse that = (TestExecutionResponse) o;
        return allTestsPassed == that.allTestsPassed &&
                totalTests == that.totalTests &&
                passedTests == that.passedTests &&
                failedTests == that.failedTests &&
                Objects.equals(testResults, that.testResults) &&
                Objects.equals(compilationError, that.compilationError);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(allTestsPassed, totalTests, passedTests, failedTests, testResults, compilationError);
    }
    
    @Override
    public String toString() {
        return "TestExecutionResponse{" +
                "allTestsPassed=" + allTestsPassed +
                ", totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", testResults=" + testResults +
                ", compilationError='" + compilationError + '\'' +
                '}';
    }
    
    public static class TestResult {
        private String testName;
        private boolean passed;
        private String message;
        private String hint; // LLM-generated hint for failed tests
        
        public TestResult() {
        }
        
        public TestResult(String testName, boolean passed, String message, String hint) {
            this.testName = testName;
            this.passed = passed;
            this.message = message;
            this.hint = hint;
        }
        
        // Getters and Setters
        public String getTestName() {
            return testName;
        }
        
        public void setTestName(String testName) {
            this.testName = testName;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public void setPassed(boolean passed) {
            this.passed = passed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getHint() {
            return hint;
        }
        
        public void setHint(String hint) {
            this.hint = hint;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestResult that = (TestResult) o;
            return passed == that.passed &&
                    Objects.equals(testName, that.testName) &&
                    Objects.equals(message, that.message) &&
                    Objects.equals(hint, that.hint);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(testName, passed, message, hint);
        }
        
        @Override
        public String toString() {
            return "TestResult{" +
                    "testName='" + testName + '\'' +
                    ", passed=" + passed +
                    ", message='" + message + '\'' +
                    ", hint='" + hint + '\'' +
                    '}';
        }
    }
}
