<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'

const router = useRouter()
const { token, isLoggedIn } = useAuth()
const { items, total, setQuantity, remove, clear } = useCart()
const recipientName = ref('')
const recipientPhone = ref('')
const shippingAddress = ref('')
const error = ref('')
const notice = ref('')
const submitting = ref(false)

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
  const next = []
  for (const item of items.value) {
    const book = byId.get(item.bookId)
    if (!book) {
      notes.push(`${item.title}已下架，已從購物車移除`)
      continue
    }
    if (book.stock != null && book.stock < 1) {
      notes.push(`${item.title}已售完，已從購物車移除`)
      continue
    }
    const quantity = book.stock == null ? item.quantity : Math.min(item.quantity, book.stock)
    if (quantity < item.quantity) {
      notes.push(`${item.title}庫存只剩 ${book.stock} 本，數量已調整`)
    }
    next.push({
      ...item,
      title: book.title,
      price: book.price,
      stock: book.stock,
      quantity,
    })
  }
  items.value = next
  return notes.join(' ')
}

onMounted(async () => {
  if (items.value.length === 0) return
  try {
    notice.value = await syncStock()
  } catch {
    notice.value = ''
  }
})

async function checkout() {
  error.value = ''
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: '/cart' } })
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
        orderLines: items.value.map((item) => ({
          bookId: item.bookId,
          quantity: item.quantity,
        })),
      },
    })
    clear()
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

    <ul v-else class="lines">
      <li v-for="item in items" :key="item.bookId">
        <div>
          <h2>{{ item.title }}</h2>
          <p>{{ formatPrice(item.price) }}</p>
        </div>
        <div class="qty">
          <button type="button" aria-label="減少數量" @click="setQuantity(item.bookId, item.quantity - 1)">
            −
          </button>
          <span>{{ item.quantity }}</span>
          <button type="button" aria-label="增加數量" @click="setQuantity(item.bookId, item.quantity + 1)">
            +
          </button>
        </div>
        <button type="button" class="remove" @click="remove(item.bookId)">移除</button>
      </li>
    </ul>

    <form v-if="items.length" class="summary" @submit.prevent="checkout">
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
      <p>合計 {{ formatPrice(total) }}</p>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="submitting">
        {{ submitting ? '送出中…' : isLoggedIn ? '送出訂單' : '登入後結帳' }}
      </button>
    </form>
  </main>
</template>

<style scoped>
.page {
  max-width: 40rem;
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

.lines {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding: 0;
  list-style: none;
}

.lines li {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 1rem;
  align-items: center;
  padding: 1rem;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--bg-elevated);
}

h2 {
  font-size: 1.15rem;
}

.lines p,
.remove {
  color: var(--muted);
}

.qty {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.qty button,
.remove,
.summary button {
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
}

.qty button {
  width: 1.8rem;
  height: 1.8rem;
}

.remove {
  padding: 0.35rem 0.7rem;
}

.summary {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.summary label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.92rem;
}

.summary input,
.summary textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 0.7rem 0.8rem;
  background: var(--bg-elevated);
  color: var(--ink);
  resize: vertical;
}

.notice {
  margin-top: 0.75rem;
  color: var(--muted);
}

.summary button {
  align-self: flex-start;
  border: 0;
  padding: 0.7rem 1.1rem;
  background: var(--ink);
  color: var(--bg);
  font-weight: 700;
}

.summary button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.error {
  color: var(--accent);
}

@media (max-width: 640px) {
  .lines li {
    grid-template-columns: 1fr auto;
  }

  .remove {
    grid-column: 2;
  }
}
</style>
