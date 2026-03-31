<template>
  <span :class="['inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold', config.color]">
    <span :class="['w-1.5 h-1.5 rounded-full', config.dot, config.pulse ? 'animate-pulse' : '']"></span>
    {{ config.label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: String })

const configs = {
  AWAITING_PAYMENT: { color: 'bg-orange-100 text-orange-700', dot: 'bg-orange-500', label: 'Awaiting Payment', pulse: true  },
  PENDING:          { color: 'bg-yellow-100 text-yellow-700', dot: 'bg-yellow-500', label: 'Order Received',    pulse: true  },
  CONFIRMED:        { color: 'bg-blue-100 text-blue-700',     dot: 'bg-blue-500',   label: 'Confirmed',         pulse: false },
  PREPARING:        { color: 'bg-purple-100 text-purple-700', dot: 'bg-purple-500', label: 'Preparing',         pulse: true  },
  READY:            { color: 'bg-green-100 text-green-700',   dot: 'bg-green-500',  label: 'Ready',             pulse: true  },
  DELIVERED:        { color: 'bg-gray-100 text-gray-600',     dot: 'bg-gray-400',   label: 'Delivered',         pulse: false },
  CANCELLED:        { color: 'bg-red-100 text-red-700',       dot: 'bg-red-500',    label: 'Cancelled',         pulse: false },
}

const config = computed(() => configs[props.status] || { color: 'bg-gray-100 text-gray-600', dot: 'bg-gray-400', label: props.status, pulse: false })
</script>
