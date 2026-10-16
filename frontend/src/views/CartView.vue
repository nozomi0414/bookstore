<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'

const router = useRouter()
const { token, isLoggedIn } = useAuth()
const { items, refresh, setQuantity, remove } = useCart()
const recipientName = ref('')
const recipientPhone = ref('')
const shippingAddress = ref('')
const error = ref('')
const fillNote = ref('')
const notice = ref('')
const submitting = ref(false)
const filling = ref(false)
const selected = ref([])
const brokenCovers = ref({})

const allSelected = computed(
  () => items.value.length > 0 && items.value.every((item) => selected.value.includes(item.bookId)),
)
const selectedItems = computed(() =>
  items.value.filter((item) => selected.value.includes(item.bookId)),
)
const selectedTotal = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + (item.price || 0) * item.quantity, 0),
)

function pruneSelection() {
  const ids = new Set(items.value.map((item) => item.bookId))
  selected.value = selected.value.filter((id) => ids.has(id))
}

function toggleAll(checked) {
  selected.value = checked ? items.value.map((item) => item.bookId) : []
}

function toggleOne(bookId, checked) {
  selected.value = checked
    ? [...selected.value, bookId]
    : selected.value.filter((id) => id !== bookId)
}

async function changeQuantity(bookId, quantity) {
  try {
    await setQuantity(bookId, quantity)
    pruneSelection()
  } catch (e) {
    notice.value = describeError(e)
  }
}

async function removeOne(bookId) {
  try {
    await remove(bookId)
    pruneSelection()
  } catch (e) {
    notice.value = describeError(e)
  }
}

async function removeSelected() {
  const ids = [...selected.value]
  selected.value = []
  try {
    for (const bookId of ids) await remove(bookId)
  } catch (e) {
    notice.value = describeError(e)
  }
  pruneSelection()
}

function showCover(item) {
  return Boolean(item.coverImageUrl) && !brokenCovers.value[item.bookId]
}

function formatPrice(price) {
  return `NT$ ${(price || 0).toLocaleString('zh-Hant')}`
}

function shippingError() {
  const name = recipientName.value.trim()
  const phone = recipientPhone.value.trim()
  const address = shippingAddress.value.trim()
  const digits = phone.replace(/\D/g, '')
  if (!name) return '請填寫收件人'
  if (name.length > 40) return '收件人最多 40 個字'
  if (!phone || !/^[0-9+\-()\s]+$/.test(phone) || digits.length < 8 || digits.length > 15) {
    return '請填寫有效的電話'
  }
  if (!address) return '請填寫地址'
  if (address.length > 200) return '地址最多 200 個字'
  return ''
}

async function syncStock() {
  const books = await api('/api/books')
  const byId = new Map(books.map((book) => [book.id, book]))
  const notes = []
  for (const item of [...items.value]) {
    const book = byId.get(item.bookId)
    if (!book) {
      notes.push(`${item.title}已下架，已從購物車移除`)
      await remove(item.bookId)
      continue
    }
    if (book.stock != null && book.stock < 1) {
      notes.push(`${item.title}已售完，已從購物車移除`)
      await remove(item.bookId)
      continue
    }
    const quantity = book.stock == null ? item.quantity : Math.min(item.quantity, book.stock)
    if (quantity < item.quantity) {
      notes.push(`${item.title}庫存只剩 ${book.stock} 本，數量已調整`)
      await setQuantity(item.bookId, quantity)
    }
  }
  pruneSelection()
  return notes.join(' ')
}

onMounted(async () => {
  try {
    if (token.value) await refresh()
    if (items.value.length === 0) return
    notice.value = await syncStock()
  } catch {
    notice.value = ''
  }
})

async function fillProfile() {
  fillNote.value = ''
  error.value = ''
  filling.value = true
  try {
    const profile = await api('/api/users/me', { token: token.value })
    if (!profile.phone && !profile.address) {
      fillNote.value = '尚未儲存電話與地址'
      return
    }
    recipientName.value = profile.name || ''
    recipientPhone.value = profile.phone || ''
    shippingAddress.value = profile.address || ''
  } catch (e) {
    fillNote.value = describeError(e)
  } finally {
    filling.value = false
  }
}

async function checkout() {
  error.value = ''
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: '/cart' } })
    return
  }
  const lines = selectedItems.value
  if (lines.length === 0) {
    error.value = '請先勾選要結帳的商品'
    return
  }
  const invalid = shippingError()
  if (invalid) {
    error.value = invalid
    return
  }
  submitting.value = true
  try {
    await api('/api/orders', {
      method: 'POST',
      token: token.value,
      body: {
        recipientName: recipientName.value.trim(),
        recipientPhone: recipientPhone.value.trim(),
        shippingAddress: shippingAddress.value.trim(),
        orderLines: lines.map((item) => ({
          bookId: item.bookId,
          quantity: item.quantity,
        })),
      },
    })
    for (const item of lines) await remove(item.bookId)
    selected.value = []
    router.push({ name: 'orders' })
  } catch (e) {
    error.value = describeError(e)
    try {
      const notes = await syncStock()
      if (notes) notice.value = notes
    } catch {
      // 庫存同步失敗時仍保留後端回傳的錯誤
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="page">
    <h1>購物車</h1>
    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="items.length === 0" class="lead">購物車是空的。</p>

    <div v-else class="sheet">
      <div class="head" role="row">
        <span></span>
        <span>商品</span>
        <span>價格</span>
        <span>數量</span>
        <span></span>
      </div>
      <div v-for="item in items" :key="item.bookId" class="row">
        <input
          type="checkbox"
          :checked="selected.includes(item.bookId)"
          :aria-label="`選取${item.title}`"
          @change="toggleOne(item.bookId, $event.target.checked)"
        />
        <RouterLink class="product" :to="{ name: 'book-detail', params: { id: item.bookId } }">
          <img
            v-if="showCover(item)"
            class="cover"
            :src="item.coverImageUrl"
            :alt="item.title"
            @error="brokenCovers[item.bookId] = true"
          />
          <span v-else class="cover placeholder" aria-hidden="true">{{ item.title.slice(0, 1) }}</span>
          <span class="meta">
            <span class="title">{{ item.title }}</span>
            <span v-if="item.author" class="author">{{ item.author }}</span>
          </span>
        </RouterLink>
        <p class="price">{{ formatPrice(item.price) }}</p>
        <div class="qty">
          <button type="button" aria-label="減少數量" @click="changeQuantity(item.bookId, item.quantity - 1)">
            −
          </button>
          <span>{{ item.quantity }}</span>
          <button type="button" aria-label="增加數量" @click="changeQuantity(item.bookId, item.quantity + 1)">
            +
          </button>
        </div>
        <button type="button" class="remove" @click="removeOne(item.bookId)">移除</button>
      </div>
      <div class="batch">
        <label class="select-all">
          <input
            type="checkbox"
            :checked="allSelected"
            aria-label="全選"
            @change="toggleAll($event.target.checked)"
          />
          全選
        </label>
        <button type="button" class="batch-delete" :disabled="selected.length === 0" @click="removeSelected">
          整批刪除
        </button>
      </div>
    </div>

    <form v-if="items.length" class="checkout" @submit.prevent="checkout">
      <div class="section-head">
        <h2>收件資料</h2>
        <button v-if="isLoggedIn" type="button" class="ghost" :disabled="filling" @click="fillProfile">
          {{ filling ? '讀取中…' : '填入已存資料' }}
        </button>
      </div>
      <p v-if="fillNote" class="note">
        {{ fillNote }}
        <RouterLink to="/account">前往帳號頁</RouterLink>
      </p>
      <label>
        收件人
        <input v-model="recipientName" type="text" autocomplete="name" maxlength="40" />
      </label>
      <label>
        電話
        <input v-model="recipientPhone" type="tel" autocomplete="tel" />
      </label>
      <label>
        地址
        <textarea v-model="shippingAddress" rows="3" autocomplete="street-address" maxlength="200"></textarea>
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="total-row">
        <p>合計 <strong>{{ formatPrice(selectedTotal) }}</strong></p>
        <div class="submit-block">
          <button type="submit" class="submit" :disabled="submitting">
            {{ submitting ? '送出中…' : isLoggedIn ? '送出訂單' : '登入後結帳' }}
          </button>
          <p v-if="isLoggedIn" class="pay-hint">送出後請至訂單頁完成付款。</p>
        </div>
      </div>
    </form>
  </main>
</template>

<style scoped>
.page {
  max-width: 68rem;
  padding-top: 2rem;
}

h1,
h2 {
  font-family: var(--font-serif);
}

h1 {
  font-size: 2.2rem;
}

.lead {
  margin-top: 0.5rem;
  color: var(--muted);
}

h2 {
  font-size: 1.35rem;
}

.sheet {
  margin-top: 1.5rem;
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
}

.head,
.row {
  display: grid;
  grid-template-columns: 2.75rem minmax(16rem, 1fr) 8rem 8.5rem 4.5rem;
  align-items: center;
  gap: 0.75rem;
  min-width: 42rem;
}

.head {
  padding: 0.7rem 1rem;
  background: #8d3d2c;
  color: #fffaf6;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.head span:nth-child(3),
.head span:nth-child(4) {
  text-align: center;
}

.row {
  padding: 1rem;
  border-top: 1px solid var(--line);
}

.product {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  min-width: 0;
  color: inherit;
}

.cover {
  width: 3.4rem;
  height: 4.6rem;
  flex: none;
  object-fit: cover;
  border-radius: 3px;
  background: var(--panel);
}

.placeholder {
  display: grid;
  place-items: center;
  color: #8d3d2c;
  font-family: var(--font-serif);
  font-size: 1.2rem;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.title {
  font-weight: 700;
}

.author,
.price,
.remove {
  color: var(--muted);
}

.price {
  text-align: center;
  font-weight: 700;
  color: var(--ink);
}

.qty {
  display: flex;
  align-items: center;
  justify-self: center;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.qty button,
.remove,
.ghost,
.batch-delete {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink);
  cursor: pointer;
  font-weight: 700;
}

.qty button {
  width: 2rem;
  height: 2rem;
  border: 0;
  background: transparent;
}

.qty span {
  min-width: 1.6rem;
  text-align: center;
}

.remove {
  border: 0;
  background: transparent;
  color: var(--muted);
}

.batch {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  min-width: 42rem;
  padding: 0.85rem 1rem;
  border-top: 1px solid var(--line);
}

.select-all {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.sheet input[type='checkbox'] {
  width: 1.05rem;
  height: 1.05rem;
  accent-color: #8d3d2c;
}

.batch-delete {
  border-color: var(--accent);
  border-radius: 6px;
  padding: 0.35rem 0.8rem;
  color: var(--accent);
}

.batch-delete:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.checkout {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--line);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.ghost {
  border-radius: 999px;
  padding: 0.4rem 0.85rem;
  background: transparent;
}

.ghost:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.checkout label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.92rem;
}

.checkout input,
.checkout textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 0.7rem 0.8rem;
  background: var(--bg-elevated);
  color: var(--ink);
  resize: vertical;
}

.note,
.notice {
  color: var(--muted);
}

.note a {
  margin-left: 0.35rem;
  color: var(--accent);
  font-weight: 700;
}

.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.4rem;
}

.total-row strong {
  font-size: 1.2rem;
}

.submit {
  border: 0;
  border-radius: 999px;
  padding: 0.7rem 1.2rem;
  background: var(--accent);
  color: #fffaf6;
  cursor: pointer;
  font-weight: 700;
}

.submit:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.submit-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
}

.pay-hint {
  color: var(--muted);
  font-size: 0.85rem;
}

.error {
  color: var(--accent);
}

@media (max-width: 640px) {
  .total-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
