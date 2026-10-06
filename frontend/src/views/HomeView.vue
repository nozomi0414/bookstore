<script setup>
import { onMounted, ref } from 'vue'

const books = ref([])
const error = ref('')

onMounted(async () => {
  try {
    const response = await fetch('http://localhost:8080/api/books')
    if (!response.ok) {
      throw new Error('無法取得書單')
    }
    books.value = await response.json()
  } catch (e) {
    error.value = e.message
  }
})
</script>

<template>
  <main>
    <p v-if="error">{{ error }}</p>
    <ul v-else>
      <li v-for="book in books" :key="book.id">
        {{ book.title }} — {{ book.price }}
      </li>
    </ul>
  </main>
</template>