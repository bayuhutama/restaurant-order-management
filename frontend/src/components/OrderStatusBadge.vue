<template>
  <span :class="['inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold', config.color]">
    <span :class="['w-1.5 h-1.5 rounded-full', config.dot, config.pulse ? 'animate-pulse' : '']"></span>
    {{ config.label }}
  </span>
</template>

<script setup>
/**
 * Colored pill badge that represents an order status.
 * Each status has its own color scheme, label, and optional pulsing dot
 * to draw attention to active/in-progress states.
 *
 * Dark mode class strings are written in full (not constructed dynamically)
 * so Tailwind's content scanner can detect and include them in the build.
 */
import { computed } from 'vue'

const props = defineProps({ status: String })

// Config map: each status defines its Tailwind color classes, dot color,
// display label, and whether the dot should animate (active states).
const configs = {
  AWAITING_PAYMENT: { color: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-300', dot: 'bg-orange-500', label: 'Awaiting Payment', pulse: true  },
  PENDING:          { color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300', dot: 'bg-yellow-500', label: 'Order Received',    pulse: true  },
  CONFIRMED:        { color: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',         dot: 'bg-blue-500',   label: 'Confirmed',         pulse: false },
  PREPARING:        { color: 'bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300', dot: 'bg-purple-500', label: 'Preparing',         pulse: true  },
  READY:            { color: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',     dot: 'bg-green-500',  label: 'Ready',             pulse: true  },
  DELIVERED:        { color: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',            dot: 'bg-gray-400',   label: 'Delivered',         pulse: false },
  CANCELLED:        { color: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',             dot: 'bg-red-500',    label: 'Cancelled',         pulse: false },
}

const config = computed(() => configs[props.status] || { color: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300', dot: 'bg-gray-400', label: props.status, pulse: false })
</script>
