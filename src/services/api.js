import axios from 'axios'

// Create axios instance with base configuration
// Use relative path to leverage Vite proxy in development
// In production, this will need to be configured based on deployment
const api = axios.create({
  baseURL: import.meta.env.DEV ? '/api' : 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
  responseType: 'json',
  responseEncoding: 'utf8',
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    // Enhanced error logging
    if (error.response) {
      // Server responded with error status
      console.error('API Error Response:', error.response.status, error.response.data)
    } else if (error.request) {
      // Request was made but no response received
      console.error('API Network Error - No response received:', {
        url: error.config?.url,
        baseURL: error.config?.baseURL,
        message: error.message
      })
    } else {
      // Something else happened
      console.error('API Error:', error.message)
    }
    
    if (error.response?.status === 401) {
      // Handle unauthorized access
      localStorage.removeItem('authToken')
      // Redirect to login or show error
    }
    return Promise.reject(error)
  }
)

// Exercise API
export const exerciseAPI = {
  // Get all exercises with pagination and filters
  getExercises: (params = {}) => {
    return api.get('/exercises', { params })
  },

  // Get exercise by ID
  getExercise: (id) => {
    return api.get(`/exercises/${id}`)
  },

  // Create new exercise
  createExercise: (exercise) => {
    return api.post('/exercises', exercise)
  },

  // Update exercise
  updateExercise: (id, exercise) => {
    return api.put(`/exercises/${id}`, exercise)
  },

  // Delete exercise
  deleteExercise: (id) => {
    return api.delete(`/exercises/${id}`)
  },

  // Get all topics
  getTopics: () => {
    return api.get('/exercises/topics')
  },

  // Search exercises
  searchExercises: (keyword, page = 0, size = 10) => {
    return api.get('/exercises', {
      params: { keyword, page, size }
    })
  },

  // Get published exercises only
  getPublishedExercises: (page = 0, size = 10) => {
    return api.get('/exercises/published', {
      params: { page, size }
    })
  }
}

// User API
export const userAPI = {
  // Get all users
  getUsers: () => {
    return api.get('/users')
  },

  // Get user by ID
  getUser: (id) => {
    return api.get(`/users/${id}`)
  },

  // Create new user
  createUser: (user) => {
    return api.post('/users', user)
  },

  // Update user
  updateUser: (id, user) => {
    return api.put(`/users/${id}`, user)
  },

  // Delete user
  deleteUser: (id) => {
    return api.delete(`/users/${id}`)
  },

  // Get user by username
  getUserByUsername: (username) => {
    return api.get(`/users/username/${username}`)
  },

  // Get user by email
  getUserByEmail: (email) => {
    return api.get(`/users/email/${email}`)
  }
}

// Submission API
export const submissionAPI = {
  // Get all submissions with pagination
  getSubmissions: (page = 0, size = 10) => {
    return api.get('/submissions', {
      params: { page, size }
    })
  },

  // Get submission by ID
  getSubmission: (id) => {
    return api.get(`/submissions/${id}`)
  },

  // Create new submission
  createSubmission: (submission) => {
    return api.post('/submissions', submission)
  },

  // Update submission
  updateSubmission: (id, submission) => {
    return api.put(`/submissions/${id}`, submission)
  },

  // Delete submission
  deleteSubmission: (id) => {
    return api.delete(`/submissions/${id}`)
  },

  // Get submissions by user
  getSubmissionsByUser: (userId) => {
    return api.get(`/submissions/user/${userId}`)
  },

  // Get submissions by exercise
  getSubmissionsByExercise: (exerciseId, page = 0, size = 10) => {
    return api.get(`/submissions/exercise/${exerciseId}`, {
      params: { page, size }
    })
  },

  // Get submissions by user and exercise
  getSubmissionsByUserAndExercise: (userId, exerciseId) => {
    return api.get(`/submissions/user/${userId}/exercise/${exerciseId}`)
  }
}

// Utility functions
export const apiUtils = {
  // Handle API errors
  handleError: (error) => {
    if (error.response) {
      // Server responded with error status
      const message = error.response.data?.message || error.response.statusText
      return { error: message, status: error.response.status }
    } else if (error.request) {
      // Request was made but no response received
      return { error: 'Network error - please check your connection', status: 0 }
    } else {
      // Something else happened
      return { error: error.message, status: 0 }
    }
  },

  // Extract data from response
  extractData: (response) => {
    return response.data
  },

  // Check if response is successful
  isSuccess: (response) => {
    return response.status >= 200 && response.status < 300
  }
}

export default api










