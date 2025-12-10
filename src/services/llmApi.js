import axios from 'axios'

// Use relative path to leverage Vite proxy in development
const API_BASE_URL = import.meta.env.DEV ? '/api' : 'http://localhost:8080/api'

const llmApi = {
  // Teacher: Generate exercise from natural language
  generateExercise(description, difficulty = 'L1', language = 'Java') {
    return axios.post(`${API_BASE_URL}/llm/generate-exercise`, {
      naturalLanguageDescription: description,
      targetDifficulty: difficulty,
      programmingLanguage: language
    })
  },

  // Teacher: Save exercise (with modifications)
  saveExercise(exerciseData) {
    return axios.post(`${API_BASE_URL}/llm/save-exercise`, exerciseData)
  },

  // Student: Execute tests
  executeTests(exerciseId, code, language = 'Java') {
    return axios.post(`${API_BASE_URL}/llm/execute-tests`, {
      exerciseId,
      code,
      language
    })
  },

  // Student: Get hint for failed test
  getHint(testName, testCode, studentCode, errorMessage) {
    return axios.post(`${API_BASE_URL}/llm/get-hint`, {
      testName,
      testCode,
      studentCode,
      errorMessage
    })
  }
}

export default llmApi










