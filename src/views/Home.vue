<template>
  <div class="home-container">
    <!-- Tableau de bord pour utilisateurs connectés -->
    <div v-if="isLoggedIn" class="dashboard-container">
      <!-- Tableau de bord enseignant -->
      <div v-if="isTeacher" class="teacher-dashboard">
        <div class="container-fluid py-4">
          <div class="row mb-4">
            <div class="col-12">
              <div class="welcome-banner teacher-banner">
                <div class="banner-content">
                  <h1 class="display-5 fw-bold">👨‍🏫 Tableau de bord Enseignant</h1>
                  <p class="lead">Bienvenue, {{ user.name || user.username }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Cartes de statistiques -->
          <div class="row g-4 mb-4">
            <div class="col-md-3">
              <div class="stat-card">
                <div class="stat-icon">📚</div>
                <div class="stat-content">
                  <h3>{{ stats.totalExercises || 0 }}</h3>
                  <p>Exercices créés</p>
                </div>
              </div>
            </div>
            <div class="col-md-3">
              <div class="stat-card">
                <div class="stat-icon">👥</div>
                <div class="stat-content">
                  <h3>{{ stats.activeStudents || 0 }}</h3>
                  <p>Étudiants actifs</p>
                </div>
              </div>
            </div>
            <div class="col-md-3">
              <div class="stat-card">
                <div class="stat-icon">✅</div>
                <div class="stat-content">
                  <h3>{{ stats.totalSubmissions || 0 }}</h3>
                  <p>Soumissions totales</p>
                </div>
              </div>
            </div>
            <div class="col-md-3">
              <div class="stat-card">
                <div class="stat-icon">📈</div>
                <div class="stat-content">
                  <h3>{{ stats.successRate || 0 }}%</h3>
                  <p>Taux de réussite</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Actions rapides -->
          <div class="row g-4">
            <div class="col-md-8">
              <div class="action-card">
                <h4 class="mb-4">🎯 Actions rapides</h4>
                <div class="row g-3">
                  <div class="col-md-6">
                    <router-link to="/teacher/create" class="action-btn">
                      <div class="action-icon">➕</div>
                      <div class="action-text">
                        <h5>Créer un exercice</h5>
                        <p>Nouveau défi pour vos étudiants</p>
                      </div>
                    </router-link>
                  </div>
                  <div class="col-md-6">
                    <router-link to="/exercises" class="action-btn">
                      <div class="action-icon">📋</div>
                      <div class="action-text">
                        <h5>Gérer les exercices</h5>
                        <p>Modifier ou supprimer</p>
                      </div>
                    </router-link>
                  </div>
                  <div class="col-md-6">
                    <router-link to="/submissions" class="action-btn">
                      <div class="action-icon">📊</div>
                      <div class="action-text">
                        <h5>Voir les soumissions</h5>
                        <p>Évaluer le travail des étudiants</p>
                      </div>
                    </router-link>
                  </div>
                  <div class="col-md-6">
                    <router-link to="/analytics" class="action-btn">
                      <div class="action-icon">📈</div>
                      <div class="action-text">
                        <h5>Analyses détaillées</h5>
                        <p>Performances de la classe</p>
                      </div>
                    </router-link>
                  </div>
                </div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="recent-card">
                <h4 class="mb-3">📌 Activités récentes</h4>
                <div class="activity-list">
                  <div class="activity-item">
                    <div class="activity-dot"></div>
                    <div class="activity-content">
                      <p class="activity-title">Nouvel exercice créé</p>
                      <small>Il y a 2 heures</small>
                    </div>
                  </div>
                  <div class="activity-item">
                    <div class="activity-dot"></div>
                    <div class="activity-content">
                      <p class="activity-title">15 nouvelles soumissions</p>
                      <small>Il y a 5 heures</small>
                    </div>
                  </div>
                  <div class="activity-item">
                    <div class="activity-dot"></div>
                    <div class="activity-content">
                      <p class="activity-title">Exercice modifié</p>
                      <small>Hier</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tableau de bord étudiant -->
      <div v-else class="student-dashboard">
        <div class="container-fluid py-4">
          <div class="row mb-4">
            <div class="col-12">
              <div class="welcome-banner student-banner">
                <div class="banner-content">
                  <h1 class="display-5 fw-bold">👨‍🎓 Mon parcours d'apprentissage</h1>
                  <p class="lead">Bienvenue, {{ user.name || user.username }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Statistiques de progression -->
          <div class="row g-4 mb-4">
            <div class="col-md-3">
              <div class="stat-card student-stat">
                <div class="stat-icon">🎯</div>
                <div class="stat-content">
                  <h3>{{ studentStats.completedExercises || 0 }}/{{ studentStats.totalExercises || 0 }}</h3>
                  <p>Exercices complétés</p>
                </div>
              </div>
            </div>
            <div class="col-md-3">
              <div class="stat-card student-stat">
                <div class="stat-icon">⭐</div>
                <div class="stat-content">
                  <h3>{{ studentStats.points || 0 }}</h3>
                  <p>Points obtenus</p>
                </div>
              </div>
            </div>
            <div class="col-md-3">
              <div class="stat-card student-stat">
                <div class="stat-icon">🔥</div>
                <div class="stat-content">
                  <h3>{{ studentStats.consecutiveDays || 0 }}</h3>
                  <p>Jours consécutifs</p>
                </div>
              </div>
            </div>
            <div class="col-md-3">
              <div class="stat-card student-stat">
                <div class="stat-icon">🏆</div>
                <div class="stat-content">
                  <h3>{{ studentStats.ranking || 0 }}</h3>
                  <p>Classement</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Contenu d'apprentissage -->
          <div class="row g-4">
            <div class="col-md-8">
              <div class="action-card">
                <h4 class="mb-4">🚀 Continuer l'apprentissage</h4>
                <div class="learning-cards">
                  <div class="learning-card">
                    <div class="learning-badge in-progress">En cours</div>
                    <h5>Algorithmes de tri</h5>
                    <div class="progress mb-2">
                      <div class="progress-bar" style="width: 60%"></div>
                    </div>
                    <p class="text-muted">3 exercices restants</p>
                    <router-link to="/exercises/1" class="btn btn-sm btn-primary">Continuer</router-link>
                  </div>
                  <div class="learning-card">
                    <div class="learning-badge recommended">Recommandé</div>
                    <h5>Structures de données</h5>
                    <div class="progress mb-2">
                      <div class="progress-bar" style="width: 25%"></div>
                    </div>
                    <p class="text-muted">6 exercices restants</p>
                    <router-link to="/exercises/2" class="btn btn-sm btn-outline-primary">Commencer</router-link>
                  </div>
                </div>
                <div class="mt-3">
                  <router-link to="/exercises" class="btn btn-primary">
                    {{ isTeacher ? 'Voir tous les exercices' : 'Voir les exercices publiés' }} →
                  </router-link>
                </div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="recent-card">
                <h4 class="mb-3">🎖️ Mes réalisations</h4>
                <div class="achievement-list">
                  <div class="achievement-item">
                    <span class="achievement-icon">🥇</span>
                    <div>
                      <p class="achievement-title">Premier exercice complété</p>
                      <small>Obtenu</small>
                    </div>
                  </div>
                  <div class="achievement-item">
                    <span class="achievement-icon">🔥</span>
                    <div>
                      <p class="achievement-title">Série de 7 jours</p>
                      <small>Obtenu</small>
                    </div>
                  </div>
                  <div class="achievement-item locked">
                    <span class="achievement-icon">🏆</span>
                    <div>
                      <p class="achievement-title">Maître des algorithmes</p>
                      <small>Complétez 20 exercices</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Page de connexion pour utilisateurs non connectés -->
    <div v-else class="landing-container">
      <div class="hero-section">
        <div class="container text-center">
          <div class="hero-content">
            <div class="hero-icon">🧠</div>
            <h1 class="display-3 fw-bold mb-4">AICodeMentor</h1>
            <p class="lead mb-5">
              Apprenez la programmation avec l'intelligence artificielle<br>
              Plateforme d'apprentissage interactive pour enseignants et étudiants
            </p>
            <div class="hero-buttons">
              <router-link to="/login" class="btn btn-light btn-lg px-5 py-3">
                Se connecter
              </router-link>
              <router-link to="/register" class="btn btn-outline-light btn-lg px-5 py-3">
                Créer un compte
              </router-link>
            </div>
          </div>
        </div>
      </div>

      <!-- Features Section -->
      <div class="features-showcase">
        <div class="container">
          <div class="row g-4">
            <div class="col-md-4">
              <div class="feature-card">
                <div class="feature-icon">🎓</div>
                <h3>Apprentissage Interactif</h3>
                <p>Pratiquez avec des exercices guidés par l'IA</p>
                <div class="feature-glow"></div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="feature-card">
                <div class="feature-icon">💡</div>
                <h3>Feedback Intelligent</h3>
                <p>Obtenez des conseils personnalisés en temps réel</p>
                <div class="feature-glow"></div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="feature-card">
                <div class="feature-icon">🚀</div>
                <h3>Progression Rapide</h3>
                <p>Suivez vos progrès et atteignez vos objectifs</p>
                <div class="feature-glow"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../services/api'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const isLoggedIn = ref(false)
    const isTeacher = ref(false)
    const user = ref({})
    const stats = ref({})
    const studentStats = ref({})

    const checkAuth = () => {
      const token = localStorage.getItem('authToken')
      const userData = localStorage.getItem('user')
      
      if (token && userData) {
        isLoggedIn.value = true
        user.value = JSON.parse(userData)
        isTeacher.value = user.value.role === 'TEACHER'
      } else {
        isLoggedIn.value = false
        user.value = {}
        isTeacher.value = false
      }
    }

    const loadStats = async () => {
      if (!isLoggedIn.value) return
      
      try {
        if (isTeacher.value) {
          const response = await api.get('/stats/teacher')
          stats.value = response.data
        } else {
          const response = await api.get(`/stats/student/${user.value.id}`)
          studentStats.value = response.data
        }
      } catch (error) {
        console.error('Error loading stats:', error)
        // Set default values on error
        if (isTeacher.value) {
          stats.value = { totalExercises: 0, activeStudents: 0, totalSubmissions: 0, successRate: 0 }
        } else {
          studentStats.value = { completedExercises: 0, totalExercises: 0, points: 0, consecutiveDays: 0, ranking: 0 }
        }
      }
    }

    onMounted(async () => {
      checkAuth()
      if (isLoggedIn.value) {
        await loadStats()
      }
      window.addEventListener('storage', checkAuth)
    })

    // Watch for route changes
    watch(route, () => {
      checkAuth()
      if (isLoggedIn.value) {
        loadStats()
      }
    })

    // Watch for auth changes
    watch(isLoggedIn, (newVal) => {
      if (newVal) {
        loadStats()
      }
    })

    return {
      isLoggedIn,
      isTeacher,
      user,
      stats,
      studentStats
    }
  }
}
</script>

<style scoped>
.home-container {
  min-height: calc(100vh - 56px);
  background: #f8f9fa;
}

/* Page de connexion */
.landing-container {
  min-height: calc(100vh - 56px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  position: relative;
  overflow: hidden;
}

.landing-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.hero-section {
  position: relative;
  z-index: 1;
  padding: 5rem 0;
}

.hero-icon {
  font-size: 6rem;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.hero-content h1 {
  color: white;
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  animation: fadeInUp 0.8s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-content .lead {
  color: rgba(255, 255, 255, 0.95);
  font-size: 1.3rem;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.hero-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.hero-buttons .btn {
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.hero-buttons .btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 25px rgba(0, 0, 0, 0.3);
}

/* Features Showcase */
.features-showcase {
  position: relative;
  z-index: 1;
  padding: 4rem 0;
}

.feature-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 1.5rem;
  padding: 3rem 2rem;
  text-align: center;
  color: white;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover {
  transform: translateY(-10px);
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3);
}

.feature-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  animation: bounce 2s ease infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.feature-card h3 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.feature-card p {
  font-size: 1.1rem;
  opacity: 0.95;
  text-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.feature-glow {
  position: absolute;
  bottom: -50%;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  opacity: 0;
  transition: all 0.4s ease;
}

.feature-card:hover .feature-glow {
  bottom: -30%;
  opacity: 1;
}

/* Bannière de bienvenue */
.welcome-banner {
  border-radius: 1.5rem;
  padding: 3rem;
  color: white;
  margin-bottom: 2rem;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, transparent 70%);
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.teacher-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
}

.student-banner {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 50%, #ff9a8b 100%);
}

.banner-content {
  position: relative;
  z-index: 1;
}

.banner-content h1 {
  margin-bottom: 0.5rem;
}

/* Cartes de statistiques */
.stat-card {
  background: white;
  border-radius: 1.5rem;
  padding: 2rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.15);
  border-color: rgba(102, 126, 234, 0.2);
}

.stat-icon {
  font-size: 3.5rem;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
  transition: transform 0.4s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
}

.stat-content h3 {
  margin: 0;
  font-size: 2.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-content p {
  margin: 0;
  color: #6c757d;
  font-size: 0.95rem;
  font-weight: 500;
}

.student-stat .stat-content h3 {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.student-stat::before {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

/* Cartes d'action */
.action-card, .recent-card {
  background: white;
  border-radius: 1.5rem;
  padding: 2.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  height: 100%;
  transition: all 0.4s ease;
  border: 2px solid transparent;
}

.action-card:hover, .recent-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  border-color: rgba(102, 126, 234, 0.2);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.75rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  text-decoration: none;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 2px solid transparent;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  transition: left 0.5s ease;
}

.action-btn:hover::before {
  left: 0;
}

.action-btn:hover {
  background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);
  border-color: #667eea;
  transform: translateX(10px) scale(1.02);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.2);
}

.action-icon {
  font-size: 2.5rem;
  min-width: 60px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  transition: transform 0.4s ease;
  position: relative;
  z-index: 1;
}

.action-btn:hover .action-icon {
  transform: scale(1.2) rotate(10deg);
}

.action-text {
  position: relative;
  z-index: 1;
}

.action-text h5 {
  margin: 0 0 0.25rem 0;
  color: #212529;
  font-size: 1.15rem;
  font-weight: 700;
}

.action-text p {
  margin: 0;
  color: #6c757d;
  font-size: 0.9rem;
}

/* Liste d'activités */
.activity-list, .achievement-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.activity-item {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 0.75rem;
  border-bottom: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.activity-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
}

.activity-item:last-child {
  border-bottom: none;
  padding-bottom: 1.25rem;
}

.activity-dot {
  width: 14px;
  height: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  margin-top: 6px;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.4);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.activity-title {
  margin: 0;
  font-weight: 700;
  color: #212529;
  font-size: 1.05rem;
}

.activity-content small {
  color: #6c757d;
  font-size: 0.9rem;
}

/* Cartes d'apprentissage */
.learning-cards {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.learning-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 1rem;
  padding: 2rem;
  position: relative;
  transition: all 0.4s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border: 2px solid transparent;
  overflow: hidden;
}

.learning-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.learning-card:hover::before {
  opacity: 1;
}

.learning-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  border-color: rgba(102, 126, 234, 0.3);
}

.learning-badge {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  font-size: 0.8rem;
  font-weight: 700;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 1;
}

.learning-badge.in-progress {
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
  color: #000;
}

.learning-badge.recommended {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.learning-card h5 {
  margin-bottom: 1rem;
  position: relative;
  z-index: 1;
  font-weight: 700;
  font-size: 1.2rem;
}

/* Réalisations */
.achievement-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 0.5rem;
}

.achievement-item.locked {
  opacity: 0.5;
}

.achievement-icon {
  font-size: 2rem;
}

.achievement-title {
  margin: 0;
  font-weight: 600;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .welcome-banner {
    padding: 2rem 1rem;
  }
  
  .stat-card {
    justify-content: center;
    text-align: center;
    flex-direction: column;
  }
  
  .action-btn {
    flex-direction: column;
    text-align: center;
  }
  
  .hero-buttons {
    flex-direction: column;
  }
  
  .btn-lg {
    width: 100%;
  }
}
</style>
