<template>
  <div class="auth-container">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-6">
          <div class="auth-card">
            <div class="auth-header">
              <div class="auth-icon">🧠</div>
              <h2>Créer un compte</h2>
              <p>Rejoignez AICodeMentor</p>
            </div>

            <form @submit.prevent="handleRegister">
              <div class="mb-3">
                <label class="form-label">Nom complet</label>
                <input 
                  type="text" 
                  class="form-control" 
                  v-model="form.fullName"
                  :class="{ 'is-invalid': errors.fullName }"
                  required
                >
                <div v-if="errors.fullName" class="invalid-feedback">
                  {{ errors.fullName }}
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">Nom d'utilisateur</label>
                <input 
                  type="text" 
                  class="form-control" 
                  v-model="form.username"
                  :class="{ 'is-invalid': errors.username }"
                  required
                >
                <div v-if="errors.username" class="invalid-feedback">
                  {{ errors.username }}
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">Email</label>
                <input 
                  type="email" 
                  class="form-control" 
                  v-model="form.email"
                  :class="{ 'is-invalid': errors.email }"
                  required
                >
                <div v-if="errors.email" class="invalid-feedback">
                  {{ errors.email }}
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">Mot de passe</label>
                <div class="input-group">
                  <input 
                    :type="showPassword ? 'text' : 'password'" 
                    class="form-control" 
                    v-model="form.password"
                    :class="{ 'is-invalid': errors.password }"
                    required
                  >
                  <button 
                    class="btn btn-outline-secondary" 
                    type="button" 
                    @click="showPassword = !showPassword"
                  >
                    <span v-if="showPassword">👁️</span>
                    <span v-else>🙈</span>
                  </button>
                </div>
                <div v-if="errors.password" class="invalid-feedback d-block">
                  {{ errors.password }}
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">Type de compte</label>
                <div class="role-selection">
                  <div 
                    class="role-card" 
                    :class="{ 'selected': form.role === 'STUDENT' }"
                    @click="form.role = 'STUDENT'"
                  >
                    <div class="role-icon">👨‍🎓</div>
                    <div class="role-title">Étudiant</div>
                  </div>
                  <div 
                    class="role-card" 
                    :class="{ 'selected': form.role === 'TEACHER' }"
                    @click="form.role = 'TEACHER'"
                  >
                    <div class="role-icon">👨‍🏫</div>
                    <div class="role-title">Enseignant</div>
                  </div>
                </div>
              </div>

              <div v-if="errorMessage" class="alert alert-danger">
                {{ errorMessage }}
              </div>

              <div v-if="successMessage" class="alert alert-success">
                {{ successMessage }}
              </div>

              <button 
                type="submit" 
                class="btn btn-primary w-100"
                :disabled="isLoading"
              >
                <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
                {{ isLoading ? 'Création...' : 'Créer le compte' }}
              </button>

              <div class="auth-links mt-3">
                <router-link to="/login">Déjà un compte ? Se connecter</router-link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const form = ref({
      fullName: '',
      username: '',
      email: '',
      password: '',
      role: 'STUDENT'
    })
    const errors = ref({})
    const errorMessage = ref('')
    const successMessage = ref('')
    const isLoading = ref(false)
    const showPassword = ref(false)

    const handleRegister = async () => {
      errors.value = {}
      errorMessage.value = ''
      successMessage.value = ''

      // Validation
      if (!form.value.fullName || form.value.fullName.length < 3) {
        errors.value.fullName = 'Le nom complet doit contenir au moins 3 caractères'
        return
      }

      if (!form.value.username || form.value.username.length < 3) {
        errors.value.username = 'Le nom d\'utilisateur doit contenir au moins 3 caractères'
        return
      }

      if (!form.value.email || !isValidEmail(form.value.email)) {
        errors.value.email = 'Veuillez entrer une adresse email valide'
        return
      }

      if (!form.value.password || form.value.password.length < 6) {
        errors.value.password = 'Le mot de passe doit contenir au moins 6 caractères'
        return
      }

      isLoading.value = true

      try {
        // Sauvegarder l'utilisateur dans localStorage
        const users = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
        
        // Vérifier si l'utilisateur existe déjà
        const existingUser = users.find(u => 
          u.email === form.value.email || u.username === form.value.username
        )

        if (existingUser) {
          errorMessage.value = 'Cet email ou nom d\'utilisateur est déjà utilisé'
          isLoading.value = false
          return
        }

        // Créer le nouvel utilisateur
        const newUser = {
          id: Date.now(),
          fullName: form.value.fullName,
          username: form.value.username,
          email: form.value.email,
          password: form.value.password,
          role: form.value.role,
          name: form.value.fullName,
          createdAt: new Date().toISOString()
        }

        users.push(newUser)
        localStorage.setItem('registeredUsers', JSON.stringify(users))

        successMessage.value = 'Compte créé avec succès! Redirection...'
        
        setTimeout(() => {
          router.push('/login')
        }, 1500)

      } catch (error) {
        errorMessage.value = 'Erreur lors de la création du compte'
      } finally {
        isLoading.value = false
      }
    }

    const isValidEmail = (email) => {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      return emailRegex.test(email)
    }

    return { 
      form, 
      errors,
      errorMessage,
      successMessage,
      isLoading,
      showPassword,
      handleRegister 
    }
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  padding: 2rem 0;
}

.auth-card {
  background: white;
  border-radius: 1rem;
  padding: 2.5rem;
  box-shadow: 0 1rem 3rem rgba(0, 0, 0, 0.175);
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.auth-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.auth-header h2 {
  color: #667eea;
  font-weight: 700;
}

.role-selection {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.role-card {
  border: 2px solid #e9ecef;
  border-radius: 0.75rem;
  padding: 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.role-card:hover {
  border-color: #667eea;
  background: #f8f9fa;
}

.role-card.selected {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.role-icon {
  font-size: 3rem;
  margin-bottom: 0.5rem;
}

.role-title {
  font-weight: 600;
  color: #212529;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 0.75rem;
  font-weight: 600;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

.auth-links {
  text-align: center;
}

.auth-links a {
  color: #667eea;
  text-decoration: none;
}

.auth-links a:hover {
  text-decoration: underline;
}
</style>
