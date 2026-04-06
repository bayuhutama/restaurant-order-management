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
      <h1 class="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-gray-100 mb-2">Our Menu</h1>
      <p class="text-gray-500 dark:text-gray-400">Fresh ingredients, amazing flavors</p>
    </div>

    <!-- Running bill banner -->
    <div
      v-if="tableStore.tableNumber && activeSession && hasUnpaidOrders"
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

    <!-- Search -->
    <div class="relative mb-4">
      <PhMagnifyingGlass class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search menu items..."
        class="input pl-9 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100 dark:placeholder-gray-500"
      />
      <button
        v-if="searchQuery"
        @click="searchQuery = ''"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
      >
        <PhX class="h-4 w-4" />
      </button>
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
        <p>{{ searchQuery ? 'No items match your search' : 'No items available in this category' }}</p>
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 items-stretch">
        <MenuCard v-for="item in filteredItems" :key="item.id" :item="item" />
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
import { PhArrowRight, PhMagnifyingGlass, PhX } from '@phosphor-icons/vue'

const router = useRouter()
const route = useRoute()
const tableStore = useTableStore()
const categories = ref([])
const menuItems = ref([])
const selectedCategory = ref(null)
const loading = ref(true)
const activeSession = ref(null)
const searchQuery = ref('')

const hasUnpaidOrders = computed(() =>
  activeSession.value?.orders?.some(o => o.payment?.status !== 'PAID') ?? false
)

const filteredItems = computed(() => {
  let items = menuItems.value.filter(i => i.available)
  if (selectedCategory.value) {
    items = items.filter(i => i.category?.id === selectedCategory.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    items = items.filter(i =>
      i.name.toLowerCase().includes(q) ||
      (i.description && i.description.toLowerCase().includes(q))
    )
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
