<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'

const router = useRouter()
const { token, isLoggedIn } = useAuth()
const { items, total, setQuantity, remove, clear } = useCart()
const error = ref('')
const submitting = ref(false)

function formatPrice(price) {
  return `NT$ ${(price || 0).toLocaleString('zh-Hant')}`
}

async function checkout() {
  error.value = ''
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: '/cart' } })
    return
  }
  submitting.value = true
  try {
    await api('/api/orders', {
      method: 'POST',
      token: token.value,
      body: {
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
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="page">
    <h1>購物車</h1>
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

    <div v-if="items.length" class="summary">
      <p>合計 {{ formatPrice(total) }}</p>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="button" :disabled="submitting" @click="checkout">
        {{ submitting ? '送出中…' : isLoggedIn ? '送出訂單' : '登入後結帳' }}
      </button>
    </div>
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
  align-items: flex-start;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.summary button {
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
