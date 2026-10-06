<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { token, logout } = useAuth()
const orders = ref([])
const error = ref('')
const loading = ref(true)

const statusText = {
  CREATED: '已成立',
  PAID: '已付款',
  SHIPPED: '已出貨',
  DELIVERED: '已送達',
  CANCELLED: '已取消',
}

onMounted(async () => {
  try {
    orders.value = await api('/api/orders', { token: token.value })
  } catch (e) {
    if (e.message === 'Invalid or expired token' || e.message === 'Invalid authorization') {
      logout()
      router.push({ name: 'login', query: { redirect: '/orders' } })
      return
    }
    error.value = describeError(e)
  } finally {
    loading.value = false
  }
})

function formatPrice(price) {
  return `NT$ ${(price || 0).toLocaleString('zh-Hant')}`
}

function formatDate(value) {
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value
    return new Date(year, month - 1, day, hour, minute).toLocaleString('zh-Hant')
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('zh-Hant')
}
</script>

<template>
  <main class="page">
    <h1>訂單</h1>
    <p v-if="loading" class="lead">載入中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <p v-else-if="orders.length === 0" class="lead">還沒有訂單。</p>

    <ul v-else class="orders">
      <li v-for="order in orders" :key="order.id">
        <div class="heading">
          <h2>訂單 #{{ order.id }}</h2>
          <span>{{ statusText[order.status] || order.status }}</span>
        </div>
        <p class="when">{{ formatDate(order.createdAt) }}</p>
        <ul>
          <li v-for="item in order.orderItems" :key="item.id">
            {{ item.title }} × {{ item.quantity }}
          </li>
        </ul>
        <p class="total">{{ formatPrice(order.totalPrice) }}</p>
      </li>
    </ul>
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

.lead,
.when,
.orders span {
  color: var(--muted);
}

.lead {
  margin-top: 0.5rem;
}

.error {
  margin-top: 0.5rem;
  color: var(--accent);
}

.orders {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  margin-top: 1.5rem;
  padding: 0;
  list-style: none;
}

.orders > li {
  padding: 1rem 1.1rem;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--bg-elevated);
}

.heading {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

h2 {
  font-size: 1.2rem;
}

.when {
  margin-top: 0.2rem;
  font-size: 0.9rem;
}

.orders ul {
  margin: 0.75rem 0 0;
  padding-left: 1.1rem;
}

.total {
  margin-top: 0.75rem;
  font-weight: 700;
}
</style>
