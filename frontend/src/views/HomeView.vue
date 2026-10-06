<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '@/api/client'
import { useCart } from '@/composables/useCart'

const books = ref([])
const error = ref('')
const loading = ref(true)
const brokenCovers = ref({})
const selected = ref('全部')
const justAdded = ref(null)
const { items, addBook } = useCart()

const categories = computed(() => {
  const names = books.value.map((book) => book.category || '未分類')
  return [...new Set(names)]
})

const visibleBooks = computed(() => {
  if (selected.value === '全部') return books.value
  return books.value.filter((book) => (book.category || '未分類') === selected.value)
})

onMounted(async () => {
  try {
    books.value = await api('/api/books')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

function formatPrice(price) {
  if (price == null) return '價格未定'
  return `NT$ ${price.toLocaleString('zh-Hant')}`
}

function coverTone(title) {
  const hues = [16, 152, 202, 28, 262]
  const sum = [...(title || '書')].reduce((total, char) => total + char.charCodeAt(0), 0)
  return hues[sum % hues.length]
}

function coverFailed(id) {
  brokenCovers.value = { ...brokenCovers.value, [id]: true }
}

function showCover(book) {
  return Boolean(book.coverImageUrl) && !brokenCovers.value[book.id]
}

function atStockLimit(book) {
  if (book.stock == null) return false
  const item = items.value.find((entry) => entry.bookId === book.id)
  return (item?.quantity || 0) >= book.stock
}

function add(book) {
  if (!addBook(book)) return
  justAdded.value = book.id
  window.setTimeout(() => {
    if (justAdded.value === book.id) justAdded.value = null
  }, 1200)
}
</script>

<template>
  <main class="catalog">
    <aside class="filters">
      <p class="eyebrow">分類</p>
      <div class="tags" role="group" aria-label="依分類篩選">
        <button type="button" :class="{ active: selected === '全部' }" @click="selected = '全部'">
          全部
        </button>
        <button
          v-for="category in categories"
          :key="category"
          type="button"
          :class="{ active: selected === category }"
          @click="selected = category"
        >
          {{ category }}
        </button>
      </div>
    </aside>

    <section>
      <header class="page-head">
        <h1>{{ books.length === 0 && !loading && !error ? '書架還是空的' : '在架上的書' }}</h1>
        <p v-if="!loading && !error" class="lead">
          {{
            books.length === 0
              ? '新書上架後會出現在這裡。'
              : selected === '全部'
                ? `共 ${books.length} 本`
                : `${selected} · ${visibleBooks.length} 本`
          }}
        </p>
      </header>

      <p v-if="loading" class="state">正在整理書架…</p>
      <p v-else-if="error" class="state error">{{ error }}</p>
      <p v-else-if="visibleBooks.length === 0" class="state">這個分類目前沒有書</p>

      <ul v-else class="books">
        <li v-for="book in visibleBooks" :key="book.id" class="book">
          <div class="cover-wrap">
            <img
              v-if="showCover(book)"
              class="cover"
              :src="book.coverImageUrl"
              :alt="book.title"
              @error="coverFailed(book.id)"
            />
            <div
              v-else
              class="cover placeholder"
              :style="{ '--tone': coverTone(book.title) }"
              aria-hidden="true"
            >
              <span>{{ book.title }}</span>
            </div>
          </div>

          <div class="info">
            <p v-if="book.category" class="category">{{ book.category }}</p>
            <h2>{{ book.title }}</h2>
            <p class="author">{{ book.author || '作者未填' }}</p>
            <div class="meta">
              <p class="price">{{ formatPrice(book.price) }}</p>
              <p v-if="book.stock != null" class="stock">庫存 {{ book.stock }}</p>
            </div>
            <button type="button" class="add" :disabled="atStockLimit(book)" @click="add(book)">
              {{
                atStockLimit(book) ? '已達庫存' : justAdded === book.id ? '已加入購物車' : '加入購物車'
              }}
            </button>
          </div>
        </li>
      </ul>
    </section>
  </main>
</template>

<style scoped>
.catalog {
  display: grid;
  grid-template-columns: 11.5rem 1fr;
  gap: 2.5rem;
  align-items: start;
  padding-top: 1.5rem;
}

.filters {
  position: sticky;
  top: 5rem;
}

.eyebrow {
  color: var(--accent);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.tags {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.4rem;
  margin-top: 0.85rem;
}

.tags button {
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 0.35rem 0.8rem;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}

.tags button.active {
  border-color: var(--line);
  background: var(--bg-elevated);
  color: var(--ink);
  font-weight: 700;
}

.page-head {
  margin-bottom: 1.5rem;
}

h1 {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.lead,
.state {
  margin-top: 0.45rem;
  color: var(--muted);
}

.error {
  color: var(--accent);
}

.books {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 1.25rem;
  list-style: none;
  padding: 0;
}

.book {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--bg-elevated);
  box-shadow: var(--shadow);
}

.cover-wrap {
  aspect-ratio: 3 / 4;
  background: var(--line);
}

.cover {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  display: flex;
  align-items: flex-end;
  height: 100%;
  padding: 1rem;
  background: linear-gradient(165deg, hsl(var(--tone) 38% 46%), hsl(var(--tone) 32% 22%));
  color: #fffdf8;
}

.placeholder span {
  font-family: var(--font-serif);
  font-size: 1.35rem;
  font-weight: 700;
  line-height: 1.35;
}

.info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.95rem 1rem 1.05rem;
}

.category {
  color: var(--accent);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

h2 {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.35;
}

.author {
  color: var(--muted);
  font-size: 0.92rem;
}

.meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: auto;
  padding-top: 0.85rem;
}

.price {
  font-weight: 700;
}

.stock {
  color: var(--muted);
  font-size: 0.82rem;
}

.add {
  margin-top: 0.85rem;
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.5rem 0.75rem;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
  font-weight: 700;
}

.add:disabled {
  color: var(--muted);
  cursor: not-allowed;
}

@media (max-width: 800px) {
  .catalog {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }

  .filters {
    position: static;
  }

  .tags {
    flex-direction: row;
    overflow-x: auto;
  }
}
</style>
