<template>
  <div class="auth-container">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-5">
          <div class="auth-card">
            <div class="auth-header">
              <div class="auth-icon">🧠</div>
              <h2>AICodeMentor</h2>
              <p>Connectez-vous à votre compte</p>
            </div>

            <form @submit.prevent="handleLogin">
              <div class="mb-3">
                <label for="username" class="form-label">Nom d'utilisateur ou Email</label>
                <input 
                  type="text" 
                  class="form-control" 
                  id="username" 
                  v-model="loginForm.username"
                  :class="{ 'is-invalid': errors.username }"
                  placeholder="Entrez votre nom d'utilisateur ou email"
                  required
                >
                <div v-if="errors.username" class="invalid-feedback">
                  {{ errors.username }}
                </div>
              </div>

              <div class="mb-3">
                <label for="password" class="form-label">Mot de passe</label>
                <div class="input-group">
                  <input 
                    :type="showPassword ? 'text' : 'password'" 
                    class="form-control" 
                    id="password" 
                    v-model="loginForm.password"
                    :class="{ 'is-invalid': errors.password }"
                    placeholder="Entrez votre mot de passe"
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

              <div class="mb-3 form-check">
                <input 
                  type="checkbox" 
                  class="form-check-input" 
                  id="rememberMe"
                  v-model="loginForm.rememberMe"
                >
                <label class="form-check-label" for="rememberMe">
                  Se souvenir de moi
                </label>
              </div>

              <div v-if="errorMessage" class="alert alert-danger">
                {{ errorMessage }}
              </div>

              <button 
                type="submit" 
                class="btn btn-primary w-100 mb-3"
                :disabled="isLoading"
              >
                <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
                {{ isLoading ? 'Connexion...' : 'Se connecter' }}
              </button>

              <div class="auth-links">
                <router-link to="/register">Pas encore de compte ? Créer un compte</router-link>
                <router-link to="/forgot-password">Mot de passe oublié ?</router-link>
              </div>
            </form>
          </div>

          <!-- Demo Account Info -->
          <div class="demo-card">
            <h6>Comptes de démonstration</h6>
            <div class="row">
              <div class="col-6">
                <small class="text-muted d-block">Enseignant</small>
                <small class="fw-bold">teacher@demo.com</small>
              </div>
              <div class="col-6">
                <small class="text-muted d-block">Étudiant</small>
                <small class="fw-bold">student@demo.com</small>
              </div>
            </div>
            <div class="text-center mt-2">
              <small class="text-muted">Mot de passe: demo123</small>
            </div>
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
  name: 'Login',
  setup() {
    const router = useRouter()
    const loginForm = ref({
      username: '',
      password: '',
      rememberMe: false
    })
    const errors = ref({})
    const errorMessage = ref('')
    const isLoading = ref(false)
    const showPassword = ref(false)

    const handleLogin = async () => {
      errors.value = {}
      errorMessage.value = ''
      
      if (!loginForm.value.username) {
        errors.value.username = 'Le nom d\'utilisateur ou email est requis'
        return
      }
      
      if (!loginForm.value.password) {
        errors.value.password = 'Le mot de passe est requis'
        return
      }

      isLoading.value = true

      try {
        // Simulate login
        await simulateLogin()
      } finally {
        isLoading.value = false
      }
    }

    const simulateLogin = () => {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          const { username, password } = loginForm.value
          
          // Comptes de démonstration
          const demoUsers = {
            'teacher@demo.com': {
              id: 1,
              username: 'teacher',
              email: 'teacher@demo.com',
              role: 'TEACHER',
              name: 'Prof. Demo',
              password: 'demo123'
            },
            'student@demo.com': {
              id: 2,
              username: 'student',
              email: 'student@demo.com',
              role: 'STUDENT',
              name: 'Étudiant Demo',
              password: 'demo123'
            }
          }

          // Vérifier les comptes de démonstration
          for (const [email, user] of Object.entries(demoUsers)) {
            if ((username === email || username === user.username) && password === user.password) {
              const userData = { ...user }
              delete userData.password // Ne pas stocker le mot de passe
              localStorage.setItem('authToken', 'demo_token_' + Date.now())
              localStorage.setItem('user', JSON.stringify(userData))
              router.push('/')
              resolve()
              return
            }
          }

          // Vérifier les utilisateurs enregistrés
          const registeredUsers = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
          const foundUser = registeredUsers.find(u => 
            (u.email === username || u.username === username) && u.password === password
          )

          if (foundUser) {
            const userData = {
              id: foundUser.id,
              username: foundUser.username,
              email: foundUser.email,
              role: foundUser.role,
              name: foundUser.fullName || foundUser.name
            }
            localStorage.setItem('authToken', 'user_token_' + Date.now())
            localStorage.setItem('user', JSON.stringify(userData))
            router.push('/')
            resolve()
          } else {
            errorMessage.value = 'Nom d\'utilisateur ou mot de passe incorrect'
            reject()
          }
        }, 1000)
      })
    }

    return {
      loginForm,
      errors,
      errorMessage,
      isLoading,
      showPassword,
      handleLogin
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
  margin-bottom: 0.5rem;
}

.auth-header p {
  color: #6c757d;
  margin-bottom: 0;
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
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  text-align: center;
}

.auth-links a {
  color: #667eea;
  text-decoration: none;
  font-size: 0.9rem;
}

.auth-links a:hover {
  text-decoration: underline;
}

.demo-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1rem;
  padding: 1.5rem;
  margin-top: 1rem;
  text-align: center;
}

.demo-card h6 {
  margin-bottom: 1rem;
  font-weight: 600;
}
</style>

