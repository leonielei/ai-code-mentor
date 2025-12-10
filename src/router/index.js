import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPassword.vue')
    },
    {
      path: '/exercises',
      name: 'exercises',
      component: () => import('../views/ExerciseList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/exercises/:id',
      name: 'exercise-detail',
      component: () => import('../views/ExerciseDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/exercise/:id/test',
      name: 'exercise-test',
      component: () => import('../views/ExerciseTest.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/teacher/create',
      name: 'create-exercise',
      component: () => import('../views/CreateExercise.vue'),
      meta: { requiresAuth: true, requiresTeacher: true }
    },
    {
      path: '/database',
      name: 'database',
      component: () => import('../views/DatabaseManager.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/submissions',
      name: 'submissions',
      component: () => import('../views/Submissions.vue'),
      meta: { requiresAuth: true, requiresTeacher: true }
    }
  ]
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('authToken')
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresTeacher && user.role !== 'TEACHER') {
    next('/')
  } else {
    next()
  }
})

export default router

