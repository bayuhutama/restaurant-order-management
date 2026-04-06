<template>
  <div class="space-y-2">
    <!-- Preview -->
    <div
      class="relative w-full h-40 rounded-xl overflow-hidden border-2 border-dashed border-gray-300 bg-gray-50 flex items-center justify-center cursor-pointer hover:border-orange-400 hover:bg-orange-50 transition-colors"
      :class="{ 'border-orange-500 bg-orange-50': isDragging }"
      @click="triggerPicker"
      @dragover.prevent="isDragging = true"
      @dragleave="isDragging = false"
      @drop.prevent="onDrop"
    >
      <img
        v-if="modelValue"
        :src="modelValue"
        alt="Preview"
        class="absolute inset-0 w-full h-full object-cover"
        @error="onImgError"
      />

      <div v-if="!modelValue || imgError" class="text-center p-4 z-10">
        <PhImage class="h-8 w-8 text-gray-400 mx-auto mb-1" />
        <p class="text-sm font-medium text-gray-600">Click or drag & drop</p>
        <p class="text-xs text-gray-400">JPG, PNG, GIF, WebP · max 10 MB</p>
      </div>

      <!-- Uploading overlay -->
      <div v-if="uploading" class="absolute inset-0 bg-black/50 flex items-center justify-center z-20">
        <div class="text-white text-center">
          <div class="w-6 h-6 border-2 border-white/40 border-t-white rounded-full animate-spin mx-auto mb-1"></div>
          <p class="text-sm">Uploading...</p>
        </div>
      </div>

      <!-- Remove button -->
      <button
        v-if="modelValue && !uploading"
        type="button"
        @click.stop="clear"
        class="absolute top-2 right-2 z-20 bg-red-500 hover:bg-red-600 text-white rounded-full w-7 h-7 flex items-center justify-center text-sm shadow"
      >
        <PhX class="h-4 w-4" />
      </button>
    </div>

    <!-- Hidden file input -->
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="hidden"
      @change="onFileChange"
    />

    <!-- Action row -->
    <div class="flex gap-2">
      <button
        type="button"
        @click="triggerPicker"
        :disabled="uploading"
        class="btn-secondary btn-sm flex-1"
      >
        Choose File
      </button>
      <span class="text-gray-400 text-sm flex items-center">or</span>
      <input
        v-model="urlDraft"
        class="input flex-[2] text-sm"
        placeholder="Paste URL..."
        @keyup.enter="applyUrl"
        @blur="applyUrl"
      />
    </div>

    <!-- Error -->
    <p v-if="uploadError" class="text-xs text-red-500">{{ uploadError }}</p>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { uploadApi } from '@/api'
import { PhImage, PhX } from '@phosphor-icons/vue'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const fileInput = ref(null)
const uploading = ref(false)
const uploadError = ref('')
const isDragging = ref(false)
const imgError = ref(false)
const urlDraft = ref('')

// Sync url draft when parent changes value externally
watch(() => props.modelValue, (val) => {
  if (val && !val.startsWith('blob:')) {
    urlDraft.value = val
  }
  imgError.value = false
}, { immediate: true })

function triggerPicker() {
  if (!uploading.value) fileInput.value?.click()
}

function onImgError() {
  imgError.value = true
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) await upload(file)
  // reset so same file can be re-selected
  e.target.value = ''
}

async function onDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer.files?.[0]
  if (file) await upload(file)
}

async function upload(file) {
  uploadError.value = ''
  uploading.value = true
  imgError.value = false

  // Instant local preview while uploading
  const localUrl = URL.createObjectURL(file)
  emit('update:modelValue', localUrl)

  try {
    const res = await uploadApi.uploadImage(file)
    URL.revokeObjectURL(localUrl)
    emit('update:modelValue', res.data.url)
    urlDraft.value = res.data.url
  } catch (e) {
    URL.revokeObjectURL(localUrl)
    emit('update:modelValue', '')
    uploadError.value = e.response?.data?.message || 'Upload failed'
  } finally {
    uploading.value = false
  }
}

function applyUrl() {
  const trimmed = urlDraft.value.trim()
  if (trimmed !== props.modelValue) {
    imgError.value = false
    emit('update:modelValue', trimmed)
  }
}

function clear() {
  emit('update:modelValue', '')
  urlDraft.value = ''
  uploadError.value = ''
  imgError.value = false
}
</script>
