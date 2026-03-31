<template>
  <div class="min-h-screen flex">
    <!-- Left branding panel -->
    <div class="hidden lg:flex lg:w-1/2 bg-gray-950 flex-col justify-between p-12">
      <div>
        <div class="flex items-center gap-2 mb-2">
          <div class="w-8 h-8 bg-orange-500 rounded-sm"></div>
          <span class="text-white text-lg font-semibold tracking-wide">Savoria</span>
        </div>
        <p class="text-gray-500 text-xs tracking-widest uppercase">Order Management System</p>
      </div>

      <div>
        <h2 class="text-white text-3xl font-light leading-snug mb-4">
          Full control<br />
          <span class="text-orange-400 font-semibold">at your fingertips.</span>
        </h2>
        <p class="text-gray-400 text-sm leading-relaxed max-w-xs">
          Access the admin panel to manage menus, categories, orders, and system settings.
        </p>
      </div>

      <p class="text-gray-600 text-xs">© {{ new Date().getFullYear() }} Savoria. All rights reserved.</p>
    </div>

    <!-- Right form panel -->
    <div class="w-full lg:w-1/2 flex items-center justify-center px-8 py-12 bg-white">
      <div class="w-full max-w-sm">
        <!-- Mobile logo -->
        <div class="lg:hidden flex items-center gap-2 mb-10">
          <div class="w-7 h-7 bg-orange-500 rounded-sm"></div>
          <span class="text-gray-900 text-lg font-semibold">Savoria</span>
        </div>

        <h1 class="text-2xl font-bold text-gray-900 mb-1">Admin Portal</h1>
        <p class="text-gray-500 text-sm mb-8">Restricted access — administrators only.</p>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Username</label>
            <input
              v-model="form.username"
              type="text"
              class="w-full px-4 py-2.5 border border-gray-200 rounded-lg text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition"
              placeholder="Input your Username"
              required
              autofocus
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Password</label>
            <div class="relative">
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="w-full px-4 py-2.5 pr-10 border border-gray-200 rounded-lg text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition"
                placeholder="Input your Password"
                required
              />
              <button type="button" @click="showPassword = !showPassword" class="absolute inset-y-0 right-3 flex items-center text-gray-400 hover:text-gray-600">
                <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="error" class="flex items-start gap-2 p-3 bg-red-50 border border-red-100 rounded-lg">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-500 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-red-600 text-sm">{{ error }}</span>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-orange-500 hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-semibold py-2.5 rounded-lg transition focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2"
          >
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="mt-8 pt-6 border-t border-gray-100 text-center">
          <RouterLink to="/" class="text-xs text-gray-400 hover:text-gray-600 transition">
            &larr; Back to Customer Menu
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(form.value.username, form.value.password)

    if (!auth.isAdmin) {
      auth.logout()
      error.value = 'Access denied. This portal is for administrators only.'
      return
    }

    router.push('/admin')
  } catch {
    error.value = 'Invalid username or password.'
  } finally {
    loading.value = false
  }
}
</script>
