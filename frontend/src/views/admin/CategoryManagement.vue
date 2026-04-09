<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-bold">Categories</h1>
      <button @click="openCreate" class="btn-primary btn-sm">+ Add Category</button>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-400">Loading...</div>

    <div v-else class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="cat in categories" :key="cat.id" class="card p-4">
        <div class="flex items-start gap-3">
          <img
            v-if="cat.imageUrl"
            :src="cat.imageUrl"
            :alt="cat.name"
            class="w-16 h-16 object-cover rounded-lg flex-shrink-0"
            @error="e => e.target.style.display='none'"
          />
          <div class="flex-1 min-w-0">
            <h3 class="font-semibold">{{ cat.name }}</h3>
            <p class="text-sm text-gray-500 dark:text-gray-400 truncate">{{ cat.description || 'No description' }}</p>
          </div>
        </div>
        <div class="flex gap-2 mt-3">
          <button @click="openEdit(cat)" class="btn-secondary btn-sm flex-1">Edit</button>
          <button @click="deleteCategory(cat.id)" class="btn-danger btn-sm flex-1">Delete</button>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <Modal v-if="showModal" :title="editing ? 'Edit Category' : 'Add Category'" @close="showModal = false">
      <form @submit.prevent="save" class="space-y-4">
        <div>
          <label class="label">Name *</label>
          <input v-model="form.name" class="input" placeholder="Category name" required />
        </div>
        <div>
          <label class="label">Description</label>
          <input v-model="form.description" class="input" placeholder="Short description" />
        </div>
        <div>
          <label class="label">Image</label>
          <ImageUpload v-model="form.imageUrl" />
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
 * Admin category management page.
 * Supports creating, editing, and deleting categories.
 * Uses a shared Modal component for the create/edit form.
 * Images are uploaded via ImageUpload (drag & drop, file picker, or URL paste).
 * Deletion requires confirmation via useDialog to prevent accidental data loss.
 */
import { ref, onMounted } from 'vue'
import { menuApi, adminCategoryApi } from '@/api'
import Modal from '@/components/Modal.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { useDialog } from '@/composables/useDialog'

const categories = ref([])
const { showConfirm, showAlert } = useDialog()
const loading = ref(true)
const showModal = ref(false)
const editing = ref(null)   // holds the ID of the category being edited; null = creating new
const saving = ref(false)
const error = ref('')
const form = ref({ name: '', description: '', imageUrl: '' })

async function load() {
  loading.value = true
  try {
    const res = await menuApi.getCategories()
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', description: '', imageUrl: '' }
  error.value = ''
  showModal.value = true
}

function openEdit(cat) {
  editing.value = cat.id
  form.value = { name: cat.name, description: cat.description || '', imageUrl: cat.imageUrl || '' }
  error.value = ''
  showModal.value = true
}

async function save() {
  error.value = ''
  saving.value = true
  try {
    if (editing.value) {
      const res = await adminCategoryApi.update(editing.value, form.value)
      const idx = categories.value.findIndex(c => c.id === editing.value)
      if (idx !== -1) categories.value[idx] = res.data
    } else {
      const res = await adminCategoryApi.create(form.value)
      categories.value.push(res.data)
    }
    showModal.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to save'
  } finally {
    saving.value = false
  }
}

async function deleteCategory(id) {
  const ok = await showConfirm('This category will be permanently deleted.', 'Delete category?', 'danger')
  if (!ok) return
  try {
    await adminCategoryApi.delete(id)
    categories.value = categories.value.filter(c => c.id !== id)
  } catch (e) {
    showAlert(e.response?.data?.message || 'Failed to delete', 'Error')
  }
}

onMounted(load)
</script>
