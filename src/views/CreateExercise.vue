<template>
  <div class="create-exercise-page">
    <div class="container py-5">
      <div class="row">
        <div class="col-12">
          <div class="page-header mb-4">
            <h1 class="display-5 fw-bold">
              <i class="fas fa-magic me-3"></i>
              Créer un Exercice avec l'IA
            </h1>
            <p class="lead text-muted">
              Décrivez votre exercice en langage naturel et laissez l'IA générer un exercice complet
            </p>
          </div>
        </div>
      </div>

      <!-- Step 1: Natural Language Description -->
      <div v-if="step === 1" class="generation-step">
        <div class="card shadow-lg">
          <div class="card-header bg-gradient-primary text-white">
            <h4 class="mb-0">
              <i class="fas fa-edit me-2"></i>
              Étape 1: Décrire l'Exercice
            </h4>
          </div>
          <div class="card-body p-4">
            <div class="mb-4">
              <label for="description" class="form-label fw-bold">
                Description en Langage Naturel
              </label>
              <textarea
                id="description"
                v-model="naturalDescription"
                class="form-control"
                rows="6"
                placeholder="Exemple: Créer un exercice où les étudiants implémentent une fonction pour trouver le deuxième plus grand nombre dans un tableau en Java. Cet exercice s'adresse à des étudiants en L2 Informatique."
              ></textarea>
              <small class="form-text text-muted">
                Décrivez l'exercice que vous souhaitez créer, incluant le niveau et les concepts
              </small>
            </div>

            <div class="row">
              <div class="col-md-6 mb-3">
                <label for="difficulty" class="form-label fw-bold">Niveau de Difficulté</label>
                <select id="difficulty" v-model="difficulty" class="form-select">
                  <option value="L1">L1 - Licence 1</option>
                  <option value="L2">L2 - Licence 2</option>
                  <option value="L3">L3 - Licence 3</option>
                  <option value="M1">M1 - Master 1</option>
                  <option value="M2">M2 - Master 2</option>
                </select>
              </div>

              <div class="col-md-6 mb-3">
                <label for="language" class="form-label fw-bold">Langage de Programmation</label>
                <select id="language" v-model="language" class="form-select">
                  <option value="Java">Java</option>
                  <option value="Python">Python</option>
                  <option value="JavaScript">JavaScript</option>
                </select>
              </div>
            </div>

            <!-- Progress Bar -->
            <div v-if="isGenerating" class="mb-4">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted">
                  <i class="fas fa-cog fa-spin me-2"></i>
                  Génération en cours...
                </span>
                <span class="text-muted fw-bold">{{ progress }}%</span>
              </div>
              <div class="progress" style="height: 25px;">
                <div 
                  class="progress-bar progress-bar-striped progress-bar-animated bg-primary" 
                  role="progressbar" 
                  :style="{ width: progress + '%' }"
                  :aria-valuenow="progress" 
                  aria-valuemin="0" 
                  aria-valuemax="100"
                >
                  {{ progressText }}
                </div>
              </div>
            </div>

            <div class="d-flex justify-content-end">
              <button 
                class="btn btn-primary btn-lg px-5"
                @click="generateExercise"
                :disabled="isGenerating || !naturalDescription"
              >
                <span v-if="isGenerating">
                  <span class="spinner-border spinner-border-sm me-2"></span>
                  Génération en cours...
                </span>
                <span v-else>
                  <i class="fas fa-robot me-2"></i>
                  Générer l'Exercice
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 2: Review and Edit Generated Exercise -->
      <div v-if="step === 2" class="review-step">
        <div class="alert alert-success mb-4">
          <i class="fas fa-check-circle me-2"></i>
          Exercice généré avec succès ! Vous pouvez maintenant le réviser et le publier.
        </div>

        <div class="card shadow-lg mb-4">
          <div class="card-header bg-gradient-primary text-white">
            <h4 class="mb-0">
              <i class="fas fa-file-alt me-2"></i>
              Informations Générales
            </h4>
          </div>
          <div class="card-body p-4">
            <div class="mb-3">
              <label for="title" class="form-label fw-bold">Titre de l'Exercice</label>
              <input
                id="title"
                v-model="generatedExercise.title"
                type="text"
                class="form-control"
              />
            </div>

            <div class="mb-3">
              <label for="topic" class="form-label fw-bold">Sujet/Topic</label>
              <input
                id="topic"
                v-model="topic"
                type="text"
                class="form-control"
                placeholder="Ex: Tableaux, Algorithmes de tri, etc."
              />
            </div>

            <div class="mb-3">
              <label for="detailedDescription" class="form-label fw-bold">Description Détaillée</label>
              <textarea
                id="detailedDescription"
                v-model="generatedExercise.detailedDescription"
                class="form-control"
                rows="6"
              ></textarea>
            </div>

            <div class="mb-3">
              <label for="concepts" class="form-label fw-bold">Concepts Requis</label>
              <input
                id="concepts"
                v-model="generatedExercise.concepts"
                type="text"
                class="form-control"
                placeholder="Séparés par des virgules"
              />
            </div>

            <div class="mb-3">
              <label for="examples" class="form-label fw-bold">Exemples d'Utilisation</label>
              <textarea
                id="examples"
                v-model="generatedExercise.examples"
                class="form-control"
                rows="4"
              ></textarea>
            </div>
          </div>
        </div>

        <div class="card shadow-lg mb-4">
          <div class="card-header bg-gradient-primary text-white">
            <h4 class="mb-0">
              <i class="fas fa-code me-2"></i>
              Code de Départ (Starter Code)
            </h4>
          </div>
          <div class="card-body p-0">
            <CodeEditor
              v-model="generatedExercise.starterCode"
              :language="language.toLowerCase()"
              title="Code de Départ pour les Étudiants"
              height="400px"
            />
          </div>
        </div>

        <div class="card shadow-lg mb-4">
          <div class="card-header bg-gradient-primary text-white">
            <h4 class="mb-0">
              <i class="fas fa-vial me-2"></i>
              Tests Unitaires
            </h4>
          </div>
          <div class="card-body p-0">
            <CodeEditor
              v-model="generatedExercise.unitTests"
              :language="language.toLowerCase()"
              title="Tests JUnit"
              height="400px"
            />
          </div>
        </div>

        <div class="card shadow-lg mb-4">
          <div class="card-header bg-gradient-primary text-white">
            <h4 class="mb-0">
              <i class="fas fa-lightbulb me-2"></i>
              Solution Exemple
            </h4>
          </div>
          <div class="card-body p-0">
            <CodeEditor
              v-model="generatedExercise.exampleSolution"
              :language="language.toLowerCase()"
              title="Solution de Référence"
              height="400px"
            />
          </div>
        </div>

        <div class="d-flex justify-content-between">
          <button class="btn btn-outline-secondary btn-lg px-4" @click="goBack" :disabled="isSaving">
            <i class="fas fa-arrow-left me-2"></i>
            Retour
          </button>
          <div class="d-flex gap-3">
            <button class="btn btn-primary btn-lg px-4" @click="saveExercise" :disabled="isSaving">
              <span v-if="isSaving">
                <span class="spinner-border spinner-border-sm me-2"></span>
                Sauvegarde...
              </span>
              <span v-else>
                <i class="fas fa-save me-2"></i>
                Sauvegarder
              </span>
            </button>
            <button class="btn btn-success btn-lg px-5" @click="publishExercise" :disabled="isSaving || !generatedExercise.title">
              <span v-if="isSaving">
                <span class="spinner-border spinner-border-sm me-2"></span>
                Publication...
              </span>
              <span v-else>
                <i class="fas fa-paper-plane me-2"></i>
                Publier
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CodeEditor from '../components/CodeEditor.vue'
import llmApi from '../services/llmApi'

const router = useRouter()

const step = ref(1)
const naturalDescription = ref('')
const difficulty = ref('L1')
const language = ref('Java')
const topic = ref('')

const isGenerating = ref(false)
const isSaving = ref(false)
const progress = ref(0)
const progressText = ref('Initialisation...')

const generatedExercise = ref({
  title: '',
  detailedDescription: '',
  concepts: '',
  starterCode: '',
  unitTests: '',
  exampleSolution: '',
  examples: ''
})

const generateExercise = async () => {
  isGenerating.value = true
  progress.value = 0
  progressText.value = 'Initializing...'
  
  // Progress update - adapted for sequential execution (3 steps)
  let progressInterval
  const startTime = Date.now()
  const estimatedTime = 15000 // Estimate 15 seconds total (5s per step)
  
  progressInterval = setInterval(() => {
    const elapsed = Date.now() - startTime
    const estimatedProgress = Math.min(95, (elapsed / estimatedTime) * 100)
    
    // Smooth progress update
    if (progress.value < estimatedProgress) {
      progress.value = Math.min(95, progress.value + 3)
    }
    
    // Update text based on progress (sequential execution: step 1/3, 2/3, 3/3)
    if (progress.value < 30) {
      progressText.value = 'Step 1/3: Generating starter code...'
    } else if (progress.value < 65) {
      progressText.value = 'Step 2/3: Generating JUnit tests...'
    } else if (progress.value < 95) {
      progressText.value = 'Step 3/3: Generating reference solution...'
    }
  }, 200)
  
  try {
    const response = await llmApi.generateExercise(
      naturalDescription.value,
      difficulty.value,
      language.value
    )
    
    // Complete progress
    clearInterval(progressInterval)
    progress.value = 100
    progressText.value = 'Completed!'
    
    // Brief delay to show 100%
    await new Promise(resolve => setTimeout(resolve, 300))
    
    generatedExercise.value = response.data
    step.value = 2
  } catch (error) {
    clearInterval(progressInterval)
    console.error('Error generating exercise:', error)
    const errorMsg = error.response?.data?.message || error.message || 'Unknown error'
    alert(`Error during exercise generation.\n\n` +
          `Details: ${errorMsg}\n\n` +
          `Please verify:\n` +
          `1. llama.cpp server is started (port 11435)\n` +
          `2. Spring Boot backend is started (port 8080)\n\n` +
          `Run: start-all.bat`)
  } finally {
    isGenerating.value = false
    progress.value = 0
    progressText.value = 'Initializing...'
  }
}

const saveExercise = async (publish = false) => {
  isSaving.value = true
  try {
    // Validate required fields
    if (!generatedExercise.value.title || !generatedExercise.value.title.trim()) {
      alert('Le titre de l\'exercice est requis')
      isSaving.value = false
      return
    }
    if (!naturalDescription.value || !naturalDescription.value.trim()) {
      alert('La description de l\'exercice est requise')
      isSaving.value = false
      return
    }
    if (!difficulty.value) {
      alert('Le niveau de difficulté est requis')
      isSaving.value = false
      return
    }
    
    const exerciseData = {
      title: generatedExercise.value.title.trim(),
      description: naturalDescription.value.trim(),
      topic: topic.value || 'Général',
      difficulty: difficulty.value,
      problemStatement: generatedExercise.value.detailedDescription || '',
      starterCode: generatedExercise.value.starterCode || '',
      unitTests: generatedExercise.value.unitTests || '',
      solution: generatedExercise.value.exampleSolution || '',
      concepts: generatedExercise.value.concepts || '',
      examples: generatedExercise.value.examples || '',
      published: Boolean(publish)  // Ensure it's a boolean
    }
    
    console.log('Saving exercise with data:', {
      title: exerciseData.title,
      description: exerciseData.description?.substring(0, 50),
      difficulty: exerciseData.difficulty,
      published: exerciseData.published
    })
    
    const response = await llmApi.saveExercise(exerciseData)
    
    // Check if response is successful
    if (response && (response.status === 200 || response.status === 201)) {
      console.log('Exercise saved successfully:', response.data)
      alert(publish ? '✅ Exercice publié avec succès !' : '✅ Exercice sauvegardé avec succès !')
      // Small delay to ensure backend has saved the data
      await new Promise(resolve => setTimeout(resolve, 500))
      router.push('/exercises')
    } else {
      const errorMsg = response?.data || response?.data?.message || 'Erreur inconnue'
      console.error('Save exercise failed:', errorMsg)
      throw new Error(errorMsg)
    }
  } catch (error) {
    console.error('Error saving exercise:', error)
    console.error('Error details:', {
      message: error.message,
      response: error.response,
      status: error.response?.status,
      data: error.response?.data
    })
    
    let errorMessage = 'Erreur lors de la sauvegarde de l\'exercice.'
    
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      errorMessage = '⏱️ La requête a pris trop de temps. Veuillez vérifier que le serveur backend est démarré et réessayer.'
    } else if (error.response) {
      // Server responded with error
      const status = error.response.status
      const data = error.response.data
      
      if (status === 400) {
        errorMessage = '❌ Données invalides: ' + (typeof data === 'string' ? data : data?.message || 'Vérifiez que tous les champs requis sont remplis')
      } else if (status === 500) {
        errorMessage = '❌ Erreur serveur: ' + (typeof data === 'string' ? data : data?.message || 'Veuillez réessayer plus tard')
      } else {
        errorMessage = '❌ Erreur ' + status + ': ' + (typeof data === 'string' ? data : data?.message || errorMessage)
      }
    } else if (error.message) {
      errorMessage = '❌ ' + error.message
    }
    
    alert(errorMessage)
  } finally {
    isSaving.value = false
  }
}

const publishExercise = () => {
  saveExercise(true)
}

const goBack = () => {
  step.value = 1
}
</script>

<style lang="scss" scoped>
.create-exercise-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;

  h1 {
    color: #333;
    margin-bottom: 1rem;
  }

  .lead {
    font-size: 1.25rem;
  }
}

.card {
  border: none;
  border-radius: 1rem;
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15) !important;
  }
}

.card-header {
  padding: 1.5rem;
  border-bottom: 2px solid rgba(255, 255, 255, 0.2);

  h4 {
    color: white;
    margin: 0;
  }
}

.bg-gradient-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
  }

  &:disabled {
    opacity: 0.6;
  }
}

.btn-success {
  background: linear-gradient(135deg, #28a745 0%, #218838 100%);
  border: none;

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #218838 0%, #1e7e34 100%);
    transform: translateY(-2px);
  }
}

.form-control, .form-select {
  border-radius: 0.5rem;
  border: 2px solid #dee2e6;
  padding: 0.75rem 1rem;

  &:focus {
    border-color: #667eea;
    box-shadow: 0 0 0 0.25rem rgba(102, 126, 234, 0.25);
  }
}

.alert-success {
  border-radius: 0.75rem;
  border: 2px solid #28a745;
  background-color: rgba(40, 167, 69, 0.1);
}
</style>
