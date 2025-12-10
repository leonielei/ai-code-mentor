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

              <!-- Solution - Only visible to teachers -->
              <div v-if="exercise.solution && isTeacher" class="mb-4">
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
                ref="codeEditorRef"
                v-model="studentCode"
                language="java"
                title="Éditeur de Code"
                height="600px"
                :readonly="false"
              />
            </div>
          </div>

          <div class="card shadow-lg mb-4">
            <div class="card-body p-4">
              <div class="d-flex gap-3">
                <button
                  class="btn btn-primary btn-lg flex-fill"
                  @click.prevent="saveCode"
                  :disabled="isSaving"
                  type="button"
                >
                  <span v-if="isSaving">
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Enregistrement...
                  </span>
                  <span v-else>
                    <i class="fas fa-save me-2"></i>
                    Enregistrer le Code
                  </span>
                </button>
                <button
                  class="btn btn-warning btn-lg flex-fill"
                  @click.prevent="getHint"
                  :disabled="isGettingHint"
                  type="button"
                >
                  <span v-if="isGettingHint">
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Génération de l'indice...
                  </span>
                  <span v-else>
                    <i class="fas fa-lightbulb me-2"></i>
                    Obtenir un Indice (Astuce)
                  </span>
                </button>
                <button
                  class="btn btn-success btn-lg flex-fill"
                  @click.prevent="goToTestPage"
                  type="button"
                >
                  <i class="fas fa-play me-2"></i>
                  Exécuter les Tests
                </button>
                <!-- Submit button - Only visible to students -->
                <button
                  v-if="isStudent"
                  class="btn btn-info btn-lg flex-fill"
                  @click.prevent="submitExercise"
                  :disabled="isSubmitting"
                  type="button"
                >
                  <span v-if="isSubmitting">
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Soumission...
                  </span>
                  <span v-else>
                    <i class="fas fa-paper-plane me-2"></i>
                    Soumettre
                  </span>
                </button>
              </div>
            </div>
          </div>

          <!-- Hint Display - Using simple card instead of modal to avoid freezing -->
          <div 
            v-if="currentHint"
            class="card shadow-lg mb-4 border-warning"
            style="border-width: 3px !important;"
          >
            <div class="card-header bg-warning text-dark d-flex justify-content-between align-items-center">
              <h5 class="mb-0">
                <i class="fas fa-lightbulb me-2"></i>
                Indice IA
              </h5>
              <button
                type="button"
                class="btn btn-sm btn-outline-dark"
                @click="closeHint"
                aria-label="Close"
              >
                <i class="fas fa-times"></i>
              </button>
            </div>
            <div class="card-body">
              <div class="hint-content">
                <p class="mb-0" style="white-space: pre-wrap; line-height: 1.8;">{{ currentHint }}</p>
              </div>
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
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CodeEditor from '../components/CodeEditor.vue'
import llmApi from '../services/llmApi'
import api, { submissionAPI } from '../services/api'

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
  unitTests: '', // Add unit tests field
  isPublished: false // Add published status
})

const showSolution = ref(false)
const isPublishing = ref(false)

const studentCode = ref('')
const testResults = ref(null)
const isRunningTests = ref(false)
const loading = ref(true)
const error = ref(null)
const currentHint = ref(null)
const isGettingHint = ref(false)
const isSaving = ref(false)
const isSubmitting = ref(false)
const codeEditorRef = ref(null)

// Check user role
const user = ref(null)
const isTeacher = ref(false)
const isStudent = ref(false)

onMounted(() => {
  // Check user role
  const userData = localStorage.getItem('user')
  if (userData) {
    try {
      user.value = JSON.parse(userData)
      isTeacher.value = user.value.role === 'TEACHER'
      isStudent.value = user.value.role === 'STUDENT'
    } catch (e) {
      console.warn('Error parsing user data:', e)
    }
  }
  
  const exerciseId = route.params.id
  if (!exerciseId) {
    error.value = 'ID d\'exercice manquant'
    loading.value = false
    return
  }
  
  loading.value = true
  error.value = null
  
  console.log('Loading exercise:', exerciseId)
  
  // Load exercise asynchronously - don't block
  api.get(`/exercises/${exerciseId}`)
    .then(response => {
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
        
        // Set code immediately (synchronous) - this will trigger modelValue update
        const starter = exercise.value.starterCode || ''
        const storageKey = `exercise_${exercise.value.id}_code`
        const saved = localStorage.getItem(storageKey)
        studentCode.value = (saved && saved.trim()) ? saved : starter
        
        // Mark loading complete immediately - CRITICAL
        loading.value = false
        console.log('Exercise loaded, loading set to false')
        
        // Load from DB in background (optional)
        if (!saved) {
          setTimeout(() => {
            loadSavedCodeFromDB(exercise.value.id, starter).catch(() => {})
          }, 1000)
        }
      } else {
        throw new Error('Exercise data is empty')
      }
    })
    .catch(err => {
      console.error('Error loading exercise:', err)
      error.value = err.response?.data?.message || err.response?.data || err.message || 'Erreur inconnue'
      loading.value = false
      console.log('Error set, loading set to false')
    })
})

/**
 * 从数据库加载已保存的代码（完全可选，非阻塞）
 */
const loadSavedCodeFromDB = async (exerciseId, starterCode = '') => {
  try {
    const userData = localStorage.getItem('user')
    if (!userData || !exerciseId) return
    
    const user = JSON.parse(userData)
    if (!user || !user.id) return
    
    // Very short timeout (1 second)
    const timeoutPromise = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('Timeout')), 1000)
    )
    
    const { userAPI } = await import('../services/api')
    await Promise.race([userAPI.getUser(user.id), timeoutPromise])
    
    const existingSubmissions = await Promise.race([
      submissionAPI.getSubmissionsByUserAndExercise(user.id, exerciseId),
      timeoutPromise
    ])
    
    if (existingSubmissions.data?.length > 0) {
      const latestSubmission = existingSubmissions.data[0]
      if (latestSubmission.code && latestSubmission.code.trim()) {
        studentCode.value = latestSubmission.code
        const storageKey = `exercise_${exerciseId}_code`
        localStorage.setItem(storageKey, latestSubmission.code)
        if (codeEditorRef.value?.setValue) {
          codeEditorRef.value.setValue(latestSubmission.code)
        }
        console.log('✅ Loaded from DB, length:', latestSubmission.code.length)
      }
    }
  } catch (error) {
    // Silently fail - we already have starterCode
  }
}

// DISABLED watch to prevent freezing - editor will be updated via setValue calls only
// watch(() => codeEditorRef.value, ...) - DISABLED

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

const saveCode = () => {
  console.log('saveCode called')
  
  // Get code from editor using Monaco API (the correct way)
  let currentCode = ''
  if (codeEditorRef.value && typeof codeEditorRef.value.getValue === 'function') {
    try {
      currentCode = codeEditorRef.value.getValue() || ''
      console.log('Got code from editor, length:', currentCode.length)
    } catch (e) {
      console.warn('Error getting code from editor:', e)
    }
  }
  
  // Fallback to studentCode (from v-model) if editor fails
  if (!currentCode || !currentCode.trim()) {
    currentCode = studentCode.value || ''
  }
  
  // Fallback to starterCode if still empty
  if (!currentCode || !currentCode.trim()) {
    currentCode = exercise.value.starterCode || ''
  }
  
  // Validate code
  if (!currentCode || !currentCode.trim()) {
    alert('⚠️ no code')
    return
  }
  
  // Save to localStorage immediately (synchronous, fast)
  const storageKey = `exercise_${exercise.value.id}_code`
  localStorage.setItem(storageKey, currentCode)
  studentCode.value = currentCode
  console.log('✅ Code saved to localStorage, length:', currentCode.length)
  
  // Show success immediately
  alert('✅ Code sauvegardé avec succès')
  
  // Save to database in background (completely non-blocking, don't wait)
  const userData = localStorage.getItem('user')
  if (userData && exercise.value?.id) {
    try {
      const user = JSON.parse(userData)
      if (user?.id) {
        // Use setTimeout to push to next event loop - completely non-blocking
        setTimeout(() => {
          saveToDatabase(user.id, exercise.value.id, currentCode)
            .then(() => console.log('✅ Code also saved to database'))
            .catch(() => {}) // Silently fail
        }, 0)
      }
    } catch (e) {
      // Ignore
    }
  }
}

// Separate function for database save (completely non-blocking, runs in background)
const saveToDatabase = async (userId, exerciseId, code) => {
  try {
    const { userAPI } = await import('../services/api')
    
    // Very short timeout (500ms)
    const timeoutPromise = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('Timeout')), 500)
    )
    
    // Verify user (quick check)
    await Promise.race([userAPI.getUser(userId), timeoutPromise])
    
    // Get existing submission (quick check)
    let submissionId = null
    try {
      const existingSubmissions = await Promise.race([
        submissionAPI.getSubmissionsByUserAndExercise(userId, exerciseId),
        timeoutPromise
      ])
      if (existingSubmissions.data?.length > 0) {
        submissionId = existingSubmissions.data[0].id
      }
    } catch (e) {
      // Ignore - will create new
    }
    
    const submissionData = {
      user: { id: userId },
      exercise: { id: exerciseId },
      code: code,
      status: 'PENDING'
    }
    
    if (submissionId) {
      await Promise.race([
        submissionAPI.updateSubmission(submissionId, submissionData),
        timeoutPromise
      ])
    } else {
      await Promise.race([
        submissionAPI.createSubmission(submissionData),
        timeoutPromise
      ])
    }
    
    return true
  } catch (error) {
    // Silently fail - localStorage save already succeeded
    return false
  }
}

const goToTestPage = () => {
  console.log('goToTestPage called')
  
  // Check if exercise ID is available
  if (!exercise.value || !exercise.value.id) {
    alert('Erreur: ID de l\'exercice introuvable')
    return
  }
  
  // Get code from editor using getValue()
  let currentCode = ''
  if (codeEditorRef.value && typeof codeEditorRef.value.getValue === 'function') {
    try {
      currentCode = codeEditorRef.value.getValue() || ''
      console.log('Got code from editor for test page, length:', currentCode.length)
    } catch (e) {
      console.warn('Error getting code from editor:', e)
    }
  }
  
  // Fallback to studentCode if editor read failed
  if (!currentCode || !currentCode.trim()) {
    currentCode = studentCode.value || ''
    console.log('Using studentCode for test page, length:', currentCode.length)
  }
  
  // If still empty, try to load from localStorage
  if (!currentCode || !currentCode.trim()) {
    const storageKey = `exercise_${exercise.value.id}_code`
    const saved = localStorage.getItem(storageKey)
    if (saved) {
      currentCode = saved
      console.log('Loaded code from localStorage for test page')
    } else {
      currentCode = exercise.value.starterCode || ''
      console.log('Using starterCode for test page')
    }
  }
  
  // Save to localStorage (ensure it's saved with latest code)
  const storageKey = `exercise_${exercise.value.id}_code`
  localStorage.setItem(storageKey, currentCode)
  console.log('Code saved to localStorage before navigation, length:', currentCode.length)
  
  // Navigate to test page
  const exerciseId = exercise.value.id
  const targetRoute = `/exercise/${exerciseId}/test`
  console.log('Navigating to:', targetRoute, 'exerciseId:', exerciseId)
  
  router.push(targetRoute).catch(() => {
    window.location.href = targetRoute
  })
}

const resetCode = () => {
  if (confirm('Voulez-vous vraiment réinitialiser votre code ?')) {
    const newCode = exercise.value.starterCode || ''
    if (codeEditorRef.value) {
      codeEditorRef.value.setValue(newCode)
    }
    studentCode.value = newCode
    testResults.value = null
  }
}

const runTests = async () => {
  console.log('runTests called')
  
  // Set loading state immediately
  isRunningTests.value = true
  testResults.value = null
  
  try {
    console.log('Reading code from editor...')
    // Read code directly from editor - getValue() works fine for runTests
    // (hint also uses getValue() and works, so it should work here too)
    let currentCode = ''
    try {
      if (codeEditorRef.value && typeof codeEditorRef.value.getValue === 'function') {
        console.log('Calling getValue()...')
        currentCode = codeEditorRef.value.getValue() || ''
        console.log('Got code from editor, length:', currentCode.length)
      } else {
        console.log('getValue not available, using studentCode')
        currentCode = studentCode.value || ''
      }
    } catch (e) {
      console.error('Error reading code from editor:', e)
      currentCode = studentCode.value || ''
    }
    
    if (!currentCode || !currentCode.trim()) {
      alert('Veuillez d\'abord écrire du code pour exécuter les tests.')
      isRunningTests.value = false
      return
    }
    
    studentCode.value = currentCode // Update for hint generation
    
    console.log('Calling executeTests API...')
    console.log('Exercise ID:', exercise.value.id)
    console.log('Code length:', currentCode.length)
    
    const response = await llmApi.executeTests(
      exercise.value.id,
      currentCode,
      'Java'
    )
    
    console.log('Test execution response received:', response)
    testResults.value = response.data
    
    if (response.data && response.data.allTestsPassed) {
      setTimeout(() => {
        alert('🎉 Félicitations ! Tous les tests sont passés !')
      }, 500)
    }
  } catch (error) {
    console.error('Error running tests:', error)
    console.error('Error details:', error.response)
    const errorMsg = error.response?.data?.message || error.message || 'Erreur inconnue'
    alert('Erreur lors de l\'exécution des tests: ' + errorMsg)
  } finally {
    isRunningTests.value = false
    console.log('runTests finished')
  }
}

const getHint = async () => {
  console.log('getHint called')
  
  // DO NOT call getValue() - it blocks in getHint context
  // Use studentCode directly, or load from localStorage
  let currentCode = studentCode.value || ''
  
  // If studentCode is empty, try to load from localStorage
  if (!currentCode || !currentCode.trim()) {
    const storageKey = `exercise_${exercise.value.id}_code`
    const saved = localStorage.getItem(storageKey)
    if (saved) {
      currentCode = saved
      console.log('Loaded code from localStorage, length:', currentCode.length)
    } else {
      currentCode = exercise.value.starterCode || ''
      console.log('Using starterCode, length:', currentCode.length)
    }
  } else {
    console.log('Using studentCode, length:', currentCode.length)
  }
  
  if (!currentCode || !currentCode.trim()) {
    alert('Veuillez d\'abord écrire du code pour obtenir un indice.')
    return
  }

  console.log('Starting hint generation...')
  isGettingHint.value = true
  currentHint.value = null

  try {
    // Use a generic test name and error message for general hints
    // If we have test results with failures, use the first failed test
    let testName = 'GeneralHint'
    let testCode = exercise.value.unitTests || ''
    let errorMessage = 'L\'étudiant demande un indice général pour progresser sur l\'exercice. ' +
      'Exercice: ' + (exercise.value.problemStatement || exercise.value.description || exercise.value.title || '') + '. ' +
      'Concepts requis: ' + (exercise.value.concepts || 'Non spécifié') + '.'

    // If we have test results, use the first failed test for more specific hint
    if (testResults.value && testResults.value.testResults && testResults.value.testResults.length > 0) {
      const failedTest = testResults.value.testResults.find(t => !t.passed)
      if (failedTest) {
        testName = failedTest.testName || 'GeneralHint'
        errorMessage = failedTest.message || errorMessage
      }
    }

    const response = await llmApi.getHint(
      testName,
      testCode,
      currentCode,
      errorMessage
    )

    console.log('Hint response:', response)
    
    // Process response synchronously to avoid async issues
    let hintText = ''
    try {
      if (response && response.data) {
        if (response.data.hint) {
          hintText = String(response.data.hint)
        } else if (typeof response.data === 'string') {
          hintText = response.data
        } else if (response.data.message) {
          hintText = String(response.data.message)
        }
      }
      
      // Fallback to default if no hint found
      if (!hintText || hintText.trim().length === 0) {
        hintText = 'Relisez attentivement l\'énoncé et vérifiez votre logique. Assurez-vous de bien comprendre ce qui est demandé.'
      }
      
      console.log('Setting hint, length:', hintText.length)
      
      // Set currentHint to display in card
      currentHint.value = hintText
      
      console.log('Hint set, currentHint:', currentHint.value ? 'SET' : 'NULL')
    } catch (e) {
      console.error('Error processing hint:', e)
      currentHint.value = 'Erreur lors du traitement de l\'indice. Veuillez réessayer.'
    }
  } catch (error) {
    console.error('Error getting hint:', error)
    const errorMsg = error.response?.data?.message || error.message || 'Erreur inconnue'
    // Show error in hint card instead of alert
    currentHint.value = 'Erreur lors de la génération de l\'indice: ' + errorMsg
  } finally {
    isGettingHint.value = false
    console.log('Hint generation finished, currentHint:', currentHint.value ? 'SET' : 'NULL')
  }
}

const closeHint = () => {
  currentHint.value = null
}

const submitExercise = async () => {
  console.log('submitExercise called')
  
  // Get code from editor
  let currentCode = ''
  if (codeEditorRef.value && typeof codeEditorRef.value.getValue === 'function') {
    try {
      currentCode = codeEditorRef.value.getValue() || ''
      console.log('Got code from editor for submission, length:', currentCode.length)
    } catch (e) {
      console.warn('Error getting code from editor:', e)
    }
  }
  
  // Fallback to studentCode
  if (!currentCode || !currentCode.trim()) {
    currentCode = studentCode.value || ''
  }
  
  // Fallback to starterCode
  if (!currentCode || !currentCode.trim()) {
    currentCode = exercise.value.starterCode || ''
  }
  
  // Validate code
  if (!currentCode || !currentCode.trim()) {
    alert('⚠️ 没有代码可提交！')
    return
  }
  
  // Validate user
  const userData = localStorage.getItem('user')
  if (!userData) {
    alert('⚠️ 请先登录！')
    return
  }
  
  isSubmitting.value = true
  
  try {
    const user = JSON.parse(userData)
    if (!user.id) {
      throw new Error('用户ID不存在')
    }
    
    // Save code first
    const storageKey = `exercise_${exercise.value.id}_code`
    localStorage.setItem(storageKey, currentCode)
    studentCode.value = currentCode
    
    // Create submission with SUBMITTED status
    const submissionData = {
      user: { id: user.id },
      exercise: { id: exercise.value.id },
      code: currentCode,
      status: 'SUBMITTED'
    }
    
    // Check if submission exists
    let submissionId = null
    try {
      const existingSubmissions = await submissionAPI.getSubmissionsByUserAndExercise(user.id, exercise.value.id)
      if (existingSubmissions.data?.length > 0) {
        submissionId = existingSubmissions.data[0].id
      }
    } catch (e) {
      // Ignore - will create new
    }
    
    if (submissionId) {
      await submissionAPI.updateSubmission(submissionId, submissionData)
      console.log('✅ Submission updated, ID:', submissionId)
    } else {
      const response = await submissionAPI.createSubmission(submissionData)
      console.log('✅ Submission created, ID:', response.data?.id)
    }
    
    alert('✅ Exercice soumis avec succès !\n\nVotre code a été enregistré et sera visible par votre enseignant.')
    
  } catch (error) {
    console.error('Error submitting exercise:', error)
    const errorMsg = error.response?.data?.message || error.message || 'Erreur inconnue'
    alert('Erreur lors de la soumission: ' + errorMsg)
  } finally {
    isSubmitting.value = false
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

.hint-content {
  font-size: 1.05rem;
  line-height: 1.8;
  color: #333;
  padding: 0.5rem 0;
}

.modal.show {
  display: block !important;
}

.modal-backdrop.show {
  opacity: 0.5;
}

.action-buttons {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 2px solid #dee2e6;
}

/* Custom modal styles to avoid Bootstrap conflicts and ensure visibility */
.hint-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 9999 !important;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.hint-modal-content {
  background: white;
  border-radius: 0.75rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 10000 !important;
}

.hint-modal-header {
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
  color: #333;
  padding: 1.25rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid rgba(0, 0, 0, 0.1);
}

.hint-modal-title {
  margin: 0;
  font-weight: 700;
  font-size: 1.25rem;
}

.hint-modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #333;
  cursor: pointer;
  padding: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.25rem;
  transition: background-color 0.2s;
}

.hint-modal-close:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.hint-modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.hint-modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
}

.hint-content {
  font-size: 1.05rem;
  line-height: 1.8;
  color: #333;
  padding: 0.5rem 0;
}
</style>
