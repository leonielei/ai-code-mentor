<template>
  <div id="app">
    <Navbar v-if="showNavbar" />
    <router-view />
  </div>
</template>

<script>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from './components/Navbar.vue'

export default {
  name: 'App',
  components: {
    Navbar
  },
  setup() {
    const route = useRoute()
    
    const showNavbar = computed(() => {
      const hideNavbarRoutes = ['login', 'register', 'forgot-password']
      return !hideNavbarRoutes.includes(route.name)
    })
    
    return {
      showNavbar
    }
  }
}
</script>

<style>
#app {
  min-height: 100vh;
}
</style>
