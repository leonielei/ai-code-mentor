<template>
  <div class="container py-5">
    <div class="row">
      <div class="col-12">
        <div class="card">
          <div class="card-header bg-primary text-white">
            <h3 class="mb-0">🗄️ Gestionnaire de Base de Données</h3>
          </div>
          <div class="card-body">
            <div class="tabs">
              <button 
                class="tab-btn" 
                :class="{ 'active': activeTab === 'users' }"
                @click="activeTab = 'users'"
              >
                👥 Utilisateurs ({{ users.length }})
              </button>
              <button 
                class="tab-btn" 
                :class="{ 'active': activeTab === 'stats' }"
                @click="activeTab = 'stats'"
              >
                📊 Statistiques
              </button>
            </div>

            <!-- Users Tab -->
            <div v-if="activeTab === 'users'" class="tab-content">
              <div class="table-responsive">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nom</th>
                      <th>Nom d'utilisateur</th>
                      <th>Email</th>
                      <th>Rôle</th>
                      <th>Date d'inscription</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="user in users" :key="user.id">
                      <td>{{ user.id }}</td>
                      <td>{{ user.fullName || user.name }}</td>
                      <td>{{ user.username }}</td>
                      <td>{{ user.email }}</td>
                      <td>
                        <span 
                          class="badge" 
                          :class="user.role === 'TEACHER' ? 'bg-primary' : 'bg-success'"
                        >
                          {{ user.role === 'TEACHER' ? '👨‍🏫 Enseignant' : '👨‍🎓 Étudiant' }}
                        </span>
                      </td>
                      <td>{{ formatDate(user.createdAt) }}</td>
                      <td>
                        <button 
                          class="btn btn-sm btn-danger"
                          @click="deleteUser(user.id)"
                          v-if="!isDemoUser(user.email)"
                        >
                          🗑️ Supprimer
                        </button>
                        <span v-else class="text-muted">Démo</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="mt-3">
                <button class="btn btn-warning" @click="clearAllUsers">
                  🗑️ Supprimer tous les utilisateurs (sauf démo)
                </button>
                <button class="btn btn-info ms-2" @click="exportData">
                  💾 Exporter les données
                </button>
              </div>
            </div>

            <!-- Stats Tab -->
            <div v-if="activeTab === 'stats'" class="tab-content">
              <div class="row g-4">
                <div class="col-md-3">
                  <div class="stat-box">
                    <h3>{{ totalUsers }}</h3>
                    <p>Total utilisateurs</p>
                  </div>
                </div>
                <div class="col-md-3">
                  <div class="stat-box">
                    <h3>{{ teacherCount }}</h3>
                    <p>Enseignants</p>
                  </div>
                </div>
                <div class="col-md-3">
                  <div class="stat-box">
                    <h3>{{ studentCount }}</h3>
                    <p>Étudiants</p>
                  </div>
                </div>
                <div class="col-md-3">
                  <div class="stat-box">
                    <h3>{{ recentRegistrations }}</h3>
                    <p>Inscriptions récentes (7j)</p>
                  </div>
                </div>
              </div>

              <div class="mt-4">
                <h4>📈 Tendances d'inscription</h4>
                <div class="chart-placeholder">
                  <p>{{ users.length }} utilisateurs enregistrés au total</p>
                  <div class="progress">
                    <div 
                      class="progress-bar bg-primary" 
                      :style="{ width: (teacherCount / totalUsers * 100) + '%' }"
                    >
                      Enseignants: {{ teacherCount }}
                    </div>
                    <div 
                      class="progress-bar bg-success" 
                      :style="{ width: (studentCount / totalUsers * 100) + '%' }"
                    >
                      Étudiants: {{ studentCount }}
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

<script>
import { ref, computed, onMounted } from 'vue'

export default {
  name: 'DatabaseManager',
  setup() {
    const activeTab = ref('users')
    const users = ref([])

    const loadUsers = () => {
      // Charger les utilisateurs démonstration
      const demoUsers = [
        {
          id: 1,
          username: 'teacher',
          email: 'teacher@demo.com',
          role: 'TEACHER',
          name: 'Prof. Demo',
          fullName: 'Prof. Demo',
          createdAt: '2024-01-01T00:00:00.000Z'
        },
        {
          id: 2,
          username: 'student',
          email: 'student@demo.com',
          role: 'STUDENT',
          name: 'Étudiant Demo',
          fullName: 'Étudiant Demo',
          createdAt: '2024-01-01T00:00:00.000Z'
        }
      ]

      // Charger les utilisateurs enregistrés
      const registeredUsers = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
      users.value = [...demoUsers, ...registeredUsers]
    }

    const totalUsers = computed(() => users.value.length)
    const teacherCount = computed(() => users.value.filter(u => u.role === 'TEACHER').length)
    const studentCount = computed(() => users.value.filter(u => u.role === 'STUDENT').length)
    
    const recentRegistrations = computed(() => {
      const sevenDaysAgo = new Date()
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
      return users.value.filter(u => 
        u.createdAt && new Date(u.createdAt) > sevenDaysAgo
      ).length
    })

    const isDemoUser = (email) => {
      return email === 'teacher@demo.com' || email === 'student@demo.com'
    }

    const formatDate = (dateString) => {
      if (!dateString) return 'N/A'
      const date = new Date(dateString)
      return date.toLocaleDateString('fr-FR')
    }

    const deleteUser = (userId) => {
      if (confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
        const registeredUsers = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
        const updatedUsers = registeredUsers.filter(u => u.id !== userId)
        localStorage.setItem('registeredUsers', JSON.stringify(updatedUsers))
        loadUsers()
        alert('Utilisateur supprimé!')
      }
    }

    const clearAllUsers = () => {
      if (confirm('Êtes-vous sûr de vouloir supprimer tous les utilisateurs (sauf les comptes démo) ?')) {
        localStorage.setItem('registeredUsers', '[]')
        loadUsers()
        alert('Tous les utilisateurs ont été supprimés!')
      }
    }

    const exportData = () => {
      const data = {
        users: users.value,
        exportDate: new Date().toISOString()
      }
      const dataStr = JSON.stringify(data, null, 2)
      const dataBlob = new Blob([dataStr], { type: 'application/json' })
      const url = URL.createObjectURL(dataBlob)
      const link = document.createElement('a')
      link.href = url
      link.download = `users_export_${Date.now()}.json`
      link.click()
    }

    onMounted(() => {
      loadUsers()
    })

    return {
      activeTab,
      users,
      totalUsers,
      teacherCount,
      studentCount,
      recentRegistrations,
      isDemoUser,
      formatDate,
      deleteUser,
      clearAllUsers,
      exportData
    }
  }
}
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
  border-bottom: 2px solid #e9ecef;
}

.tab-btn {
  padding: 1rem 2rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-weight: 600;
  color: #6c757d;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  color: #667eea;
}

.tab-btn.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-box {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  border-radius: 1rem;
  text-align: center;
}

.stat-box h3 {
  font-size: 3rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.stat-box p {
  margin: 0;
  opacity: 0.9;
}

.chart-placeholder {
  background: #f8f9fa;
  padding: 2rem;
  border-radius: 1rem;
}

.progress {
  height: 40px;
  font-size: 1rem;
}

.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>




















