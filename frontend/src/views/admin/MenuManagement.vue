<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold">Menu Items</h1>
      <button @click="openCreate" class="btn-primary btn-sm">+ Add Item</button>
    </div>

    <!-- Filters -->
    <div class="flex gap-3 mb-6 flex-wrap">
      <div class="relative flex-1 min-w-[180px]">
        <PhMagnifyingGlass class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search items..."
          class="input pl-9 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100 dark:placeholder-gray-500"
        />
        <button v-if="searchQuery" @click="searchQuery = ''" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
          <PhX class="h-3.5 w-3.5" />
        </button>
      </div>
      <select v-model="filterCategory" class="input w-auto dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100">
        <option value="">All Categories</option>
        <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>
      <select v-model="filterAvailable" class="input w-auto dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100">
        <option value="">All Status</option>
        <option value="true">Available</option>
        <option value="false">Unavailable</option>
      </select>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-400">Loading...</div>

    <div v-else class="bg-white dark:bg-gray-800 rounded-xl border dark:border-gray-700 overflow-hidden">
      <div class="overflow-x-auto">
      <table class="w-full text-sm min-w-[600px]">
        <thead class="bg-gray-50 dark:bg-gray-700/50 border-b dark:border-gray-700">
          <tr>
            <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Item</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Category</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Price</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Status</th>
            <th class="px-4 py-3 text-left font-medium text-gray-600 dark:text-gray-300">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y dark:divide-gray-700">
          <tr v-for="item in displayedItems" :key="item.id" class="hover:bg-gray-50 dark:hover:bg-gray-700/50">
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <img
                  :src="item.imageUrl || 'https://via.placeholder.com/40'"
                  :alt="item.name"
                  class="w-10 h-10 object-cover rounded-lg"
                  @error="e => e.target.src = 'https://via.placeholder.com/40'"
                />
                <div>
                  <p class="font-medium">{{ item.name }}</p>
                  <p class="text-xs text-gray-400 truncate max-w-xs">{{ item.description }}</p>
                </div>
              </div>
            </td>
            <td class="px-4 py-3 text-gray-600 dark:text-gray-400">{{ item.category?.name || '—' }}</td>
            <td class="px-4 py-3 font-semibold text-orange-600">{{ formatRupiah(item.price) }}</td>
            <td class="px-4 py-3">
              <button
                @click="toggleAvailability(item)"
                :class="['text-xs px-2 py-1 rounded-full font-medium transition-colors', item.available ? 'bg-green-100 text-green-700 hover:bg-green-200 dark:bg-green-900/40 dark:text-green-300 dark:hover:bg-green-900/60' : 'bg-red-100 text-red-700 hover:bg-red-200 dark:bg-red-900/40 dark:text-red-300 dark:hover:bg-red-900/60']"
              >
                {{ item.available ? 'Available' : 'Unavailable' }}
              </button>
            </td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button @click="openEdit(item)" class="btn-secondary btn-sm">Edit</button>
                <button @click="deleteItem(item.id)" class="btn-danger btn-sm">Delete</button>
              </div>
            </td>
          </tr>
          <tr v-if="displayedItems.length === 0">
            <td colspan="5" class="px-4 py-8 text-center text-gray-400">
              {{ searchQuery ? 'No items match your search' : 'No items found' }}
            </td>
          </tr>
        </tbody>
      </table>
      </div>
    </div>

    <!-- Modal -->
    <Modal v-if="showModal" :title="editing ? 'Edit Menu Item' : 'Add Menu Item'" @close="showModal = false">
      <form @submit.prevent="save" class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="col-span-2">
            <label class="label">Name *</label>
            <input v-model="form.name" class="input" placeholder="Item name" required />
          </div>
          <div>
            <label class="label">Price *</label>
            <input v-model.number="form.price" class="input" type="number" step="0.01" min="0.01" placeholder="0.00" required />
          </div>
          <div>
            <label class="label">Category</label>
            <select v-model="form.categoryId" class="input">
              <option :value="null">None</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="col-span-2">
            <label class="label">Description</label>
            <textarea v-model="form.description" class="input resize-none" rows="2" placeholder="Describe this item..."></textarea>
          </div>
          <div class="col-span-2">
            <label class="label">Image</label>
            <ImageUpload v-model="form.imageUrl" />
          </div>
          <div class="col-span-2 flex items-center gap-2">
            <input type="checkbox" v-model="form.available" id="available" class="rounded" />
            <label for="available" class="text-sm font-medium">Available for ordering</label>
          </div>
        </div>

        <div v-if="error" class="text-red-600 text-sm">{{ error }}</div>

        <div class="flex gap-2 pt-2">
          <button type="button" @click="showModal = false" class="btn-secondary flex-1">Cancel</button>
          <button type="submit" :disabled="saving" class="btn-primary flex-1">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </form>
    </Modal>
  </div>
</template>

<script setup>
/**
 * Admin menu item management page.
 * Supports creating, editing, deleting, and toggling availability of menu items.
 *
 * Filtering (client-side computed):
 * - By category (dropdown)
 * - By availability (dropdown)
 * - By name or description (search input)
 * All three filters are combined with AND logic.
 *
 * Uses Modal + ImageUpload for the create/edit form.
 * Availability can also be toggled inline from the table row without opening the modal.
 */
import { ref, computed, onMounted } from 'vue'
import { menuApi, adminMenuApi } from '@/api'
import { formatRupiah } from '@/utils/format'
import Modal from '@/components/Modal.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { PhMagnifyingGlass, PhX } from '@phosphor-icons/vue'
import { useDialog } from '@/composables/useDialog'

const menuItems = ref([])
const categories = ref([])
const loading = ref(true)
const showModal = ref(false)
const editing = ref(null)   // holds the ID being edited; null = creating new
const saving = ref(false)
const error = ref('')
const filterCategory = ref('')
const filterAvailable = ref('')

const form = ref({ name: '', description: '', price: '', imageUrl: '', categoryId: null, available: true })
const searchQuery = ref('')
const { showConfirm, showAlert } = useDialog()

/** Applies all three filters (category, availability, search text) to the full items list. */
const displayedItems = computed(() => {
  return menuItems.value.filter(item => {
    if (filterCategory.value && item.category?.id !== Number(filterCategory.value)) return false
    if (filterAvailable.value === 'true' && !item.available) return false
    if (filterAvailable.value === 'false' && item.available) return false
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.trim().toLowerCase()
      if (!item.name.toLowerCase().includes(q) && !(item.description && item.description.toLowerCase().includes(q))) return false
    }
    return true
  })
})

async function load() {
  loading.value = true
  try {
    const [menuRes, catRes] = await Promise.all([menuApi.getMenuItems(), menuApi.getCategories()])
    menuItems.value = menuRes.data
    categories.value = catRes.data
  } finally {
    loading.value = false
  }
}


function openCreate() {
  editing.value = null
  form.value = { name: '', description: '', price: '', imageUrl: '', categoryId: null, available: true }
  error.value = ''
  showModal.value = true
}

function openEdit(item) {
  editing.value = item.id
  form.value = {
    name: item.name,
    description: item.description || '',
    price: item.price,
    imageUrl: item.imageUrl || '',
    categoryId: item.category?.id || null,
    available: item.available
  }
  error.value = ''
  showModal.value = true
}

async function save() {
  error.value = ''
  saving.value = true
  try {
    if (editing.value) {
      const res = await adminMenuApi.update(editing.value, form.value)
      const idx = menuItems.value.findIndex(i => i.id === editing.value)
      if (idx !== -1) menuItems.value[idx] = res.data
    } else {
      const res = await adminMenuApi.create(form.value)
      menuItems.value.unshift(res.data)
    }
    showModal.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to save'
  } finally {
    saving.value = false
  }
}

async function toggleAvailability(item) {
  try {
    const res = await adminMenuApi.toggleAvailability(item.id)
    const idx = menuItems.value.findIndex(i => i.id === item.id)
    if (idx !== -1) menuItems.value[idx] = res.data
  } catch (e) {
    showAlert('Failed to update availability', 'Error')
  }
}

async function deleteItem(id) {
  const ok = await showConfirm('This menu item will be permanently deleted.', 'Delete menu item?', 'danger')
  if (!ok) return
  try {
    await adminMenuApi.delete(id)
    menuItems.value = menuItems.value.filter(i => i.id !== id)
  } catch (e) {
    showAlert(e.response?.data?.message || 'Failed to delete', 'Error')
  }
}

onMounted(load)
</script>
