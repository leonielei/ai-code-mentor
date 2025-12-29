<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-gradient-primary">
    <div class="container">
      <router-link class="navbar-brand" to="/">
        <span class="brand-icon">🧠</span>
        AICodeMentor
      </router-link>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarNav">
        <ul class="navbar-nav ms-auto">
          <li class="nav-item">
            <router-link class="nav-link" to="/">Accueil</router-link>
          </li>
          <li class="nav-item" v-if="isLoggedIn">
            <router-link class="nav-link" to="/exercises">Exercices</router-link>
          </li>
          <li class="nav-item" v-if="isLoggedIn && isTeacher">
            <router-link class="nav-link" to="/teacher/create">Créer</router-link>
          </li>
          <li class="nav-item" v-if="isLoggedIn && isTeacher">
            <router-link class="nav-link" to="/submissions">Soumissions</router-link>
          </li>
          <li class="nav-item" v-if="!isLoggedIn">
            <router-link class="nav-link" to="/login">Connexion</router-link>
          </li>
          <li class="nav-item dropdown" v-if="isLoggedIn">
            <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
              {{ user.name || user.username }}
            </a>
            <ul class="dropdown-menu dropdown-menu-end">
              <li><span class="dropdown-item-text">{{ user.email }}</span></li>
              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item" href="#" @click.prevent="logout">Se déconnecter</a></li>
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'Navbar',
  setup() {
    const router = useRouter()
    const isLoggedIn = ref(false)
    const user = ref({})
    
    const isTeacher = computed(() => user.value.role === 'TEACHER')
    
    const checkAuth = () => {
      const token = localStorage.getItem('authToken')
      const userData = localStorage.getItem('user')
      
      if (token && userData) {
        isLoggedIn.value = true
        user.value = JSON.parse(userData)
      } else {
        isLoggedIn.value = false
        user.value = {}
      }
    }
    
    const logout = () => {
      localStorage.removeItem('authToken')
      localStorage.removeItem('user')
      isLoggedIn.value = false
      user.value = {}
      router.push('/login')
    }
    
    onMounted(() => {
      checkAuth()
      // Listen for storage changes
      window.addEventListener('storage', checkAuth)
    })
    
    return {
      isLoggedIn,
      user,
      isTeacher,
      logout
    }
  }
}
</script>

<style scoped>
.bg-gradient-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.brand-icon {
  font-size: 1.5rem;
  margin-right: 0.5rem;
}

.navbar-brand {
  font-weight: 700;
  font-size: 1.5rem;
}

.nav-link {
  font-weight: 500;
  margin: 0 0.5rem;
}

.dropdown-item-text {
  color: #6c757d;
  font-size: 0.875rem;
}
</style>

