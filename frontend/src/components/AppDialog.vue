<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="state.show"
        class="fixed inset-0 z-[200] flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/50" @click="state.type === 'alert' && close(true)" />

        <!-- Dialog box -->
        <Transition
          enter-active-class="transition duration-150 ease-out"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition duration-100 ease-in"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="state.show"
            class="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-sm p-6"
          >
            <!-- Icon -->
            <div class="flex justify-center mb-4">
              <div
                :class="[
                  'w-12 h-12 rounded-full flex items-center justify-center',
                  state.variant === 'danger'
                    ? 'bg-red-100 dark:bg-red-900/40'
                    : 'bg-orange-100 dark:bg-orange-900/40'
                ]"
              >
                <PhWarning
                  v-if="state.variant === 'danger'"
                  class="h-6 w-6 text-red-500"
                  weight="fill"
                />
                <PhInfo
                  v-else
                  class="h-6 w-6 text-orange-500"
                  weight="fill"
                />
              </div>
            </div>

            <!-- Title -->
            <h3 class="text-center font-bold text-gray-900 dark:text-gray-100 mb-2">
              {{ state.title }}
            </h3>

            <!-- Message -->
            <p class="text-center text-sm text-gray-600 dark:text-gray-400 mb-6">
              {{ state.message }}
            </p>

            <!-- Buttons -->
            <div v-if="state.type === 'confirm'" class="flex gap-3">
              <button @click="close(false)" class="btn-secondary flex-1">
                Cancel
              </button>
              <button
                @click="close(true)"
                :class="['flex-1', state.variant === 'danger' ? 'btn-danger' : 'btn-primary']"
              >
                Confirm
              </button>
            </div>
            <div v-else>
              <button @click="close(true)" class="btn-primary w-full">
                OK
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useDialog } from '@/composables/useDialog'
import { PhWarning, PhInfo } from '@phosphor-icons/vue'

const { state, close } = useDialog()
</script>
