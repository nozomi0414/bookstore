<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api/client'
import { categoryTones } from '@/constants/categories'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const book = ref(null)
const error = ref('')
const loading = ref(true)
const coverBroken = ref(false)
const { isAdmin } = useAuth()
const { items, addBook } = useCart()
const { show } = useToast()

const atStockLimit = computed(() => {
  if (!book.value || book.value.stock == null) return false
  const item = items.value.find((entry) => entry.bookId === book.value.id)
  return (item?.quantity || 0) >= book.value.stock
})

const chipStyle = computed(() => {
  const tone = categoryTones[book.value?.category]
  if (!tone) return undefined
  return { background: tone.bg, color: tone.ink }
})

const coverTone = computed(() => {
  const hues = [16, 152, 202, 28, 262]
  const title = book.value?.title || '書'
  const sum = [...title].reduce((total, char) => total + char.charCodeAt(0), 0)
  return hues[sum % hues.length]
})

function formatPrice(price) {
  if (price == null) return '價格未定'
  return `NT$ ${price.toLocaleString('zh-Hant')}`
}

function field(value) {
  const text = value == null ? '' : String(value).trim()
  return text || '尚未填寫'
}

async function add() {
  if (!book.value || !(await addBook(book.value))) return
  show(`已將《${book.value.title}》加入購物車`)
}

async function load(id) {
  loading.value = true
  error.value = ''
  book.value = null
  coverBroken.value = false
  try {
    book.value = await api(`/api/books/${id}`)
    document.title = `${book.value.title}｜拾頁`
  } catch (e) {
    error.value = e.message || '找不到這本書'
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (id) load(id)
  },
  { immediate: true },
)
</script>

<template>
  <main class="page">
    <RouterLink class="back" to="/">← 書架</RouterLink>
    <p v-if="loading" class="state">正在打開這本書…</p>
    <p v-else-if="error" class="state error">{{ error }}</p>
    <article v-else-if="book" class="detail">
      <div class="spread">
        <div class="cover-wrap">
          <img
            v-if="book.coverImageUrl && !coverBroken"
            class="cover"
            :src="book.coverImageUrl"
            :alt="book.title"
            @error="coverBroken = true"
          />
          <div v-else class="cover placeholder" :style="{ '--tone': coverTone }" aria-hidden="true">
            <span>{{ book.title }}</span>
          </div>
        </div>
        <div class="intro">
          <p v-if="book.category" class="category" :style="chipStyle">{{ book.category }}</p>
          <div class="title-row">
            <h1>{{ book.title }}</h1>
            <RouterLink
              v-if="isAdmin"
              class="edit"
              :to="{ name: 'book-edit', params: { id: book.id } }"
            >
              編輯
            </RouterLink>
          </div>
          <p class="author">{{ book.author || '作者未填' }}</p>
          <div class="buy">
            <p class="price">{{ formatPrice(book.price) }}</p>
            <p class="stock">{{ book.stock == null ? '庫存未填' : `庫存 ${book.stock}` }}</p>
            <button type="button" class="add" :disabled="atStockLimit" @click="add">
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
              {{ atStockLimit ? '已達庫存' : '加入購物車' }}
            </button>
          </div>
        </div>
      </div>

      <section class="section">
        <h2>內容簡介</h2>
        <p class="description">{{ field(book.description) }}</p>
      </section>

      <section class="section">
        <h2>出版資訊</h2>
        <dl class="facts">
          <div>
            <dt>出版社</dt>
            <dd>{{ field(book.publisher) }}</dd>
          </div>
          <div>
            <dt>ISBN</dt>
            <dd>{{ field(book.isbn) }}</dd>
          </div>
        </dl>
      </section>
    </article>
  </main>
</template>

<style scoped>
.page {
  padding-top: 0.35rem;
}

.back {
  display: inline-block;
  margin-bottom: 1.75rem;
  color: var(--muted);
  font-weight: 700;
}

.state {
  color: var(--muted);
}

.error {
  color: var(--accent);
}

.spread {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  gap: clamp(1.75rem, 4vw, 4.5rem);
  align-items: start;
}

.cover-wrap {
  position: sticky;
  top: 6.25rem;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border-radius: 4px 14px 14px 4px;
  background: var(--line);
  box-shadow: 0 22px 40px rgba(80, 48, 32, 0.16);
}

.cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
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
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.35;
}

.intro {
  min-width: 0;
  padding-top: 0.35rem;
}

.category {
  display: inline-block;
  padding: 0.12rem 0.55rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.7rem;
}

h1 {
  font-family: var(--font-serif);
  font-size: clamp(2.2rem, 4vw, 3.4rem);
  font-weight: 700;
  line-height: 1.25;
}

.edit {
  flex: none;
  margin-top: 0.45rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.35rem 0.85rem;
  color: var(--ink);
  font-weight: 700;
}

.author {
  margin-top: 0.55rem;
  color: var(--muted);
  font-size: 0.95rem;
  font-weight: 400;
}

.buy {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem 1.25rem;
  margin-top: 1.75rem;
  padding: 1.15rem 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.price {
  font-size: 1.85rem;
  font-weight: 700;
}

.stock {
  color: var(--muted);
  font-size: 0.85rem;
}

.section {
  margin-top: 2.75rem;
  padding-top: 1.6rem;
  border-top: 1px solid var(--line);
}

h2 {
  font-family: var(--font-serif);
  font-size: 1.35rem;
  font-weight: 700;
}

.description {
  max-width: 40rem;
  margin-top: 0.85rem;
  font-size: 1.05rem;
  line-height: 1.9;
}

.facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 16rem));
  gap: 1.25rem 2.5rem;
  margin-top: 1rem;
}

.facts dt {
  color: var(--muted);
  font-size: 0.85rem;
}

.facts dd {
  margin-top: 0.25rem;
  overflow-wrap: anywhere;
}

.add {
  display: inline-flex;
  align-items: center;
  margin-left: auto;
  gap: 0.4rem;
  border: 0;
  border-radius: 999px;
  padding: 0.65rem 1.2rem;
  background: var(--accent);
  color: #fffaf6;
  cursor: pointer;
  font-weight: 700;
}

.cart-icon {
  width: 1.1rem;
  height: 1.1rem;
  flex-shrink: 0;
}

.add:disabled {
  background: color-mix(in srgb, var(--line) 80%, var(--bg-elevated));
  color: var(--muted);
  cursor: not-allowed;
}

@media (max-width: 800px) {
  .spread {
    grid-template-columns: 1fr;
  }

  .cover-wrap {
    position: static;
    width: min(240px, 70%);
  }

  .facts {
    grid-template-columns: 1fr;
  }
}
</style>
