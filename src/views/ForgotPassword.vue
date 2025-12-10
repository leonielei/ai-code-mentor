<template>
  <div class="auth-container">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-md-5">
          <div class="auth-card">
            <div class="auth-header">
              <div class="auth-icon">🔑</div>
              <h2>Mot de passe oublié</h2>
              <p>Entrez votre email pour réinitialiser</p>
            </div>

            <form @submit.prevent="handleSubmit">
              <div class="mb-3">
                <label class="form-label">Adresse email</label>
                <input 
                  type="email" 
                  class="form-control" 
                  v-model="email"
                  required
                >
              </div>

              <button type="submit" class="btn btn-primary w-100">
                Envoyer le lien
              </button>

              <div class="auth-links mt-3">
                <router-link to="/login">Retour à la connexion</router-link>
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
  name: 'ForgotPassword',
  setup() {
    const router = useRouter()
    const email = ref('')

    const handleSubmit = () => {
      alert('Email envoyé!')
      router.push('/login')
    }

    return { email, handleSubmit }
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

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 0.75rem;
  font-weight: 600;
}

.auth-links {
  text-align: center;
}

.auth-links a {
  color: #667eea;
  text-decoration: none;
}
</style>




















