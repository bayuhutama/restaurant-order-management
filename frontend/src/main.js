// Application entry point — bootstraps Vue, Pinia, and the router
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

const app = createApp(App)
app.use(createPinia())  // state management
app.use(router)         // client-side routing
app.mount('#app')
