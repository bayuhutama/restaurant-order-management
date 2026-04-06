<template>
  <!-- Table badge (shown after QR scan) -->
  <div
    v-if="tableStore.tableNumber"
    class="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-gray-900 text-white px-5 py-2.5 rounded-full shadow-xl flex items-center gap-2 text-sm font-medium pointer-events-none"
  >
    Table {{ tableStore.tableNumber }}
  </div>

  <div class="max-w-7xl mx-auto px-4 py-8">
    <!-- Hero -->
    <div class="text-center mb-10">
      <h1 class="text-3xl sm:text-4xl font-bold text-gray-900 mb-2">Our Menu</h1>
      <p class="text-gray-500 dark:text-gray-400">Fresh ingredients, amazing flavors</p>
    </div>

    <!-- Running bill banner -->
    <div
      v-if="tableStore.tableNumber && activeSession && activeSession.orderCount > 0"
      class="bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-xl p-4 mb-6 flex items-center justify-between"
    >
      <span class="text-sm text-gray-700 dark:text-gray-300 font-medium">
        Table {{ tableStore.tableNumber }} has a running bill &middot; {{ formatRupiah(activeSession.totalAmount) }}
      </span>
      <RouterLink
        :to="`/table/${tableStore.tableNumber}/bill`"
        class="text-orange-600 font-semibold text-sm hover:text-orange-700 inline-flex items-center gap-1"
      >View Bill <PhArrowRight class="h-4 w-4" /></RouterLink>
    </div>

    <!-- Category Filter -->
    <div class="flex gap-2 overflow-x-auto py-1 px-1 mb-8 scrollbar-hide">
      <button
        @click="selectedCategory = null"
        :class="['btn btn-sm whitespace-nowrap', selectedCategory === null ? 'btn-primary' : 'btn-secondary']"
      >
        All
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        @click="selectedCategory = cat.id"
        :class="['btn btn-sm whitespace-nowrap', selectedCategory === cat.id ? 'btn-primary' : 'btn-secondary']"
      >
        {{ cat.name }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div v-for="n in 8" :key="n" class="card animate-pulse">
        <div class="h-48 bg-gray-200 rounded-t-xl"></div>
        <div class="p-4 space-y-2">
          <div class="h-4 bg-gray-200 rounded w-3/4"></div>
          <div class="h-3 bg-gray-200 rounded w-full"></div>
          <div class="h-8 bg-gray-200 rounded mt-4"></div>
        </div>
      </div>
    </div>

    <!-- Menu Grid -->
    <div v-else>
      <div v-if="filteredItems.length === 0" class="text-center py-20 text-gray-400">
        <p>No items available in this category</p>
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 items-stretch">
        <MenuCard v-for="item in filteredItems" :key="item.id" :item="item" />
      </div>
    </div>

    <!-- Track Order -->
    <div class="mt-12 p-6 bg-gray-800 text-white rounded-2xl text-center">
      <h2 class="text-lg font-bold mb-2">Track Your Order</h2>
      <p class="text-gray-400 text-sm mb-4">Enter your order number to see its status</p>
      <div class="flex flex-col sm:flex-row gap-2 max-w-sm mx-auto">
        <input
          v-model="trackNumber"
          placeholder="ORD-YYYYMMDD-XXXXXX"
          class="input bg-gray-700 border-gray-600 text-white placeholder-gray-400 dark:bg-gray-700 dark:border-gray-600 flex-1"
          @keyup.enter="goTrack"
        />
        <button @click="goTrack" class="btn-primary sm:flex-shrink-0">Track</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { menuApi, tableSessionApi } from '@/api'
import { useTableStore } from '@/stores/table'
import { formatRupiah } from '@/utils/format'
import MenuCard from '@/components/MenuCard.vue'
import { PhArrowRight } from '@phosphor-icons/vue'

const router = useRouter()
const route = useRoute()
const tableStore = useTableStore()
const categories = ref([])
const menuItems = ref([])
const selectedCategory = ref(null)
const loading = ref(true)
const trackNumber = ref('')
const activeSession = ref(null)

const filteredItems = computed(() => {
  let items = menuItems.value.filter(i => i.available)
  if (selectedCategory.value) {
    items = items.filter(i => i.category?.id === selectedCategory.value)
  }
  return items
})

async function loadData() {
  loading.value = true
  try {
    const [catRes, menuRes] = await Promise.all([
      menuApi.getCategories(),
      menuApi.getMenuItems({ available: true })
    ])
    categories.value = catRes.data
    menuItems.value = menuRes.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goTrack() {
  if (trackNumber.value.trim()) {
    router.push(`/track/${trackNumber.value.trim()}`)
  }
}

async function loadActiveSession(tableNumber) {
  try {
    const res = await tableSessionApi.getSession(tableNumber)
    activeSession.value = res.data
  } catch {
    activeSession.value = null
  }
}

onMounted(() => {
  // QR code scan sets ?table=X in the URL
  if (route.query.table) {
    tableStore.setTable(route.query.table)
    // Clean the query param from the URL without a page reload
    router.replace({ query: {} })
  }
  if (tableStore.tableNumber) {
    loadActiveSession(tableStore.tableNumber)
  }
  loadData()
})
</script>
