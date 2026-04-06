<template>
  <div class="min-h-screen flex">
    <!-- Left branding panel -->
    <div class="hidden lg:flex lg:w-1/2 bg-gray-900 flex-col justify-between p-12">
      <div>
        <div class="flex items-center gap-2 mb-2">
          <div class="w-8 h-8 bg-orange-500 rounded-lg flex items-center justify-center">
            <PhForkKnife class="w-5 h-5 text-white" weight="bold" />
          </div>
          <span class="text-white text-lg font-semibold tracking-wide">Savoria</span>
        </div>
        <p class="text-gray-500 text-xs tracking-widest uppercase">Fine Dining & Cuisine</p>
      </div>

      <div>
        <h2 class="text-white text-3xl font-light leading-snug mb-4">
          Manage orders<br />
          <span class="text-orange-400 font-semibold">efficiently.</span>
        </h2>
        <p class="text-gray-400 text-sm leading-relaxed max-w-xs">
          Access the staff dashboard to view, track, and update customer orders in real time.
        </p>
      </div>

      <p class="text-gray-600 text-xs">© {{ new Date().getFullYear() }} Savoria. All rights reserved.</p>
    </div>

    <!-- Right form panel -->
    <div class="w-full lg:w-1/2 flex items-center justify-center px-8 py-12 bg-white">
      <div class="w-full max-w-sm">
        <!-- Mobile logo -->
        <div class="lg:hidden flex items-center gap-2 mb-10">
          <div class="w-7 h-7 bg-orange-500 rounded-lg flex items-center justify-center">
            <PhForkKnife class="w-4 h-4 text-white" weight="bold" />
          </div>
          <span class="text-gray-900 text-lg font-semibold">Savoria</span>
        </div>

        <h1 class="text-2xl font-bold text-gray-900 mb-1">Staff Portal</h1>
        <p class="text-gray-500 text-sm mb-8">Sign in to access the order dashboard.</p>

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
                <PhEye v-if="!showPassword" class="h-4 w-4" />
                <PhEyeSlash v-else class="h-4 w-4" />
              </button>
            </div>
          </div>

          <div v-if="error" class="flex items-start gap-2 p-3 bg-red-50 border border-red-100 rounded-lg">
            <PhWarningCircle class="h-4 w-4 text-red-500 mt-0.5 shrink-0" />
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
            <PhArrowLeft class="h-3 w-3 inline mr-1" />Back to Customer Menu
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
import { PhEye, PhEyeSlash, PhWarningCircle, PhArrowLeft, PhForkKnife } from '@phosphor-icons/vue'

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

    if (!auth.isStaff) {
      auth.logout()
      error.value = 'Access denied. This portal is for staff only.'
      return
    }

    router.push('/staff')
  } catch {
    error.value = 'Invalid username or password.'
  } finally {
    loading.value = false
  }
}
</script>
