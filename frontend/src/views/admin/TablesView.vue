<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-xl font-bold">Table QR Codes</h1>
        <p class="text-sm text-gray-500 mt-1">Print and place these at each table so customers can scan to order.</p>
      </div>
      <button @click="printAll" class="btn-secondary btn-sm">Print All</button>
    </div>

    <!-- Config -->
    <div class="card p-5 mb-6 flex items-end gap-4">
      <div>
        <label class="label">Number of Tables</label>
        <input v-model.number="tableCount" type="number" min="1" max="100" class="input w-32" />
      </div>
      <div>
        <label class="label">Base URL</label>
        <input v-model="baseUrl" class="input w-72" placeholder="http://localhost:5173" />
      </div>
      <button @click="generateAll" :disabled="generating" class="btn-primary">
        {{ generating ? 'Generating...' : 'Generate QR Codes' }}
      </button>
    </div>

    <!-- QR Grid -->
    <div v-if="qrCodes.length" id="print-area" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
      <div
        v-for="qr in qrCodes"
        :key="qr.table"
        class="card p-4 flex flex-col items-center gap-2 print:break-inside-avoid"
      >
        <img :src="qr.dataUrl" :alt="`Table ${qr.table}`" class="w-40 h-40" />
        <div class="text-center">
          <p class="font-bold text-lg">Table {{ qr.table }}</p>
          <p class="text-xs text-gray-400 break-all">{{ qr.url }}</p>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-20 text-gray-400">
      <p>Set the number of tables and click <strong>Generate QR Codes</strong>.</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import QRCode from 'qrcode'

const tableCount = ref(10)
const baseUrl = ref(window.location.origin.replace('8080', '5173'))
const qrCodes = ref([])
const generating = ref(false)

async function generateAll() {
  generating.value = true
  qrCodes.value = []
  try {
    const results = []
    for (let i = 1; i <= tableCount.value; i++) {
      const url = `${baseUrl.value}/?table=${i}`
      const dataUrl = await QRCode.toDataURL(url, {
        width: 200,
        margin: 2,
        color: { dark: '#111827', light: '#ffffff' }
      })
      results.push({ table: i, url, dataUrl })
    }
    qrCodes.value = results
  } finally {
    generating.value = false
  }
}

function printAll() {
  window.print()
}
</script>

<style>
@media print {
  body > *:not(#app) { display: none; }
  nav, aside { display: none !important; }
  #print-area { display: grid !important; }
}
</style>
