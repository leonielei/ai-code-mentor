import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles/main.scss'

// Configure Monaco Editor to prevent UI freezing
// Disable web workers and use main thread to avoid blocking
if (typeof window !== 'undefined' && window.MonacoEnvironment === undefined) {
  window.MonacoEnvironment = {
    getWorkerUrl: function (moduleId, label) {
      // Return empty string to disable web workers and use main thread
      // This prevents UI freezing but may be slightly slower
      return ''
    },
    getWorker: function (moduleId, label) {
      // Return a promise that resolves to null (not reject) to properly disable web workers
      // This prevents uncaught promise errors
      return Promise.resolve(null)
    }
  }
}

const app = createApp(App)
app.use(router)
app.mount('#app')
