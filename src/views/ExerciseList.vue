<template>
  <div class="container py-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="mb-0">Liste des Exercices</h1>
      <!-- 只有老师才能切换查看所有练习 -->
      <div v-if="!isStudent" class="btn-group" role="group">
        <button 
          class="btn" 
          :class="showPublished ? 'btn-outline-primary' : 'btn-primary'"
          @click="showPublished = false"
        >
          Tous les exercices
        </button>
        <button 
          class="btn" 
          :class="showPublished ? 'btn-primary' : 'btn-outline-primary'"
          @click="showPublished = true"
        >
          Exercices publiés
        </button>
      </div>
      <div v-else class="badge bg-success fs-6">
        Exercices publiés
      </div>
    </div>
    
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Chargement...</span>
      </div>
      <p class="mt-3">Chargement de la liste des exercices...</p>
    </div>
    
    <div v-else-if="error" class="alert alert-danger">
      {{ error }}
      <button class="btn btn-sm btn-outline-danger ms-2" @click="loadExercises">Réessayer</button>
    </div>
    
    <div v-else-if="exercises.length === 0" class="alert alert-info">
      <p class="mb-0">
        {{ showPublished 
          ? 'Aucun exercice publié disponible pour le moment.' 
          : 'Aucun exercice disponible pour le moment. Veuillez créer des exercices.' 
        }}
      </p>
    </div>
    
    <div v-else class="row">
      <div class="col-md-4" v-for="exercise in exercises" :key="exercise.id">
        <div class="card mb-4">
          <div class="card-body">
            <h5 class="card-title">{{ exercise.title }}</h5>
            <p class="card-text">{{ exercise.description || exercise.problemStatement || 'Aucune description' }}</p>
            <div class="mb-2">
              <span class="badge bg-info me-1">{{ exercise.topic || 'Non classé' }}</span>
              <span class="badge bg-warning text-dark">{{ exercise.difficulty || 'Inconnu' }}</span>
              <span v-if="exercise.published || exercise.isPublished" class="badge bg-success ms-1">Publié</span>
            </div>
            <router-link :to="`/exercises/${exercise.id}`" class="btn btn-primary">
              Voir l'exercice
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onActivated, watch } from 'vue'
import { exerciseAPI } from '../services/api'

export default {
  name: 'ExerciseList',
  setup() {
    const exercises = ref([])
    const loading = ref(true)
    const error = ref(null)
    
    // 检查用户角色，学生默认只显示已发布的练习
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const isStudent = user.role === 'STUDENT'
    const showPublished = ref(isStudent) // 学生默认只显示已发布的练习
    
    // 如果是学生，强制只显示已发布的练习
    if (isStudent) {
      showPublished.value = true
    }

    const loadExercises = async () => {
      loading.value = true
      error.value = null
      try {
        console.log('Loading exercises...', showPublished.value ? 'published' : 'all')
        let response
        if (showPublished.value) {
          response = await exerciseAPI.getPublishedExercises(0, 100)
        } else {
          response = await exerciseAPI.getExercises({ page: 0, size: 100 })
        }
        console.log('Exercises response:', response.data)
        
        // Extract exercises from paginated response
        if (response.data && response.data.content) {
          exercises.value = response.data.content
          console.log('Loaded exercises:', exercises.value.length)
          // Debug: log published status
          exercises.value.forEach(ex => {
            console.log(`Exercise ${ex.id} (${ex.title}): published=${ex.published}, isPublished=${ex.isPublished}`)
          })
        } else if (Array.isArray(response.data)) {
          exercises.value = response.data
          console.log('Loaded exercises (array):', exercises.value.length)
        } else {
          console.warn('Unexpected response format:', response.data)
          exercises.value = []
        }
      } catch (err) {
        console.error('Error loading exercises:', err)
        let errorMessage = 'Impossible de charger la liste des exercices: '
        
        if (err.response) {
          // Server responded with error status
          errorMessage += `Erreur ${err.response.status}: ${err.response.data?.message || err.response.statusText}`
        } else if (err.request) {
          // Request was made but no response received
          errorMessage += 'Erreur réseau - Le serveur ne répond pas. Vérifiez que le backend est démarré sur le port 8080.'
        } else {
          // Something else happened
          errorMessage += err.message || 'Erreur inconnue'
        }
        
        error.value = errorMessage
        exercises.value = []
      } finally {
        loading.value = false
      }
    }

    // Watch for changes in showPublished
    watch(showPublished, () => {
      loadExercises()
    })

    onMounted(() => {
      loadExercises()
    })

    // Reload when route is activated (e.g., coming back from create page)
    onActivated(() => {
      loadExercises()
    })

    return { 
      exercises, 
      loading, 
      error,
      showPublished,
      isStudent,
      loadExercises
    }
  }
}
</script>








