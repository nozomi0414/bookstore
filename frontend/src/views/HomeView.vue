<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '@/api/client'
import { categories } from '@/constants/categories'
import { useCart } from '@/composables/useCart'
import { useToast } from '@/composables/useToast'

const pageSize = 10
const books = ref([])
const error = ref('')
const loading = ref(true)
const brokenCovers = ref({})
const selected = ref('全部')
const query = ref('')
const page = ref(1)
const { items, addBook } = useCart()
const { show } = useToast()

const keyword = computed(() => query.value.trim())

const visibleBooks = computed(() => {
  const term = keyword.value.toLowerCase()
  return books.value.filter((book) => {
    const inCategory = selected.value === '全部' || (book.category || '未分類') === selected.value
    if (!inCategory) return false
    if (!term) return true
    return [book.title, book.author].some((value) => (value || '').toLowerCase().includes(term))
  })
})

const pageCount = computed(() => Math.max(1, Math.ceil(visibleBooks.value.length / pageSize)))

const pagedBooks = computed(() => {
  const start = (page.value - 1) * pageSize
  return visibleBooks.value.slice(start, start + pageSize)
})

const leadText = computed(() => {
  if (books.value.length === 0) return '新書上架後會出現在這裡。'
  const count = visibleBooks.value.length
  const start = count === 0 ? 0 : (page.value - 1) * pageSize + 1
  const end = Math.min(page.value * pageSize, count)
  const range = count === 0 ? '0 本' : `第 ${start}–${end} 本，共 ${count} 本`
  return keyword.value ? `找到 ${range}` : range
})

const pageTitle = computed(() => {
  if (books.value.length === 0 && !loading.value && !error.value) return '書架還是空的'
  return selected.value
})

watch([selected, keyword], () => {
  page.value = 1
})

watch(pageCount, (count) => {
  if (page.value > count) page.value = count
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

async function add(book) {
  if (!(await addBook(book))) return
  show(`已將《${book.title}》加入購物車`)
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
        <h1>{{ pageTitle }}</h1>
        <p v-if="!loading && !error" class="lead">{{ leadText }}</p>
        <input
          v-model="query"
          class="search"
          type="search"
          placeholder="搜尋書名或作者"
          aria-label="搜尋書名或作者"
        />
      </header>

      <p v-if="loading" class="state">正在整理書架…</p>
      <p v-else-if="error" class="state error">{{ error }}</p>
      <p v-else-if="visibleBooks.length === 0" class="state">
        {{ keyword ? '找不到符合的書' : '這個分類目前沒有書' }}
      </p>

      <ul v-else class="books">
        <li v-for="book in pagedBooks" :key="book.id" class="book">
          <RouterLink
            class="card-hit"
            :to="{ name: 'book-detail', params: { id: book.id } }"
            :aria-label="`查看${book.title}的詳細資訊`"
          ></RouterLink>
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
            <h2>{{ book.title }}</h2>
            <p class="author">{{ book.author || '作者未填' }}</p>
            <p class="price">{{ formatPrice(book.price) }}</p>
            <button type="button" class="add" :disabled="atStockLimit(book)" @click="add(book)">
              <svg class="cart-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M6.5 7h14l-1.4 8.2a1 1 0 0 1-1 .8H8.2a1 1 0 0 1-1-.8L5.2 4.5H3"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <circle cx="9" cy="20" r="1.3" fill="currentColor" />
                <circle cx="17" cy="20" r="1.3" fill="currentColor" />
              </svg>
              {{
                atStockLimit(book) ? '已達庫存' : '加入購物車'
              }}
            </button>
          </div>
        </li>
      </ul>

      <div v-if="!loading && !error && visibleBooks.length > 0" class="pager">
        <button type="button" :disabled="page === 1" @click="page -= 1">上一頁</button>
        <span>第 {{ page }} / {{ pageCount }} 頁</span>
        <button type="button" :disabled="page === pageCount" @click="page += 1">下一頁</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.catalog {
  display: grid;
  grid-template-columns: 15.625rem 1fr;
  gap: 1.15rem;
  align-items: start;
  padding-top: 0.75rem;
}

.filters {
  padding: 1.44rem 1.25rem 1.56rem;
  border-radius: 28px;
  background: var(--panel);
}

.eyebrow {
  color: var(--accent);
  font-size: 0.98rem;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.tags {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1.13rem;
}

.tags button {
  width: 100%;
  border: 0;
  border-radius: 999px;
  padding: 0.6rem 1rem;
  background: color-mix(in srgb, var(--bg-elevated) 78%, transparent);
  color: var(--ink);
  cursor: pointer;
  font-size: 1.15rem;
  text-align: left;
  transition: all var(--transition-base);
}

.tags button:hover:not(.active) {
  background: var(--bg-elevated);
  transform: translateX(4px);
}

.tags button.active {
  background: #f6d5cc;
  color: #8d3d2c;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(141, 61, 44, 0.12);
}

.page-head {
  margin-bottom: 1.75rem;
  padding: 1.45rem 1.55rem 1.55rem;
  border-radius: 22px;
  background: var(--banner);
}

h1 {
  font-family: var(--font-serif);
  font-size: clamp(1.7rem, 2.4vw, 2.4rem);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.lead,
.state {
  margin-top: 0.45rem;
  padding: 2rem;
  text-align: center;
  color: var(--muted);
  font-size: 1.05rem;
}

.page-head .lead {
  color: inherit;
  opacity: 0.78;
}

.search {
  display: block;
  width: min(100%, 28rem);
  margin-top: 1rem;
  border: 2px solid rgba(120, 78, 52, 0.08);
  border-radius: 999px;
  padding: 0.7rem 1rem;
  background: #fffdfb;
  color: var(--ink);
  transition: all var(--transition-base);
}

.search:focus {
  border-color: var(--accent);
  box-shadow: 0 4px 16px rgba(212, 101, 74, 0.15);
}

.error {
  color: var(--accent);
}

.books {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1rem;
  list-style: none;
  padding: 0;
}

.book {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--bg-elevated);
  box-shadow: var(--shadow);
<<<<<<< HEAD
  transition: box-shadow 0.2s ease;
}

.book:hover {
  box-shadow: 0 16px 36px rgba(120, 78, 52, 0.16);
=======
  transition: all var(--transition-base);
}

.book:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
  border-color: rgba(90, 62, 48, 0.2);
>>>>>>> a4bc78dffc644df7f3fba307cf2b5f510a938bac
}

.book:has(.card-hit:focus-visible) {
  box-shadow: 0 0 0 2px var(--accent), var(--shadow);
}

.card-hit {
  position: absolute;
  inset: 0;
  z-index: 1;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: var(--line);
  flex-shrink: 0;
}

.cover {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  transition: transform 0.35s ease;
}

.book:hover .cover {
  transform: scale(1.04);
}

.placeholder {
  display: flex;
  align-items: flex-end;
  padding: 1rem;
  background: linear-gradient(165deg, hsl(var(--tone) 62% 78%), hsl(var(--tone) 46% 64%));
  color: #3f342e;
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
  gap: 0.45rem;
  padding: 1.1rem 1.15rem 1.2rem;
}

h2 {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.45;
}

.author {
  color: var(--muted);
  font-size: 0.92rem;
}

.price {
  margin-top: auto;
  padding-top: 0.85rem;
  font-weight: 700;
}

.add {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  margin-top: 1rem;
  width: 100%;
  border: 0;
  border-radius: 999px;
  padding: 0.58rem 0.85rem;
  background: var(--accent);
  color: #fffaf6;
  cursor: pointer;
  font-weight: 700;
  transition: all var(--transition-base);
}

.add:hover:not(:disabled) {
  background: var(--accent-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(212, 101, 74, 0.3);
}

<<<<<<< HEAD
.cart-icon {
  width: 1.05rem;
  height: 1.05rem;
  flex-shrink: 0;
}

.pager {
=======
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 1.25rem;
  background: rgba(28, 25, 21, 0.46);
  backdrop-filter: blur(4px);
  animation: fadeIn var(--transition-base);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.dialog {
  position: relative;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 1.5rem;
  width: min(720px, 100%);
  max-height: min(90vh, 820px);
  overflow: auto;
  padding: 1.35rem;
  border-radius: 20px;
  background: var(--bg-elevated);
  box-shadow: 0 20px 60px rgba(28, 25, 21, 0.3);
  animation: slideUp var(--transition-slow);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.detail-cover {
  width: 220px;
}

.close {
  position: absolute;
  top: 0.85rem;
  right: 0.85rem;
  z-index: 1;
  border: 0;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  background: rgba(255, 255, 255, 0.8);
  color: var(--muted);
  cursor: pointer;
  font-weight: 700;
  transition: all var(--transition-base);
  backdrop-filter: blur(4px);
}

.close:hover {
  background: var(--bg-elevated);
  color: var(--ink);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.detail-body {
>>>>>>> a4bc78dffc644df7f3fba307cf2b5f510a938bac
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.pager button {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.45rem 0.85rem;
  background: var(--bg-elevated);
  color: var(--ink);
  cursor: pointer;
}

.pager button:disabled {
  color: var(--muted);
  cursor: not-allowed;
  opacity: 0.6;
}

.add:disabled {
  background: color-mix(in srgb, var(--line) 80%, var(--bg-elevated));
  color: var(--muted);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

@media (max-width: 1100px) {
  .books {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .catalog {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }

  .books {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tags {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 0.2rem;
  }

  .tags button {
    width: auto;
    white-space: nowrap;
  }
}
</style>
