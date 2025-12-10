<template>
  <div class="container-fluid py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="mb-0">
        <i class="fas fa-file-alt me-2"></i>
        Soumissions des Étudiants
      </h1>
      <button
        class="btn btn-outline-secondary"
        @click="goBack"
      >
        <i class="fas fa-arrow-left me-2"></i>
        Retour
      </button>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Chargement...</span>
      </div>
      <p class="mt-3">Chargement des soumissions...</p>
    </div>

    <div v-else-if="error" class="alert alert-danger">
      <h5><i class="fas fa-exclamation-triangle me-2"></i>Erreur</h5>
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="loadSubmissions">Réessayer</button>
    </div>

    <div v-else-if="submissions.length === 0" class="alert alert-info">
      <p class="mb-0">Aucune soumission disponible pour le moment.</p>
    </div>

    <div v-else>
      <!-- Filter by exercise -->
      <div class="card shadow-lg mb-4">
        <div class="card-body">
          <div class="row align-items-end">
            <div class="col-md-6">
              <label class="form-label">Filtrer par exercice:</label>
              <select v-model="selectedExerciseId" class="form-select" @change="loadSubmissions">
                <option value="">Tous les exercices</option>
                <option v-for="ex in exercises" :key="ex.id" :value="ex.id">
                  {{ ex.title }}
                </option>
              </select>
            </div>
            <div class="col-md-6 text-end">
              <span class="badge bg-primary fs-6">
                Total: {{ submissions.length }} soumission(s)
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Submissions list -->
      <div class="row">
        <div class="col-12" v-for="submission in submissions" :key="submission.id">
          <div class="card shadow-lg mb-4">
            <div class="card-header d-flex justify-content-between align-items-center"
                 :class="getStatusClass(submission.status)">
              <div>
                <h5 class="mb-0">
                  <i class="fas fa-user me-2"></i>
                  {{ submission.user?.username || submission.user?.fullName || 'Utilisateur inconnu' }}
                </h5>
                <small class="text-white-50">
                  Exercice: {{ submission.exercise?.title || 'Exercice inconnu' }}
                </small>
              </div>
              <div class="text-end">
                <span class="badge bg-light text-dark me-2">
                  {{ formatDate(submission.createdAt) }}
                </span>
                <span class="badge bg-white text-dark">
                  {{ getStatusLabel(submission.status) }}
                </span>
              </div>
            </div>
            <div class="card-body">
              <div class="mb-3">
                <h6 class="fw-bold">
                  <i class="fas fa-code me-2"></i>
                  Code soumis:
                </h6>
                <CodeEditor
                  :model-value="submission.code || ''"
                  language="java"
                  title="Code de l'étudiant"
                  height="400px"
                  :readonly="true"
                />
              </div>
              
              <div v-if="submission.testResults" class="mt-3">
                <h6 class="fw-bold">
                  <i class="fas fa-vial me-2"></i>
                  Résultats des tests:
                </h6>
                <div class="alert" :class="submission.testResults.allTestsPassed ? 'alert-success' : 'alert-danger'">
                  <div class="row text-center mb-2">
                    <div class="col-4">
                      <strong>Total:</strong> {{ submission.testResults.totalTests || 0 }}
                    </div>
                    <div class="col-4">
                      <strong>Réussis:</strong> {{ submission.testResults.passedTests || 0 }}
                    </div>
                    <div class="col-4">
                      <strong>Échoués:</strong> {{ submission.testResults.failedTests || 0 }}
                    </div>
                  </div>
                  <div v-if="submission.testResults.compilationError" class="mt-2">
                    <strong>Erreur de compilation:</strong>
                    <pre class="mb-0">{{ submission.testResults.compilationError }}</pre>
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
import { useRouter } from 'vue-router'
import CodeEditor from '../components/CodeEditor.vue'
import { submissionAPI, exerciseAPI } from '../services/api'

const router = useRouter()

const submissions = ref([])
const exercises = ref([])
const loading = ref(true)
const error = ref(null)
const selectedExerciseId = ref('')

const loadExercises = async () => {
  try {
    const response = await exerciseAPI.getExercises({ page: 0, size: 1000 })
    if (response.data?.content) {
      exercises.value = response.data.content
    } else if (Array.isArray(response.data)) {
      exercises.value = response.data
    }
  } catch (err) {
    console.error('Error loading exercises:', err)
  }
}

const loadSubmissions = async () => {
  loading.value = true
  error.value = null
  
  try {
    let response
    if (selectedExerciseId.value) {
      response = await submissionAPI.getSubmissionsByExercise(selectedExerciseId.value, 0, 1000)
    } else {
      response = await submissionAPI.getSubmissions(0, 1000)
    }
    
    if (response.data?.content) {
      submissions.value = response.data.content
    } else if (Array.isArray(response.data)) {
      submissions.value = response.data
    } else {
      submissions.value = []
    }
    
    console.log('Loaded submissions:', submissions.value.length)
  } catch (err) {
    console.error('Error loading submissions:', err)
    error.value = err.response?.data?.message || err.message || 'Erreur inconnue'
    submissions.value = []
  } finally {
    loading.value = false
  }
}

const getStatusClass = (status) => {
  const classes = {
    'SUBMITTED': 'bg-info',
    'PENDING': 'bg-warning',
    'PASSED': 'bg-success',
    'FAILED': 'bg-danger'
  }
  return classes[status] || 'bg-secondary'
}

const getStatusLabel = (status) => {
  const labels = {
    'SUBMITTED': 'Soumis',
    'PENDING': 'En attente',
    'PASSED': 'Réussi',
    'FAILED': 'Échoué'
  }
  return labels[status] || status
}

const formatDate = (dateString) => {
  if (!dateString) return 'Date inconnue'
  try {
    const date = new Date(dateString)
    return date.toLocaleString('fr-FR')
  } catch (e) {
    return dateString
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(async () => {
  // Check if user is teacher
  const userData = localStorage.getItem('user')
  if (userData) {
    try {
      const user = JSON.parse(userData)
      if (user.role !== 'TEACHER') {
        alert('⚠️ 只有老师可以查看此页面')
        router.push('/')
        return
      }
    } catch (e) {
      console.warn('Error parsing user data:', e)
    }
  } else {
    router.push('/login')
    return
  }
  
  await loadExercises()
  await loadSubmissions()
})
</script>

<style lang="scss" scoped>
.card-header {
  color: white;
}
</style>


