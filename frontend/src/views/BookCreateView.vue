<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import { categories } from '@/constants/categories'

const route = useRoute()
const { token } = useAuth()
const editing = computed(() => route.name === 'book-edit')
const loading = ref(false)

const title = ref('')
const author = ref('')
const category = ref('')
const price = ref('')
const stock = ref('')
const publisher = ref('')
const isbn = ref('')
const coverImageUrl = ref('')
const description = ref('')
const error = ref('')
const success = ref('')
const submitting = ref(false)

function wholeNumber(value) {
  return /^\d+$/.test(value.trim())
}

function validate() {
  if (!title.value.trim()) {
    return '請填寫書名'
  }
  if (title.value.trim().length > 255) {
    return '書名最多 255 個字'
  }
  if (!author.value.trim()) {
    return '請填寫作者'
  }
  if (author.value.trim().length > 255) {
    return '作者最多 255 個字'
  }
  if (!categories.includes(category.value)) {
    return '請選擇分類'
  }
  if (!wholeNumber(price.value)) {
    return '價格必須是 0 或以上的整數'
  }
  if (stock.value.trim() && !wholeNumber(stock.value)) {
    return '庫存必須是 0 或以上的整數'
  }
  const cover = coverImageUrl.value.trim()
  if (cover && !/^https?:\/\/\S+$/i.test(cover)) {
    return '封面網址必須是 http 或 https 連結'
  }
  return ''
}

function emptyToNull(value) {
  const text = value.trim()
  return text ? text : null
}

async function submit() {
  error.value = ''
  success.value = ''
  const message = validate()
  if (message) {
    error.value = message
    return
  }
  submitting.value = true
  const body = {
    title: title.value.trim(),
    author: author.value.trim(),
    category: category.value,
    price: Number(price.value),
    stock: stock.value.trim() ? Number(stock.value) : null,
    publisher: emptyToNull(publisher.value),
    isbn: emptyToNull(isbn.value),
    coverImageUrl: emptyToNull(coverImageUrl.value),
    description: emptyToNull(description.value),
  }
  try {
    const book = await api(editing.value ? `/api/books/${route.params.id}` : '/api/books', {
      method: editing.value ? 'PUT' : 'POST',
      token: token.value,
      body,
    })
    if (editing.value) {
      success.value = '書籍資料已更新'
      return
    }
    success.value = `已上架《${book.title}》`
    title.value = ''
    author.value = ''
    category.value = ''
    price.value = ''
    stock.value = ''
    publisher.value = ''
    isbn.value = ''
    coverImageUrl.value = ''
    description.value = ''
  } catch (e) {
    error.value = describeError(e)
  } finally {
    submitting.value = false
  }
}

function fill(book) {
  title.value = book.title || ''
  author.value = book.author || ''
  category.value = book.category || ''
  price.value = book.price == null ? '' : String(book.price)
  stock.value = book.stock == null ? '' : String(book.stock)
  publisher.value = book.publisher || ''
  isbn.value = book.isbn || ''
  coverImageUrl.value = book.coverImageUrl || ''
  description.value = book.description || ''
}

onMounted(async () => {
  if (!editing.value) return
  loading.value = true
  error.value = ''
  try {
    fill(await api(`/api/books/${route.params.id}`))
  } catch (e) {
    error.value = describeError(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="panel">
    <RouterLink
      v-if="editing"
      class="back"
      :to="{ name: 'book-detail', params: { id: route.params.id } }"
    >
      ← 返回上一頁
    </RouterLink>
    <h1>{{ editing ? '編輯書籍' : '上架書籍' }}</h1>
    <p v-if="loading" class="hint">載入中…</p>
    <form v-else novalidate @submit.prevent="submit">
      <label>
        書名
        <input v-model="title" type="text" />
      </label>
      <label>
        作者
        <input v-model="author" type="text" />
      </label>
      <label>
        分類
        <select v-model="category">
          <option value="">請選擇</option>
          <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>
      <label>
        價格
        <input v-model="price" type="text" inputmode="numeric" />
      </label>
      <label>
        庫存
        <input v-model="stock" type="text" inputmode="numeric" />
        <span class="hint">可以留空</span>
      </label>
      <label>
        出版社
        <input v-model="publisher" type="text" />
      </label>
      <label>
        ISBN
        <input v-model="isbn" type="text" />
      </label>
      <label>
        封面網址
        <input v-model="coverImageUrl" type="text" />
      </label>
      <label>
        簡介
        <textarea v-model="description" rows="4"></textarea>
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>
      <button type="submit" :disabled="submitting">
        {{ submitting ? '儲存中…' : editing ? '儲存' : '上架' }}
      </button>
    </form>
  </main>
</template>

<style scoped>
.panel {
  max-width: 32rem;
  margin: 2.5rem auto 0;
}

.back {
  display: inline-block;
  margin-bottom: 1.25rem;
  color: var(--muted);
  font-weight: 700;
}

h1 {
  font-family: var(--font-serif);
  font-size: 2.2rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  margin-top: 1.5rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.92rem;
}

input,
select,
textarea {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 0.7rem 0.8rem;
  background: var(--bg-elevated);
  color: var(--ink);
  font: inherit;
}

.hint {
  color: var(--muted);
  font-size: 0.8rem;
}

button {
  margin-top: 0.3rem;
  border: 0;
  border-radius: 999px;
  padding: 0.7rem 1rem;
  background: var(--accent);
  color: #fffaf6;
  cursor: pointer;
  font-weight: 700;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.error {
  color: var(--accent);
}

.success {
  color: #1f6a62;
}
</style>
