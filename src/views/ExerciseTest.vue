<template>
  <div class="container-fluid py-4">
    <div class="row">
      <div class="col-12">
        <div class="d-flex justify-content-between align-items-center mb-4">
          <h2 class="mb-0">
            <i class="fas fa-vial me-2"></i>
            Exécution des Tests
          </h2>
          <button
            class="btn btn-outline-secondary"
            @click="goBack"
          >
            <i class="fas fa-arrow-left me-2"></i>
            Retour à l'exercice
          </button>
        </div>

        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Chargement...</span>
          </div>
          <p class="mt-3">Chargement de l'exercice...</p>
        </div>

        <div v-else-if="error" class="alert alert-danger">
          <h5><i class="fas fa-exclamation-triangle me-2"></i>Erreur</h5>
          <p>{{ error }}</p>
          <button class="btn btn-primary" @click="goBack">Retour</button>
        </div>

        <div v-else>
          <!-- Exercise Info -->
          <div class="card shadow-lg mb-4">
            <div class="card-header bg-primary text-white">
              <h5 class="mb-0">
                <i class="fas fa-book me-2"></i>
                {{ exercise.title }}
              </h5>
            </div>
            <div class="card-body">
              <p><strong>Difficulté:</strong> {{ getDifficultyLabel(exercise.difficulty) }}</p>
              <p><strong>Sujet:</strong> {{ exercise.topic }}</p>
            </div>
          </div>

          <!-- Code Display (Read-only) -->
          <div class="card shadow-lg mb-4">
            <div class="card-header bg-info text-white">
              <h5 class="mb-0">
                <i class="fas fa-code me-2"></i>
                Code à tester
              </h5>
            </div>
            <div class="card-body p-0">
              <CodeEditor
                :model-value="savedCode"
                :language="'java'"
                title="Code enregistré"
                height="300px"
                :readonly="true"
              />
            </div>
          </div>

          <!-- Test Execution -->
          <div class="card shadow-lg mb-4">
            <div class="card-body p-4 text-center">
              <button
                class="btn btn-success btn-lg px-5"
                @click="runTests"
                :disabled="isRunningTests"
                type="button"
              >
                <span v-if="isRunningTests">
                  <span class="spinner-border spinner-border-sm me-2"></span>
                  Exécution des tests en cours...
                </span>
                <span v-else>
                  <i class="fas fa-play me-2"></i>
                  Exécuter les Tests
                </span>
              </button>
            </div>
          </div>

          <!-- Test Results -->
          <div v-if="testResults" class="card shadow-lg">
            <div class="card-header text-white" :class="testResults.allTestsPassed ? 'bg-success' : 'bg-danger'">
              <h5 class="mb-0">
                <i :class="testResults.allTestsPassed ? 'fas fa-check-circle' : 'fas fa-times-circle'" class="me-2"></i>
                Résultats des Tests
              </h5>
            </div>
            <div class="card-body">
              <div v-if="testResults.compilationError" class="alert alert-danger">
                <h6 class="fw-bold">
                  <i class="fas fa-exclamation-triangle me-2"></i>
                  Erreur de Compilation
                </h6>
                <pre class="mb-0">{{ testResults.compilationError }}</pre>
              </div>

              <div v-else>
                <div class="test-summary mb-4">
                  <div class="row text-center">
                    <div class="col-4">
                      <div class="stat-box">
                        <h3 class="fw-bold text-primary">{{ testResults.totalTests }}</h3>
                        <p class="text-muted mb-0">Tests totaux</p>
                      </div>
                    </div>
                    <div class="col-4">
                      <div class="stat-box">
                        <h3 class="fw-bold text-success">{{ testResults.passedTests }}</h3>
                        <p class="text-muted mb-0">Tests réussis</p>
                      </div>
                    </div>
                    <div class="col-4">
                      <div class="stat-box">
                        <h3 class="fw-bold text-danger">{{ testResults.failedTests }}</h3>
                        <p class="text-muted mb-0">Tests échoués</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="testResults.testResults && testResults.testResults.length > 0">
                  <h6 class="mb-3">Détails des tests:</h6>
                  <div
                    v-for="(test, index) in testResults.testResults"
                    :key="index"
                    class="test-item mb-3"
                    :class="test.passed ? 'test-passed' : 'test-failed'"
                  >
                    <div class="test-header">
                      <i :class="test.passed ? 'fas fa-check-circle text-success' : 'fas fa-times-circle text-danger'" class="me-2"></i>
                      <strong>{{ test.testName }}</strong>
                    </div>
                    <div v-if="test.message" class="error-message">
                      {{ test.message }}
                    </div>
                    <div v-if="test.hint && !test.passed" class="hint-box mt-2">
                      <strong><i class="fas fa-lightbulb me-2"></i>Indice:</strong>
                      <p class="mb-0">{{ test.hint }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CodeEditor from '../components/CodeEditor.vue'
import llmApi from '../services/llmApi'
import api from '../services/api'

const route = useRoute()
const router = useRouter()

const exercise = ref({
  id: null,
  title: '',
  topic: '',
  difficulty: '',
  problemStatement: '',
  description: '',
  examples: '',
  concepts: '',
  starterCode: '',
  solution: '',
  unitTests: '',
  isPublished: false
})

const savedCode = ref('')
const testResults = ref(null)
const isRunningTests = ref(false)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  const exerciseId = route.params.id
  console.log('ExerciseTest mounted, exerciseId:', exerciseId)
  
  if (!exerciseId) {
    error.value = 'ID d\'exercice manquant'
    loading.value = false
    return
  }
  
  loading.value = true
  error.value = null

  try {
    // Load exercise
    console.log('Loading exercise:', exerciseId)
    const response = await api.get(`/exercises/${exerciseId}`)
    console.log('Exercise response:', response.data)
    
    if (response.data) {
      exercise.value = {
        id: response.data.id,
        title: response.data.title || 'Exercice sans titre',
        topic: response.data.topic || '',
        difficulty: response.data.difficulty || '',
        problemStatement: response.data.problemStatement || response.data.description || '',
        description: response.data.description || response.data.problemStatement || '',
        examples: response.data.examples || '',
        concepts: response.data.concepts || '',
        starterCode: response.data.starterCode || '',
        solution: response.data.solution || '',
        unitTests: response.data.unitTests || '',
        isPublished: response.data.published || response.data.isPublished || false
      }
      console.log('Exercise loaded:', exercise.value)
    } else {
      throw new Error('Données d\'exercice vides')
    }

    // Load saved code from localStorage
    const storageKey = `exercise_${exerciseId}_code`
    const saved = localStorage.getItem(storageKey)
    if (saved && saved.trim()) {
      savedCode.value = saved
      console.log('Loaded code from localStorage, length:', saved.length)
    } else {
      savedCode.value = exercise.value.starterCode || ''
      console.log('Using starterCode, length:', savedCode.value.length)
    }
  } catch (err) {
    console.error('Error loading exercise:', err)
    const errorMessage = err.response?.data?.message || err.response?.data || err.message || 'Erreur inconnue'
    error.value = errorMessage
  } finally {
    loading.value = false
    console.log('Loading complete, loading:', loading.value, 'error:', error.value)
  }
})

const runTests = async () => {
  isRunningTests.value = true
  testResults.value = null

  try {
    const response = await llmApi.executeTests(
      exercise.value.id,
      savedCode.value,
      'Java'
    )
    testResults.value = response.data

    if (response.data && response.data.allTestsPassed) {
      setTimeout(() => {
        alert('🎉 Félicitations ! Tous les tests sont passés !')
      }, 500)
    }
  } catch (error) {
    console.error('Error running tests:', error)
    const errorMsg = error.response?.data?.message || error.message || 'Erreur inconnue'
    alert('Erreur lors de l\'exécution des tests: ' + errorMsg)
  } finally {
    isRunningTests.value = false
  }
}

const goBack = () => {
  console.log('goBack called, exercise ID:', exercise.value.id)
  const targetRoute = `/exercises/${exercise.value.id}`
  console.log('Navigating to:', targetRoute)
  
  // Try router.push first, fallback to window.location if it fails
  try {
    router.push(targetRoute).catch((err) => {
      console.error('Router push failed, using window.location:', err)
      window.location.href = targetRoute
    })
  } catch (e) {
    console.error('Navigation error, using window.location:', e)
    window.location.href = targetRoute
  }
}

const getDifficultyLabel = (difficulty) => {
  const labels = {
    'L1': 'L1 - Licence 1',
    'L2': 'L2 - Licence 2',
    'L3': 'L3 - Licence 3',
    'M1': 'M1 - Master 1',
    'M2': 'M2 - Master 2'
  }
  return labels[difficulty] || difficulty
}
</script>

<style lang="scss" scoped>
.test-summary {
  background: #f8f9fa;
  border-radius: 0.75rem;
  padding: 1.5rem;
}

.stat-box {
  h3 {
    font-size: 2.5rem;
    margin-bottom: 0.5rem;
  }
}

.test-item {
  border: 2px solid #dee2e6;
  border-radius: 0.5rem;
  padding: 1rem;
  margin-bottom: 1rem;
  transition: all 0.3s ease;

  &.test-passed {
    border-color: #28a745;
    background: rgba(40, 167, 69, 0.05);
  }

  &.test-failed {
    border-color: #dc3545;
    background: rgba(220, 53, 69, 0.05);
  }
}

.test-header {
  display: flex;
  align-items: center;
  font-size: 1.05rem;
}

.error-message {
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 0.5rem;
  padding: 0.75rem;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.hint-box {
  background: linear-gradient(135deg, #fff8e1 0%, #ffe0b2 100%);
  border: 2px solid #ffc107;
  border-radius: 0.75rem;
  padding: 1rem;

  strong {
    color: #f57c00;
  }

  p {
    color: #555;
    line-height: 1.6;
  }
}
</style>

