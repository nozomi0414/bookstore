<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api } from '@/api/client'
import { categories } from '@/constants/categories'
import { useCart } from '@/composables/useCart'

const books = ref([])
const error = ref('')
const loading = ref(true)
const brokenCovers = ref({})
const selected = ref('全部')
const justAdded = ref(null)
const detail = ref(null)
const closeButton = ref(null)
const { items, addBook } = useCart()

const visibleBooks = computed(() => {
  if (selected.value === '全部') return books.value
  return books.value.filter((book) => (book.category || '未分類') === selected.value)
})

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
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

function field(value) {
  const text = value == null ? '' : String(value).trim()
  return text || '尚未填寫'
}

async function openDetail(book) {
  detail.value = book
  await nextTick()
  closeButton.value?.focus()
}

function closeDetail() {
  detail.value = null
}

function onKeydown(event) {
  if (event.key === 'Escape' && detail.value) closeDetail()
}

watch(detail, (book) => {
  document.body.style.overflow = book ? 'hidden' : ''
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
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
          <button
            type="button"
            class="card-hit"
            :aria-label="`查看${book.title}的詳細資訊`"
            @click="openDetail(book)"
          ></button>
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

    <Teleport to="body">
      <div v-if="detail" class="backdrop" @click.self="closeDetail">
        <article
          class="dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="book-detail-title"
        >
          <button ref="closeButton" type="button" class="close" @click="closeDetail">關閉</button>
          <div class="detail-cover">
            <img
              v-if="showCover(detail)"
              class="cover"
              :src="detail.coverImageUrl"
              :alt="detail.title"
              @error="coverFailed(detail.id)"
            />
            <div
              v-else
              class="cover placeholder"
              :style="{ '--tone': coverTone(detail.title) }"
              aria-hidden="true"
            >
              <span>{{ detail.title }}</span>
            </div>
          </div>
          <div class="detail-body">
            <p v-if="detail.category" class="category">{{ detail.category }}</p>
            <h2 id="book-detail-title">{{ detail.title }}</h2>
            <p class="author">{{ detail.author || '作者未填' }}</p>
            <p class="description">{{ field(detail.description) }}</p>
            <dl class="facts">
              <div>
                <dt>出版社</dt>
                <dd>{{ field(detail.publisher) }}</dd>
              </div>
              <div>
                <dt>ISBN</dt>
                <dd>{{ field(detail.isbn) }}</dd>
              </div>
              <div>
                <dt>庫存</dt>
                <dd>{{ detail.stock == null ? '尚未填寫' : detail.stock }}</dd>
              </div>
            </dl>
            <p class="price detail-price">{{ formatPrice(detail.price) }}</p>
            <button type="button" class="add detail-add" :disabled="atStockLimit(detail)" @click="add(detail)">
              {{
                atStockLimit(detail)
                  ? '已達庫存'
                  : justAdded === detail.id
                    ? '已加入購物車'
                    : '加入購物車'
              }}
            </button>
          </div>
        </article>
      </div>
    </Teleport>
  </main>
</template>

<style scoped>
.catalog {
  display: grid;
  grid-template-columns: 14.5rem 1fr;
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
  margin-top: 0.75rem;
  border-top: 1px solid var(--line);
}

.tags button {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--line);
  border-radius: 0;
  padding: 0.55rem 0.1rem;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
  font-size: 0.92rem;
  text-align: left;
}

.tags button.active {
  color: var(--accent);
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
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--bg-elevated);
  box-shadow: var(--shadow);
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

.cover-wrap,
.detail-cover {
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
}

.placeholder {
  display: flex;
  align-items: flex-end;
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
  position: relative;
  z-index: 2;
  margin-top: 0.85rem;
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.5rem 0.75rem;
  background: var(--bg-elevated);
  color: var(--ink);
  cursor: pointer;
  font-weight: 700;
}

.backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 1.25rem;
  background: rgba(28, 25, 21, 0.46);
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
  box-shadow: var(--shadow);
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
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font-weight: 700;
}

.detail-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-right: 3rem;
}

.detail-body h2 {
  font-size: clamp(1.6rem, 3vw, 2.1rem);
}

.description {
  margin-top: 0.85rem;
  color: var(--ink);
  line-height: 1.7;
}

.facts {
  display: grid;
  gap: 0.65rem;
  margin-top: 1.1rem;
}

.facts div {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  gap: 0.75rem;
}

.facts dt {
  color: var(--muted);
}

.facts dd {
  overflow-wrap: anywhere;
}

.detail-price {
  margin-top: 1.15rem;
  font-size: 1.15rem;
}

.detail-add {
  align-self: flex-start;
  width: auto;
  min-width: 9rem;
  padding-inline: 1.1rem;
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
    border-top: 0;
  }

  .tags button {
    width: auto;
    white-space: nowrap;
    border-bottom: 0;
  }

  .dialog {
    grid-template-columns: 1fr;
  }

  .detail-cover {
    width: min(220px, 70%);
    margin-inline: auto;
  }
}
</style>
