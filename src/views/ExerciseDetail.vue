<template>
  <div class="exercise-detail-page">
    <div v-if="loading" class="container-fluid py-5">
      <div class="text-center">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Chargement...</span>
        </div>
        <p class="mt-3">Chargement de l'exercice...</p>
      </div>
    </div>
    <div v-else-if="error" class="container-fluid py-5">
      <div class="alert alert-danger">
        {{ error }}
        <div class="mt-2">
          <button class="btn btn-sm btn-outline-primary" @click="goToHome">Retour à l'accueil</button>
        </div>
      </div>
    </div>
    <div v-else class="container-fluid py-4">
      <div class="row">
        <!-- Left Panel: Exercise Description -->
        <div class="col-md-5">
          <div class="card shadow-lg sticky-top" style="top: 20px;">
            <div class="card-header bg-gradient-primary text-white">
              <h4 class="mb-0">
                <i class="fas fa-book me-2"></i>
                {{ exercise.title }}
              </h4>
            </div>
            <div class="card-body exercise-content">
              <div class="mb-4">
                <div class="d-flex gap-2 mb-3">
                  <span class="badge bg-info">{{ exercise.topic || 'Non classé' }}</span>
                  <span class="badge bg-warning text-dark">{{ getDifficultyLabel(exercise.difficulty) || 'Inconnu' }}</span>
                  <span v-if="exercise.isPublished" class="badge bg-success">Publié</span>
                  <span v-else class="badge bg-secondary">Non publié</span>
                </div>
              </div>

              <div class="mb-4">
                <h5 class="fw-bold">
                  <i class="fas fa-file-alt me-2"></i>
                  Description
                </h5>
                <p v-if="exercise.problemStatement || exercise.description" class="exercise-text">
                  {{ exercise.problemStatement || exercise.description }}
                </p>
                <p v-else class="text-muted">
                  Aucune description disponible pour cet exercice.
                </p>
              </div>

              <div v-if="exercise.examples" class="mb-4">
                <h5 class="fw-bold">
                  <i class="fas fa-lightbulb me-2"></i>
                  Exemples
                </h5>
                <pre class="examples-box">{{ exercise.examples }}</pre>
              </div>

              <div v-if="exercise.concepts" class="mb-4">
                <h5 class="fw-bold">
                  <i class="fas fa-tags me-2"></i>
                  Concepts Requis
                </h5>
                <div class="d-flex flex-wrap gap-2">
                  <span
                    v-for="(concept, index) in exercise.concepts.split(',')"
                    :key="index"
                    class="badge bg-secondary"
                  >
                    {{ concept.trim() }}
                  </span>
                </div>
              </div>

              <div v-if="exercise.solution" class="mb-4">
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <h5 class="fw-bold mb-0">
                    <i class="fas fa-check-circle me-2"></i>
                    Solution
                  </h5>
                  <button 
                    class="btn btn-sm"
                    :class="showSolution ? 'btn-outline-danger' : 'btn-outline-success'"
                    @click="showSolution = !showSolution"
                  >
                    <i :class="showSolution ? 'fas fa-eye-slash' : 'fas fa-eye'" class="me-1"></i>
                    {{ showSolution ? 'Masquer' : 'Afficher' }}
                  </button>
                </div>
                <div v-if="showSolution" class="solution-box">
                  <pre class="solution-code">{{ exercise.solution }}</pre>
                </div>
                <div v-else class="text-muted">
                  <small>Cliquez sur "Afficher" pour voir la solution</small>
                </div>
              </div>

              <div class="action-buttons d-flex gap-2">
                <button 
                  v-if="!exercise.isPublished" 
                  class="btn btn-success" 
                  @click="publishExercise"
                  :disabled="isPublishing"
                >
                  <span v-if="isPublishing">
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Publication...
                  </span>
                  <span v-else>
                    <i class="fas fa-upload me-2"></i>
                    Publier
                  </span>
                </button>
                <button class="btn btn-outline-primary" @click="goToHome">
                  <i class="fas fa-home me-2"></i>
                  Retour à l'accueil
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Panel: Code Editor and Tests -->
        <div class="col-md-7">
          <div class="card shadow-lg mb-4">
            <div class="card-header bg-gradient-primary text-white d-flex justify-content-between align-items-center">
              <h5 class="mb-0">
                <i class="fas fa-code me-2"></i>
                Votre Solution
              </h5>
              <button
                class="btn btn-light btn-sm"
                @click="resetCode"
              >
                <i class="fas fa-undo me-1"></i>
                Réinitialiser
              </button>
            </div>
            <div class="card-body p-0">
              <CodeEditor
                v-model="studentCode"
                :language="'java'"
                title="Éditeur de Code"
                height="500px"
              />
            </div>
          </div>

          <div class="card shadow-lg mb-4">
            <div class="card-body p-4">
              <button
                class="btn btn-success btn-lg w-100"
                @click="runTests"
                :disabled="isRunningTests"
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
                        <p class="mb-0">Total</p>
                      </div>
                    </div>
                    <div class="col-4">
                      <div class="stat-box">
                        <h3 class="fw-bold text-success">{{ testResults.passedTests }}</h3>
                        <p class="mb-0">Réussis</p>
                      </div>
                    </div>
                    <div class="col-4">
                      <div class="stat-box">
                        <h3 class="fw-bold text-danger">{{ testResults.failedTests }}</h3>
                        <p class="mb-0">Échoués</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="test-details">
                  <h6 class="fw-bold mb-3">Détails des Tests</h6>
                  <div
                    v-for="(test, index) in testResults.testResults"
                    :key="index"
                    class="test-item"
                    :class="test.passed ? 'test-passed' : 'test-failed'"
                  >
                    <div class="test-header">
                      <i :class="test.passed ? 'fas fa-check-circle text-success' : 'fas fa-times-circle text-danger'" class="me-2"></i>
                      <strong>{{ test.testName }}</strong>
                    </div>
                    <div v-if="!test.passed" class="test-details-content">
                      <div v-if="test.message" class="mt-2">
                        <small class="text-muted">Message d'erreur:</small>
                        <pre class="error-message">{{ test.message }}</pre>
                      </div>
                      <div v-if="test.hint" class="mt-3 hint-box">
                        <div class="d-flex align-items-start">
                          <i class="fas fa-lightbulb text-warning me-2 mt-1"></i>
                          <div>
                            <strong>Indice IA:</strong>
                            <p class="mb-0 mt-1">{{ test.hint }}</p>
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
  description: '', // Also support description field
  examples: '',
  concepts: '',
  starterCode: '',
  solution: '', // Add solution field
  isPublished: false // Add published status
})

const showSolution = ref(false)
const isPublishing = ref(false)

const studentCode = ref('')
const testResults = ref(null)
const isRunningTests = ref(false)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  const exerciseId = route.params.id
  loading.value = true
  error.value = null
  
  try {
    console.log('Loading exercise with ID:', exerciseId)
    const response = await api.get(`/exercises/${exerciseId}`)
    console.log('Exercise response:', response.data)
    
    if (response.data) {
      exercise.value = {
        id: response.data.id,
        title: response.data.title || '',
        topic: response.data.topic || '',
        difficulty: response.data.difficulty || '',
        problemStatement: response.data.problemStatement || response.data.description || '',
        description: response.data.description || response.data.problemStatement || '',
        examples: response.data.examples || '',
        concepts: response.data.concepts || '',
        starterCode: response.data.starterCode || '',
        solution: response.data.solution || '',
        isPublished: response.data.published || response.data.isPublished || false
      }
      studentCode.value = exercise.value.starterCode || ''
      console.log('Exercise loaded:', exercise.value)
    } else {
      throw new Error('Exercise data is empty')
    }
  } catch (error) {
    console.error('Error loading exercise:', error)
    const errorMessage = error.response?.data || error.message || 'Erreur inconnue'
    error.value = errorMessage
    alert(`Erreur lors du chargement de l'exercice: ${errorMessage}`)
    router.push('/exercises')
  } finally {
    loading.value = false
  }
})

const goToHome = () => {
  // Use window.location for reliable navigation
  window.location.href = '/'
}

const publishExercise = async () => {
  if (!confirm('Êtes-vous sûr de vouloir publier cet exercice ?')) {
    return
  }
  
  isPublishing.value = true
  try {
    // Create a clean update object with only necessary fields
    const updateData = {
      id: exercise.value.id,
      title: exercise.value.title,
      description: exercise.value.description || exercise.value.problemStatement,
      topic: exercise.value.topic,
      difficulty: exercise.value.difficulty,
      problemStatement: exercise.value.problemStatement,
      hints: exercise.value.hints || '',
      examples: exercise.value.examples || '',
      testCases: exercise.value.testCases || '',
      solution: exercise.value.solution || '',
      starterCode: exercise.value.starterCode || '',
      unitTests: exercise.value.unitTests || '',
      concepts: exercise.value.concepts || '',
      published: true,  // Backend expects "published" field name
      isPublished: true  // Also send isPublished for compatibility
    }
    
    console.log('Publishing exercise with data:', updateData)
    const response = await api.put(`/exercises/${exercise.value.id}`, updateData)
    console.log('Publish response:', response.data)
    
    if (response.data) {
      exercise.value.isPublished = response.data.published || response.data.isPublished || true
      alert('✅ Exercice publié avec succès !\n\nVous pouvez maintenant le voir dans la liste "Exercices publiés".')
    }
  } catch (error) {
    console.error('Error publishing exercise:', error)
    const errorMsg = error.response?.data?.message || error.response?.data || error.message
    alert('Erreur lors de la publication de l\'exercice: ' + errorMsg)
  } finally {
    isPublishing.value = false
  }
}

const resetCode = () => {
  if (confirm('Voulez-vous vraiment réinitialiser votre code ?')) {
    studentCode.value = exercise.value.starterCode || ''
    testResults.value = null
  }
}

const runTests = async () => {
  isRunningTests.value = true
  testResults.value = null
  
  try {
    const response = await llmApi.executeTests(
      exercise.value.id,
      studentCode.value,
      'Java'
    )
    testResults.value = response.data
    
    if (response.data.allTestsPassed) {
      setTimeout(() => {
        alert('🎉 Félicitations ! Tous les tests sont passés !')
      }, 500)
    }
  } catch (error) {
    console.error('Error running tests:', error)
    alert('Erreur lors de l\'exécution des tests')
  } finally {
    isRunningTests.value = false
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
.exercise-detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.card {
  border: none;
  border-radius: 1rem;
  overflow: hidden;
}

.card-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 2px solid rgba(255, 255, 255, 0.2);
}

.bg-gradient-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.exercise-content {
  max-height: calc(100vh - 150px);
  overflow-y: auto;
  padding: 1.5rem;
}

.exercise-text {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 1.05rem;
}

.examples-box {
  background: #f8f9fa;
  border: 2px solid #dee2e6;
  border-radius: 0.5rem;
  padding: 1rem;
  font-size: 0.95rem;
}

.solution-box {
  background: #f8f9fa;
  border: 2px solid #28a745;
  border-radius: 0.5rem;
  padding: 1rem;
  margin-top: 0.5rem;
}

.solution-code {
  background: #2d2d2d;
  color: #f8f8f2;
  border-radius: 0.5rem;
  padding: 1rem;
  font-size: 0.9rem;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
  font-family: 'Courier New', monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.badge {
  font-size: 0.9rem;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
}

.btn-success {
  background: linear-gradient(135deg, #28a745 0%, #218838 100%);
  border: none;
  font-size: 1.1rem;
  padding: 1rem;
  
  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #218838 0%, #1e7e34 100%);
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(40, 167, 69, 0.4);
  }
}

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
  border-radius: 0.75rem;
  padding: 1rem;
  margin-bottom: 1rem;
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  }
  
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

.action-buttons {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 2px solid #dee2e6;
}
</style>
